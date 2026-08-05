package com.eternal130.mobends_tfcp_compat;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.dunk.tfc.Core.Player.InventoryPlayerTFC;
import com.dunk.tfc.Core.Player.PlayerInfo;
import com.dunk.tfc.Core.Player.PlayerManagerTFC;
import com.dunk.tfc.Handlers.Client.PlayerRenderHandler;
import com.dunk.tfc.Items.ItemQuiver;
import com.dunk.tfc.Render.RenderClothing;
import com.dunk.tfc.Render.RenderLargeItem;
import com.dunk.tfc.Render.RenderQuiver;
import com.dunk.tfc.api.Interfaces.IEquipable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Drop-in replacement for TFC+'s {@link PlayerRenderHandler} that renders clothing with Mo'Bends'
 * whole-body transform applied, so shirts/coats/pants follow the bent torso instead of staying in
 * the vanilla pose.
 *
 * <p>Everything except the clothing pass is a verbatim copy of TFC+'s logic (shoes offset, quiver,
 * large items, sleeping translation, F5 lighting). Only the {@code RENDER_CLOTHING.render(...)}
 * calls are wrapped: we push, apply {@code ModelBendsPlayer.postRender(scale, height)}, then call
 * TFC+'s renderer, then pop. The render-time GL matrix inside TFC+'s {@code RenderClothing.doRender}
 * stacks on top of Mo'Bends' body transform, so clothing ends up in body space.</p>
 *
 * <p>One nuance: TFC+ re-creates {@code modelArmor} as a vanilla {@code ModelBiped} in its Pre
 * handler. We deliberately skip that — Mo'Bends overrides {@code shouldRenderPass} to provide its
 * own {@code ModelBendsPlayer} armor model, and forcing vanilla {@code ModelBiped} would detach
 * armor from the bent body too.</p>
 */
public class CompatPlayerRenderHandler {

    public static ItemStack toRender;

    private Entity ridingEntity = null;

    public static final RenderQuiver RENDER_QUIVER = PlayerRenderHandler.RENDER_QUIVER;
    public static final RenderLargeItem RENDER_LARGE = PlayerRenderHandler.RENDER_LARGE;
    public static final RenderClothing RENDER_CLOTHING = PlayerRenderHandler.RENDER_CLOTHING;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerRenderTickPre(RenderPlayerEvent.Pre e) {
        // TFC+ used this slot to replace the renderer's modelArmor with a plain ModelBiped.
        // Mo'Bends provides its own ModelBendsPlayer-based armor model in shouldRenderPass; touching
        // it here would either be reverted by Mo'Bends or break armor pose. So we leave it alone.

        EntityPlayer player = e.entityPlayer;

        if (player.ridingEntity != null
            && "com.dunk.tfc.Entities.EntityPlough".equals(player.ridingEntity.getClass()
                .getName())) {
            ridingEntity = player.ridingEntity;
            if (ridingEntity.ridingEntity != null) {
                player.limbSwing = ((EntityLivingBase) ridingEntity.ridingEntity).limbSwing;
                player.limbSwingAmount = ((EntityLivingBase) ridingEntity.ridingEntity).limbSwingAmount / 3f;
            }
            player.ridingEntity = null;
        } else {
            ridingEntity = null;
        }

        boolean hasShoes = ((InventoryPlayerTFC) player.inventory).extraEquipInventory[8] != null;
        if (hasShoes && !player.isSneaking()) {
            GL11.glTranslatef(0, 0.5f / 16f, 0f);
        }
    }

    @SubscribeEvent
    public void onPlayerRenderTickPost(RenderPlayerEvent.Post e) {
        EntityPlayer player = e.entityPlayer;
        float partial = e.partialRenderTick;
        RenderPlayer renderer = e.renderer;

        PlayerInfo f = PlayerManagerTFC.getInstance()
            .getPlayerInfoFromName(player.getDisplayName());

        if (player.getCurrentArmor(0) != null && !player.isSneaking()) {
            GL11.glTranslatef(0, 0.5f / 16f, 0f);
        }

        ItemStack[] equipables = (player == Minecraft.getMinecraft().thePlayer && f != null) ? f.myExtraItems
            : (f != null ? f.myExtraItems : null);

        if (RenderManager.instance.playerViewY == 180) {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        }

        if (equipables != null) {
            for (ItemStack i : equipables) {
                if (i == null) {
                    continue;
                }
                if (i.getItem() instanceof ItemQuiver) {
                    if (player.isPlayerSleeping()) {
                        continue;
                    }
                    GL11.glPushMatrix();
                    float rotateAngle;
                    if (player.ridingEntity != null && player.ridingEntity instanceof EntityLiving
                        && !isTfcHorse(player.ridingEntity)) {
                        rotateAngle = ((EntityLiving) player.ridingEntity).prevRenderYawOffset
                            + (((EntityLiving) player.ridingEntity).renderYawOffset
                                - ((EntityLiving) player.ridingEntity).prevRenderYawOffset) * partial;
                    } else {
                        rotateAngle = player.prevRenderYawOffset
                            + (player.renderYawOffset - player.prevRenderYawOffset) * partial;
                    }
                    GL11.glRotatef(-rotateAngle, 0, 1, 0);
                    GL11.glRotatef(180, 0, 0, 1);
                    GL11.glRotatef(180, 0, 1, 0);
                    GL11.glTranslatef(0, 2f / 16f, -2.5f / 16f);
                    RENDER_QUIVER.render(player, i, partial);
                    GL11.glPopMatrix();
                } else if (i.getItem() instanceof ItemBlock) {
                    if (player.isPlayerSleeping()) {
                        continue;
                    }
                    GL11.glPushMatrix();
                    if (player != Minecraft.getMinecraft().thePlayer) {
                        GL11.glTranslatef(0, -0.8f, 0);
                    }
                    RENDER_LARGE.render(player, i, partial);
                    GL11.glPopMatrix();
                } else if (i.getItem() instanceof IEquipable) {
                    // Clothing — wrap with Mo'Bends body transform.
                    GL11.glPushMatrix();
                    MoBendsTransformApplier.applyForPlayer(renderer, player, partial);
                    RENDER_CLOTHING.render(
                        player,
                        i,
                        partial,
                        renderer,
                        player.inventory.armorInventory);
                    GL11.glPopMatrix();
                }
            }
        }

        for (ItemStack i : player.inventory.armorInventory) {
            if (i == null) {
                continue;
            }
            // Wrap each clothing render with the Mo'Bends transform.
            GL11.glPushMatrix();
            MoBendsTransformApplier.applyForPlayer(renderer, player, partial);
            RENDER_CLOTHING.render(player, i, partial, renderer, player.inventory.armorInventory);
            GL11.glPopMatrix();
        }

        toRender = null;

        boolean hasShoes = ((InventoryPlayerTFC) player.inventory).extraEquipInventory[8] != null;
        if (hasShoes && !player.isSneaking()) {
            GL11.glTranslatef(0, -1f / 16f, 0f);
        }

        if (RenderManager.instance.playerViewY == 180) {
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }

        if (player.ridingEntity == null && ridingEntity != null) {
            player.ridingEntity = ridingEntity;
            ridingEntity = null;
        }
    }

    /**
     * TFC+'s code special-cases its own {@code EntityHorseTFC}. We can't import that class by name
     * in a compile-only dependency without TFC+ on the classpath at runtime, so check by FQN.
     */
    private static boolean isTfcHorse(Entity e) {
        return e != null && "com.dunk.tfc.Entities.Mobs.EntityHorseTFC".equals(e.getClass()
            .getName());
    }
}
