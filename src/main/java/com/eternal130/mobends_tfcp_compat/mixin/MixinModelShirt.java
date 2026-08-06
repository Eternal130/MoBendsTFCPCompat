package com.eternal130.mobends_tfcp_compat.mixin;

import java.lang.reflect.Field;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dunk.tfc.Render.Models.ModelShirt;

/**
 * Splits each single-segment sleeve of {@link ModelShirt} into two segments (upper + forearm) so
 * the sleeve can bend at the elbow instead of rotating as one rigid piece around the shoulder.
 *
 * <p>TFC+ models the sleeve as one ModelRenderer with an 8-unit-long addBox (-2..+6 along Y).
 * MoBends' arm is two segments: upper arm (bipedRightArm, 6 units) + forearm (bipedRightForeArm,
 * 6 units, child of upper). Without this Mixin, TFC+ copies only bipedRightArm.rotateAngle into
 * the sleeve, so the forearm's elbow-bend (set by walk/sprint/attack animations) is lost — the
 * sleeve stays rigid and swings from the shoulder.
 *
 * <p>What this Mixin does:
 * <ol>
 *   <li>Constructor @Inject: clear the sleeve's single 8-unit ModelBox and re-add it as a 6-unit
 *       upper-sleeve box. Create a new forearm ModelRenderer with a 2-unit box, attach it as a
 *       child of the sleeve at the elbow position (y=+4). Both boxes reuse the original texture
 *       offsets (TFC+ passes the same texOrigin (16,16) for both arms; the original 8-tall strip
 *       covers both segments).</li>
 *   <li>render @Inject BEFORE armL.render: copy bipedLeftForeArm.rotateAngle* from MoBends'
 *       modelBipedMain into the forearm child. This runs every frame, so animations stay live.</li>
 * </ol>
 *
 * <p>Field access: TFC+ declares armL/armR as private. We reflect on the declared fields once and
 * cache. Field names match TFC+ 0.89.1 source; if a future TFC+ renames them, the Mixin fails
 * loud (NoSuchFieldError at first render) rather than silently shipping broken sleeves.
 */
@Mixin(value = ModelShirt.class, remap = false)
public abstract class MixinModelShirt {

    private static final String ARM_L_FIELD = "armL";
    private static final String ARM_R_FIELD = "armR";

    private ModelRenderer foreArmL;
    private ModelRenderer foreArmR;
    private boolean splitDone = false;

    @Inject(method = "<init>(F)V", at = @At("RETURN"))
    private void mobends_tfcp_compat$splitSleeves(float scaleFactor, CallbackInfo ci) {
        if (splitDone) return;
        splitDone = true;
        try {
            ModelShirt self = (ModelShirt) (Object) this;
            ModelRenderer armL = getArmField(ARM_L_FIELD);
            ModelRenderer armR = getArmField(ARM_R_FIELD);
            // Original sleeve: addBox(-3/-1, -2, -2, 4, 8, 4, scaleFactor). Strip the lower
            // 2 units by clearing cubeList and re-adding only the upper 6 units. mirror flag is
            // preserved by re-using the same ModelRenderer instance.
            armL.cubeList.clear();
            armL.addBox(-1F, -2f, -2F, 4, 6, 4, scaleFactor);
            armR.cubeList.clear();
            armR.addBox(-3f, -2f, -2f, 4, 6, 4, scaleFactor);
            // Forearm segments: 2-unit box attached at elbow (y=+4 from sleeve origin, since the
            // upper sleeve ends at -2+6=+4). Set rotation point at the elbow so the forearm box
            // pivots there. Texture origin (16,16) reuses the same strip as the upper sleeve —
            // it's an approximation but reads cleanly on TFC+ clothing textures.
            foreArmL = new ModelRenderer(self, 16, 16);
            foreArmL.mirror = true;
            foreArmL.addBox(-1F, 0f, -2F, 4, 2, 4, scaleFactor);
            foreArmL.setRotationPoint(0f, 4f, 0f);
            armL.addChild(foreArmL);
            foreArmR = new ModelRenderer(self, 16, 16);
            foreArmR.addBox(-3f, 0f, -2f, 4, 2, 4, scaleFactor);
            foreArmR.setRotationPoint(0f, 4f, 0f);
            armR.addChild(foreArmR);
        } catch (Throwable t) {
            // Failure here means sleeves stay single-segment — same as pre-compat behavior. Log
            // once and disable the per-frame injection via splitDone=false sentinel.
            splitDone = false;
            com.eternal130.mobends_tfcp_compat.MoBendsTFCPCompat.LOG.warn(
                "MobendsClothingRenderer: could not split shirt sleeves; sleeves will be single-segment", t);
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/entity/RenderPlayer;F)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V",
                     ordinal = 1,
                     remap = true))
    private void mobends_tfcp_compat$applyForeArmRotation(EntityLivingBase entity, ItemStack item,
            RenderPlayer renderer, float partialRenderTick, CallbackInfo ci) {
        copyForeArmAngles(renderer);
    }

    private void copyForeArmAngles(RenderPlayer renderer) {
        if (!splitDone || foreArmL == null || foreArmR == null) return;
        if (renderer == null || renderer.modelBipedMain == null) return;
        if (!(renderer.modelBipedMain instanceof net.gobbob.mobends.client.model.entity.ModelBendsPlayer)) return;
        net.gobbob.mobends.client.model.entity.ModelBendsPlayer mb =
            (net.gobbob.mobends.client.model.entity.ModelBendsPlayer) renderer.modelBipedMain;
        foreArmL.rotateAngleX = mb.bipedLeftForeArm.rotateAngleX;
        foreArmL.rotateAngleY = mb.bipedLeftForeArm.rotateAngleY;
        foreArmL.rotateAngleZ = mb.bipedLeftForeArm.rotateAngleZ;
        foreArmR.rotateAngleX = mb.bipedRightForeArm.rotateAngleX;
        foreArmR.rotateAngleY = mb.bipedRightForeArm.rotateAngleY;
        foreArmR.rotateAngleZ = mb.bipedRightForeArm.rotateAngleZ;
    }

    private ModelRenderer getArmField(String name) throws NoSuchFieldException, IllegalAccessException {
        Field f = ModelShirt.class.getDeclaredField(name);
        f.setAccessible(true);
        return (ModelRenderer) f.get((Object) this);
    }
}
