package com.eternal130.mobends_tfcp_compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Renders TFC+ clothing as vanilla-style ModelBiped adapters inside the MoBends body-frame
 * matrix at {@code RenderPlayerEvent.Specials.Post} — before
 * {@code RendererLivingEntity.doRender}'s {@code glPopMatrix()}.
 *
 * <p>At that moment the GL modelview matrix is the exact frame MoBends used to render the
 * player body, and {@code renderer.modelBipedMain} holds this frame's bent pose. Each
 * clothing item is rendered by a {@link ModelBipedClothingAdapter} that:
 * <ol>
 *   <li>mirrors the TFC+ clothing geometry ( Shirt = body+arms, Pants = legs+waist, …),</li>
 *   <li>syncs bipedXxx.rotateAngle/rotationPoint from {@code modelBipedMain}, so the adapter
 *       inherits the exact MoBends pose ( arms swing at the shoulder, forearms/legs bend
 *       naturally — same as vanilla armor does),</li>
 *   <li>overrides {@code setRotationAngles} to NOP so vanilla walk/swing math doesn't
 *       overwrite the synced pose.</li>
 * </ol>
 *
 * <p>TFC+'s own {@code RenderClothing.doRender} is cancelled by {@code MixinRenderClothing}
 * so only the adapters draw.
 */
public class MobendsClothingRenderer {

    private final Map<ModelBipedClothingAdapter.ClothingType, ModelBipedClothingAdapter> adapterCache =
        new HashMap<ModelBipedClothingAdapter.ClothingType, ModelBipedClothingAdapter>();

    private TFCPCapeRenderer tfcpCapeRenderer;
    private com.dunk.tfc.Render.Models.ModelCloak tfcCloakModel;
    private CoatSkirtModel coatSkirt;
    private CoatSkirtModel robeSkirt;
    private static long lastDiagMs = 0L;
    private static Field extraEquipField;
    private static boolean extraEquipFieldResolved;

    public MobendsClothingRenderer() {
        try {
            tfcpCapeRenderer = new TFCPCapeRenderer();
        } catch (Throwable t) {
            MoBendsTFCPCompat.LOG.warn("MobendsClothingRenderer: could not init TFCPCapeRenderer", t);
        }
        try {
            tfcCloakModel = new com.dunk.tfc.Render.Models.ModelCloak();
        } catch (Throwable t) {
            MoBendsTFCPCompat.LOG.warn("MobendsClothingRenderer: could not init TFC+ ModelCloak", t);
        }
        coatSkirt = new CoatSkirtModel(7F, 0.6F, 0F, true);
        robeSkirt = new CoatSkirtModel(10F, 0.6F, 0.05F, false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPre(RenderPlayerEvent.Pre e) {
        long now = System.currentTimeMillis();
        if (now - lastDiagMs > 200 && e.entityPlayer.equals(Minecraft.getMinecraft().thePlayer)) {
            ModelBiped m = e.renderer.modelBipedMain;
            System.out.println("[Adapter-Diag] PRE-MOBENDS-MAIN"
                + " cls=" + (m == null ? "null" : m.getClass().getSimpleName())
                + " body=" + rad2deg(m, "body")
                + " armR=" + rad2deg(m, "armR")
                + " armL=" + rad2deg(m, "armL")
                + " legR=" + rad2deg(m, "legR")
                + " legL=" + rad2deg(m, "legL"));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialsPre(RenderPlayerEvent.Specials.Pre e) {
        if (tfcpCapeRenderer == null) return;
        if (hasCloak(e.entityPlayer)) {
            e.renderCape = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSpecialsPost(RenderPlayerEvent.Specials.Post e) {
        EntityPlayer player = e.entityPlayer;
        if (player == null) return;

        List<ItemStack> clothing = collectClothing(player);
        if (clothing.isEmpty()) return;

        ModelBiped sourceModel = e.renderer.modelBipedMain;
        float scale = 0.0625F;

        boolean diag = false;
        long now = System.currentTimeMillis();
        if (now - lastDiagMs > 200 && player.equals(Minecraft.getMinecraft().thePlayer)) {
            lastDiagMs = now;
            diag = true;
        }

        for (ItemStack item : clothing) {
            if (item == null) continue;
            if (!(item.getItem() instanceof com.dunk.tfc.api.Interfaces.IEquipable)) continue;
            com.dunk.tfc.api.Interfaces.IEquipable ie = (com.dunk.tfc.api.Interfaces.IEquipable) item.getItem();
            ModelBipedClothingAdapter.ClothingType type = mapClothingType(ie.getClothingType());
            if (type == null) continue;
            if (type == ModelBipedClothingAdapter.ClothingType.STRAW_HAT
                    && item.getItem() == com.dunk.tfc.api.TFCItems.strawHat2) {
                type = ModelBipedClothingAdapter.ClothingType.STRAW_HAT2;
            }
            if (type == ModelBipedClothingAdapter.ClothingType.CLOTH_HAT) {
                if (item.getItem() == com.dunk.tfc.api.TFCItems.bearFurHat) {
                    type = ModelBipedClothingAdapter.ClothingType.FUR_HAT_BEAR;
                } else if (item.getItem() == com.dunk.tfc.api.TFCItems.wolfFurHat) {
                    type = ModelBipedClothingAdapter.ClothingType.FUR_HAT_WOLF;
                }
            }
            if (type == ModelBipedClothingAdapter.ClothingType.PANTS
                    && isShorts(item.getItem())) {
                type = ModelBipedClothingAdapter.ClothingType.SHORTS;
            }

            // TFC+ ModelHat suppresses only the generic cloth-hat branch when a
            // helmet sits in armor[3]; straw/fur branches dispatch before it.
            if (type == ModelBipedClothingAdapter.ClothingType.CLOTH_HAT
                    && player.getCurrentArmor(3) != null) {
                continue;
            }

            ModelBipedClothingAdapter adapter = adapterCache.get(type);
            if (adapter == null) {
                boolean sleeveless = type == ModelBipedClothingAdapter.ClothingType.SHIRT
                    && isSleevelessShirt(item.getItem());
                adapter = new ModelBipedClothingAdapter(type, clothingScale(type), sleeveless);
                adapterCache.put(type, adapter);
            }

            if (diag && type == ModelBipedClothingAdapter.ClothingType.SHIRT) {
                logAngles("source", sourceModel);
            }

            adapter.syncFromModelBiped(sourceModel);

            if (diag && type == ModelBipedClothingAdapter.ClothingType.SHIRT) {
                logAngles("adapter", adapter);
            }

            int textureVariant = 0;
            try {
                if (item.getItem() instanceof com.dunk.tfc.Items.ItemTFCArmor) {
                    textureVariant = ((com.dunk.tfc.Items.ItemTFCArmor) item.getItem()).getUnadjustedArmorType();
                }
            } catch (Throwable ignored) {}
            Minecraft.getMinecraft().renderEngine.bindTexture(
                ie.getClothingTexture(player, item, textureVariant));

            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);

            applyClothingTint(item);
            if (type == ModelBipedClothingAdapter.ClothingType.CLOAK && tfcpCapeRenderer != null) {
                renderCloak(player, e, sourceModel, ie, item, textureVariant);
            } else {
                GL11.glPushMatrix();
                adapter.render((EntityLivingBase) player, 0F, 0F, 0F, 0F, 0F, scale);
                if (type == ModelBipedClothingAdapter.ClothingType.COAT && coatSkirt != null) {
                    coatSkirt.render(player, sourceModel, scale);
                } else if (type == ModelBipedClothingAdapter.ClothingType.ROBE && robeSkirt != null) {
                    robeSkirt.render(player, sourceModel, scale);
                }
                GL11.glPopMatrix();
            }
            GL11.glColor3f(1F, 1F, 1F);

            if (diag && type == ModelBipedClothingAdapter.ClothingType.SHIRT) {
                System.out.println("[Adapter-Diag] rendered shirt"
                    + " body.show=" + adapter.bipedBody.showModel
                    + " body.children=" + (adapter.bipedBody.childModels != null ? adapter.bipedBody.childModels.size() : -1)
                    + " body.rot=" + String.format("(%.1f,%.1f,%.1f)",
                        adapter.bipedBody.rotateAngleX * 180f / (float) Math.PI,
                        adapter.bipedBody.rotateAngleY * 180f / (float) Math.PI,
                        adapter.bipedBody.rotateAngleZ * 180f / (float) Math.PI));
            }
            if (diag && type == ModelBipedClothingAdapter.ClothingType.PANTS) {
                System.out.println("[Adapter-Diag] pants"
                    + " legR.rp=(" + adapter.bipedRightLeg.rotationPointX + "," + adapter.bipedRightLeg.rotationPointY + "," + adapter.bipedRightLeg.rotationPointZ + ")"
                    + " legR.show=" + adapter.bipedRightLeg.showModel
                    + " legR.children=" + (adapter.bipedRightLeg.childModels != null ? adapter.bipedRightLeg.childModels.size() : -1)
                    + " legR.rotX=" + String.format("%.1f", adapter.bipedRightLeg.rotateAngleX * 180f / (float) Math.PI));
            }
        }
    }

    /**
     * TFC+ tint: dye from NBT "color" x wetness (12000-"wetness")/12000.
     */
    private void applyClothingTint(ItemStack item) {
        float wetness = 1F;
        if (item.stackTagCompound != null) {
            wetness = (12000F - (float) item.stackTagCompound.getInteger("wetness")) / 12000F;
        }
        float r = 1F;
        float g = 1F;
        float b = 1F;
        if (item.getItem() instanceof com.dunk.tfc.Items.ItemClothing) {
            int color = ((com.dunk.tfc.Items.ItemClothing) item.getItem()).getColor(item);
            if (color != -1) {
                r = ((color >> 16) & 255) / 255.0F;
                g = ((color >> 8) & 255) / 255.0F;
                b = (color & 255) / 255.0F;
            }
        }
        // TFC+ ModelSocks applies an extra leather tint to leather sandals.
        if (item.getItem() == com.dunk.tfc.api.TFCItems.leatherSandals) {
            r *= 204F / 255.0F;
            g *= 177F / 255.0F;
            b *= 87F / 255.0F;
        }
        GL11.glColor3f(r * wetness, g * wetness, b * wetness);
    }

    private void renderCloak(EntityPlayer player, RenderPlayerEvent.Specials.Post e,
            ModelBiped sourceModel, com.dunk.tfc.api.Interfaces.IEquipable ie,
            ItemStack item, int textureVariant) {
        if (tfcpCapeRenderer == null) return;
        float scale = 0.0625F;
        float partialTicks = e.partialRenderTick;
        GL11.glPushMatrix();
        sourceModel.bipedBody.postRender(scale);
        GL11.glTranslatef(0.0F, -12.5F * scale, 0.0F);
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);

        net.gobbob.mobends.data.Data_Player capeData = net.gobbob.mobends.data.Data_Player.get(player.getEntityId());
        boolean flyingSprint = player.capabilities.isFlying && player.isSprinting();
        if (flyingSprint) {
            capeData.setCapeWaveSpeed(4.0F);
        } else {
            capeData.setCapeWaveSpeed(1.0F);
        }

        tfcpCapeRenderer.applyAnimation(capeData);
        tfcpCapeRenderer.render(scale);
        GL11.glPopMatrix();
    }

    private boolean hasCloak(EntityPlayer player) {
        if (player == null) return false;
        for (ItemStack item : collectClothing(player)) {
            if (item == null || !(item.getItem() instanceof com.dunk.tfc.api.Interfaces.IEquipable)) continue;
            com.dunk.tfc.api.Interfaces.IEquipable ie = (com.dunk.tfc.api.Interfaces.IEquipable) item.getItem();
            if (mapClothingType(ie.getClothingType()) == ModelBipedClothingAdapter.ClothingType.CLOAK) {
                return true;
            }
        }
        return false;
    }

    private static String rad2deg(ModelBiped m, String part) {
        net.minecraft.client.model.ModelRenderer r;
        if (part.equals("body")) r = m.bipedBody;
        else if (part.equals("armR")) r = m.bipedRightArm;
        else if (part.equals("armL")) r = m.bipedLeftArm;
        else if (part.equals("legR")) r = m.bipedRightLeg;
        else if (part.equals("legL")) r = m.bipedLeftLeg;
        else if (part.equals("head")) r = m.bipedHead;
        else return "?";
        if (r == null) return "null";
        return String.format("(%.1f,%.1f,%.1f)",
            r.rotateAngleX * 180f / (float) Math.PI,
            r.rotateAngleY * 180f / (float) Math.PI,
            r.rotateAngleZ * 180f / (float) Math.PI);
    }

    private static void logAngles(String tag, ModelBiped m) {
        if (m == null) { System.out.println("[Adapter-Diag] " + tag + " null"); return; }
        System.out.println("[Adapter-Diag] " + tag
            + " head=" + rad2deg(m, "head")
            + " body=" + rad2deg(m, "body")
            + " armR=" + rad2deg(m, "armR")
            + " armL=" + rad2deg(m, "armL")
            + " legR=" + rad2deg(m, "legR")
            + " legL=" + rad2deg(m, "legL"));
    }

    /**
     * Box inflation per type. PANTS = TFC+ ModelPants(0.25f); 0.5 default keeps
     * the user-validated look for other types (TFC+ uses 0.3/0.2/0.6 — see PANTS.md §6).
     */
    private float clothingScale(ModelBipedClothingAdapter.ClothingType type) {
        switch (type) {
            case PANTS:
            case SHORTS: return 0.25F;
            case SOCKS: return 0.2F;
            case BOOTS: return 0.6F;
            case FULLBOOTS: return 0.5F;
            case SANDALS: return 0.4F;
            case COAT:
            case ROBE: return 0.6F;
            default: return 0.5F;
        }
    }

    private boolean isSleevelessShirt(net.minecraft.item.Item item) {
        return item == com.dunk.tfc.api.TFCItems.woolSleevelessShirt
            || item == com.dunk.tfc.api.TFCItems.cottonSleevelessShirt
            || item == com.dunk.tfc.api.TFCItems.linenSleevelessShirt
            || item == com.dunk.tfc.api.TFCItems.silkSleevelessShirt;
    }

    private boolean isShorts(net.minecraft.item.Item item) {
        return item == com.dunk.tfc.api.TFCItems.woolShorts
            || item == com.dunk.tfc.api.TFCItems.cottonShorts
            || item == com.dunk.tfc.api.TFCItems.linenShorts
            || item == com.dunk.tfc.api.TFCItems.silkShorts
            || item == com.dunk.tfc.api.TFCItems.leatherShorts;
    }

    private ModelBipedClothingAdapter.ClothingType mapClothingType(
            com.dunk.tfc.api.Interfaces.IEquipable.ClothingType tfcType) {
        switch (tfcType) {
            case SHIRT:
            case THINSHIRT: return ModelBipedClothingAdapter.ClothingType.SHIRT;
            case PANTS:
            case THINPANTS: return ModelBipedClothingAdapter.ClothingType.PANTS;
            case SOCKS: return ModelBipedClothingAdapter.ClothingType.SOCKS;
            case BOOTS: return ModelBipedClothingAdapter.ClothingType.BOOTS;
            case FULLBOOTS: return ModelBipedClothingAdapter.ClothingType.FULLBOOTS;
            case SANDALS: return ModelBipedClothingAdapter.ClothingType.SANDALS;
            case CLOTH_HAT: return ModelBipedClothingAdapter.ClothingType.CLOTH_HAT;
            case STRAW_HAT: return ModelBipedClothingAdapter.ClothingType.STRAW_HAT;
            case COAT:
            case HEAVYCOAT:
            case HEAVIERCOAT: return ModelBipedClothingAdapter.ClothingType.COAT;
            case ROBE: return ModelBipedClothingAdapter.ClothingType.ROBE;
            case CLOAK: return ModelBipedClothingAdapter.ClothingType.CLOAK;
            default: return null;
        }
    }

    private List<ItemStack> collectClothing(EntityPlayer player) {
        List<ItemStack> result = new ArrayList<ItemStack>();

        if (!extraEquipFieldResolved) {
            try {
                extraEquipField = player.inventory.getClass().getField("extraEquipInventory");
            } catch (NoSuchFieldException nsfe) {
                extraEquipField = null;
            }
            extraEquipFieldResolved = true;
        }

        if (extraEquipField != null) {
            try {
                ItemStack[] extra = (ItemStack[]) extraEquipField.get(player.inventory);
                if (extra != null) {
                    for (ItemStack is : extra) {
                        if (is != null && is.getItem() instanceof com.dunk.tfc.api.Interfaces.IEquipable) {
                            result.add(is);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } else {
            collectFromPlayerInfo(player, result);
        }

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

