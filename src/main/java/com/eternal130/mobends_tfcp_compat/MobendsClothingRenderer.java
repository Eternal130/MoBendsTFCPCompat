package com.eternal130.mobends_tfcp_compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Renders TFC+ clothing models directly inside the MoBends coordinate frame, by hooking
 * {@link RenderPlayerEvent.Specials.Post} — which fires <i>before</i>
 * {@code RendererLivingEntity.doRender}'s {@code glPopMatrix()}.
 *
 * <p>At that moment the GL modelview matrix is the exact frame MoBends used to render the
 * player body: {@code camera · translateToEntity · rotateYaw · MoBends.postRender · scale}.
 * Rendering clothing here means the clothing models inherit MoBends' 3D body orientation
 * naturally — no {@code R(180,X)} flip correction, no matrix capture/restore, no
 * {@code makeAdjustments}. The entire frame-mismatch problem is sidestepped.
 *
 * <p>TFC+'s own clothing rendering (in {@code RenderPlayerEvent.Post}, after
 * {@code glPopMatrix}) is disabled via {@link MixinRenderClothing} making
 * {@code RenderClothing.doRender} a no-op.
 *
 * <p><b>Why Specials.Post not Post:</b> By the time {@code RenderPlayerEvent.Post} fires,
 * the matrix has been popped. TFC+ rebuilds it from scratch ({@code makeAdjustments} +
 * two 180° rotations), producing a frame where clothing only rotates around fixed world
 * axes instead of following the body in 3D. Specials.Post is the latest point where the
 * MoBends body-frame matrix is still on the GL stack.
 */
public class MobendsClothingRenderer {

    private com.dunk.tfc.Render.RenderClothing tfcClothing;
    private ItemStack[] armorArray = new ItemStack[4];

    public MobendsClothingRenderer() {
        try {
            java.lang.reflect.Field f = com.dunk.tfc.Handlers.Client.PlayerRenderHandler.class.getField("RENDER_CLOTHING");
            tfcClothing = (com.dunk.tfc.Render.RenderClothing) f.get(null);
        } catch (Throwable t) {
            MoBendsTFCPCompat.LOG.warn("MobendsClothingRenderer: could not access TFC+ RENDER_CLOTHING", t);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSpecialsPost(RenderPlayerEvent.Specials.Post e) {
        if (tfcClothing == null) return;

        EntityPlayer player = e.entityPlayer;
        if (player == null) return;

        // Collect all equipped IEquipable clothing items
        List<ItemStack> clothing = collectClothing(player);
        if (clothing.isEmpty()) return;

        // Build armor array for switchRender
        System.arraycopy(player.inventory.armorInventory, 0, armorArray, 0,
            Math.min(4, player.inventory.armorInventory.length));

        // Compute the renderYawOffset that RendererLivingEntity.rotateCorpse applied as
        // glRotatef(180 - renderYawOffset, 0, 1, 0). TFC+ clothing models (ModelShirt etc.)
        // internally re-apply glRotatef(renderYawOffset, 0, 1, 0) themselves.
        //
        // At Specials.Post the matrix already contains R_y(180-yaw). We add R_y(-yaw) to
        // pre-cancel the clothing model's internal R_y(yaw), so net Y rotation =
        // (180-yaw) + (-yaw) + yaw = 180-yaw — matching the body.
        //
        // Previous version used -(180-yaw) which was WRONG: it gives net = (180-yaw)+(yaw-180)+yaw = yaw,
        // not 180-yaw. The 180° offset caused the "clothing spins around head-foot axis" symptom
        // even when standing still and moving the view.
        float interpolatedYawOffset = player.prevRenderYawOffset
            + (player.renderYawOffset - player.prevRenderYawOffset) * e.partialRenderTick;

        for (ItemStack item : clothing) {
            if (item == null) continue;
            if (!(item.getItem() instanceof com.dunk.tfc.api.Interfaces.IEquipable)) continue;

            com.dunk.tfc.api.Interfaces.IEquipable ie = (com.dunk.tfc.api.Interfaces.IEquipable) item.getItem();

            GL11.glPushMatrix();

            // X/Z compensation MUST be applied before R_y(-yaw), in the M_body-local frame.
            // Translates placed after R_y(-yaw) get conjugated by R_y(yaw) (from the clothing
            // model) and become yaw-dependent (the "left/right flip between N/S" symptom).
            // T_sneak itself sits inside R_y(yaw)·R_y(-yaw)=I so it never rotates — to cancel
            // it, the compensation must also be un-rotated, i.e. live in the same M_body frame.
            if (player.isSneaking()) {
                GL11.glTranslatef(0.0F, 0.0F, -5.0F * 0.0625F);
            }
            GL11.glTranslatef(0.0F, -3.0F * 0.0625F, 0.0F);
            GL11.glRotatef(-interpolatedYawOffset, 0.0F, 1.0F, 0.0F);

            // Bind the clothing texture (same as TFC+ does in doRender)
            int textureVariant = 0;
            try {
                if (item.getItem() instanceof com.dunk.tfc.Items.ItemTFCArmor) {
                    textureVariant = ((com.dunk.tfc.Items.ItemTFCArmor) item.getItem()).getUnadjustedArmorType();
                }
            } catch (Throwable ignored) {}
            net.minecraft.client.Minecraft.getMinecraft().renderEngine.bindTexture(
                ie.getClothingTexture(player, item, textureVariant));

            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);

            // Dispatch to TFC+'s clothing model dispatch. This copies rotateAngleX/Y/Z from
            // modelBipedMain (the ModelBendsPlayer) into the clothing parts and renders them.
            // Because we're inside MoBends' body-frame matrix (not TFC+'s rebuilt one), the
            // rotations apply in the correct 3D orientation.
            tfcClothing.switchRender(
                (EntityLivingBase) player,
                item,
                e.renderer,
                e.partialRenderTick,
                armorArray);

            GL11.glPopMatrix();
        }
    }

    /** Collects all equipped IEquipable items from TFC+ extra inventory + vanilla armor slots. */
    private List<ItemStack> collectClothing(EntityPlayer player) {
        List<ItemStack> result = new ArrayList<ItemStack>();

        // TFC+ extra equip inventory (shirt, pants, etc.)
        try {
            java.lang.reflect.Field f = player.inventory.getClass().getField("extraEquipInventory");
            ItemStack[] extra = (ItemStack[]) f.get(player.inventory);
            if (extra != null) {
                for (ItemStack is : extra) {
                    if (is != null && is.getItem() instanceof com.dunk.tfc.api.Interfaces.IEquipable) {
                        result.add(is);
                    }
                }
            }
        } catch (NoSuchFieldException nsfe) {
            // Remote player — try PlayerInfo path
            collectFromPlayerInfo(player, result);
        } catch (Throwable ignored) {}

        // Vanilla armor slots (TFC+ boots/hats also go here)
        for (ItemStack is : player.inventory.armorInventory) {
            if (is != null && is.getItem() instanceof com.dunk.tfc.api.Interfaces.IEquipable) {
                result.add(is);
            }
        }

        return result;
    }

    private void collectFromPlayerInfo(EntityPlayer player, List<ItemStack> result) {
        try {
            Class<?> pmClass = Class.forName("com.dunk.tfc.Core.Player.PlayerManagerTFC");
            Object manager = pmClass.getMethod("getInstance").invoke(null);
            Object info = pmClass.getMethod("getPlayerInfoFromName", String.class)
                .invoke(manager, player.getCommandSenderName());
            if (info != null) {
                java.lang.reflect.Field f = info.getClass().getField("myExtraItems");
                ItemStack[] extra = (ItemStack[]) f.get(info);
                if (extra != null) {
                    for (ItemStack is : extra) {
                        if (is != null && is.getItem() instanceof com.dunk.tfc.api.Interfaces.IEquipable) {
                            result.add(is);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
