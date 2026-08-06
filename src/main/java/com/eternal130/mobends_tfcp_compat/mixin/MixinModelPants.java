package com.eternal130.mobends_tfcp_compat.mixin;

import java.lang.reflect.Field;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dunk.tfc.Render.Models.ModelPants;

/**
 * Splits each single-segment pants-leg of {@link ModelPants} into two segments (thigh + foreleg)
 * so the pant leg can bend at the knee instead of rotating as one rigid piece around the hip.
 *
 * <p>See {@link MixinModelShirt} for the matching sleeve split. Same mechanism: clear the original
 * 8-unit leg box, re-add the upper 6 units, create a foreleg ModelRenderer with the lower 2 units
 * and attach it as a child at the knee (y=+6). Per-frame @Inject copies
 * bipedRightForeLeg.rotateAngle* into the foreleg child.
 *
 * <p>Without this split, walking/running animations (which bend the knee via
 * bipedRightForeLeg.rotateAngleX, see Animation_Walk/Sprint) leave the pants-leg rigid and
 * pivoting from the hip — visually the cuff floats above where the foot actually is.
 */
@Mixin(value = ModelPants.class, remap = false)
public abstract class MixinModelPants {

    private static final String LEG_L_FIELD = "legL";
    private static final String LEG_R_FIELD = "legR";

    private ModelRenderer foreLegL;
    private ModelRenderer foreLegR;
    private boolean splitDone = false;

    @Inject(method = "<init>(F)V", at = @At("RETURN"))
    private void mobends_tfcp_compat$splitPantLegs(float scaleFactor, CallbackInfo ci) {
        if (splitDone) return;
        splitDone = true;
        try {
            ModelPants self = (ModelPants) (Object) this;
            ModelRenderer legL = getLegField(LEG_L_FIELD);
            ModelRenderer legR = getLegField(LEG_R_FIELD);
            // Original leg: addBox(-2, 0, -2, 4, 8, 4, scaleFactor). Strip the lower 2 units by
            // re-adding only the upper 6 (0..+6).
            legL.cubeList.clear();
            legL.addBox(-2, 0f, -2f, 4, 6, 4, scaleFactor);
            legR.cubeList.clear();
            legR.addBox(-2F, 0f, -2F, 4, 6, 4, scaleFactor);
            // Foreleg: 2-unit box at the knee (y=+6 from leg origin). Rotation point at the knee
            // so the box pivots there.
            foreLegL = new ModelRenderer(self, 16, 16);
            foreLegL.mirror = true;
            foreLegL.addBox(-2, 0f, -2f, 4, 2, 4, scaleFactor);
            foreLegL.setRotationPoint(0f, 6f, 0f);
            legL.addChild(foreLegL);
            foreLegR = new ModelRenderer(self, 16, 16);
            foreLegR.addBox(-2F, 0f, -2F, 4, 2, 4, scaleFactor);
            foreLegR.setRotationPoint(0f, 6f, 0f);
            legR.addChild(foreLegR);
        } catch (Throwable t) {
            splitDone = false;
            com.eternal130.mobends_tfcp_compat.MoBendsTFCPCompat.LOG.warn(
                "MobendsClothingRenderer: could not split pant legs; legs will be single-segment", t);
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/entity/RenderPlayer;F)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V",
                     ordinal = 1,
                     remap = true))
    private void mobends_tfcp_compat$applyForeLegRotation(EntityLivingBase entity, ItemStack item,
            RenderPlayer renderer, float partialRenderTick, CallbackInfo ci) {
        copyForeLegAngles(renderer);
    }

    private void copyForeLegAngles(RenderPlayer renderer) {
        if (!splitDone || foreLegL == null || foreLegR == null) return;
        if (renderer == null || renderer.modelBipedMain == null) return;
        if (!(renderer.modelBipedMain instanceof net.gobbob.mobends.client.model.entity.ModelBendsPlayer)) return;
        net.gobbob.mobends.client.model.entity.ModelBendsPlayer mb =
            (net.gobbob.mobends.client.model.entity.ModelBendsPlayer) renderer.modelBipedMain;
        foreLegL.rotateAngleX = mb.bipedLeftForeLeg.rotateAngleX;
        foreLegL.rotateAngleY = mb.bipedLeftForeLeg.rotateAngleY;
        foreLegL.rotateAngleZ = mb.bipedLeftForeLeg.rotateAngleZ;
        foreLegR.rotateAngleX = mb.bipedRightForeLeg.rotateAngleX;
        foreLegR.rotateAngleY = mb.bipedRightForeLeg.rotateAngleY;
        foreLegR.rotateAngleZ = mb.bipedRightForeLeg.rotateAngleZ;
    }

    private ModelRenderer getLegField(String name) throws NoSuchFieldException, IllegalAccessException {
        Field f = ModelPants.class.getDeclaredField(name);
        f.setAccessible(true);
        return (ModelRenderer) f.get((Object) this);
    }
}
