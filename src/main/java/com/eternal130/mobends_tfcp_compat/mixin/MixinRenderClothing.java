package com.eternal130.mobends_tfcp_compat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dunk.tfc.Render.RenderClothing;

/**
 * Completely disables TFC+'s {@link RenderClothing#doRender} so that clothing is rendered
 * exclusively by {@link com.eternal130.mobends_tfcp_compat.MobendsClothingRenderer} inside
 * MoBends' coordinate frame (at {@code RenderPlayerEvent.Specials.Post}).
 *
 * <p>TFC+'s {@code doRender} rebuilds the GL matrix from scratch via
 * {@code makeAdjustments} + {@code R(180,Z)} + {@code R(180,X)}, producing a frame that
 * doesn't match MoBends' body frame. This causes clothing to rotate around fixed world
 * axes instead of following the body's 3D orientation. By disabling this method entirely
 * and rendering clothing at {@code Specials.Post} (where MoBends' matrix is still on the
 * stack), we sidestep the entire frame-mismatch problem.
 *
 * <p>Both the float-3rd-arg and RenderPlayer-3rd-arg overloads are disabled. Only the
 * float-3rd is the live player-clothing path; the other is dead code, but we disable both
 * for safety.
 */
@Mixin(value = RenderClothing.class, remap = false)
public abstract class MixinRenderClothing {

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;FLnet/minecraft/client/renderer/entity/RenderPlayer;[Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void mobends_tfcp_compat$disableFloatThird(net.minecraft.entity.EntityLivingBase entity, net.minecraft.item.ItemStack item,
            float partialRenderTick, net.minecraft.client.renderer.entity.RenderPlayer renderer, net.minecraft.item.ItemStack[] armor, CallbackInfo ci) {
        ci.cancel();
    }
}
