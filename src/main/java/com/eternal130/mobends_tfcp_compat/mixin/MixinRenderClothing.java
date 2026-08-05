package com.eternal130.mobends_tfcp_compat.mixin;

import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dunk.tfc.Render.RenderClothing;
import com.eternal130.mobends_tfcp_compat.MoBendsTransformApplier;

/**
 * Injects Mo'Bends' whole-body transform into {@link RenderClothing#doRender} at the correct matrix
 * slot: immediately before {@code switchRender} draws the clothing boxes.
 *
 * <p>By the time {@code switchRender} is called, {@code RenderClothing.doRender} has already
 * rebuilt the GL matrix into "entity at feet, vanilla model space" (the {@code -cpPos + entityPos}
 * repositioning, the {@code glScalef(-1,-1,1)}, the two 180° flips and the renderYawOffset
 * rotation). This is exactly the slot where Mo'Bends applies its body transform during the main
 * render pass — so applying it here makes clothing follow the bent torso instead of staying in the
 * vanilla pose.
 *
 * <p>The push/pop pair around the transform is critical: {@code switchRender} (and the models it
 * dispatches to) do their own pushes and assumes the matrix is balanced on return. We push before
 * the transform and pop after {@code switchRender} returns so the matrix stays balanced for the
 * rest of {@code doRender}.
 *
 * <p>This Mixin targets only the 5-argument {@code doRender} overload (the one TFC+'s Post-handler
 * invokes for player clothing). The 4-argument overload is a different code path used elsewhere and
 * is left untouched.
 */
@Mixin(value = RenderClothing.class, remap = false)
public abstract class MixinRenderClothing {

    /**
     * Push and apply Mo'Bends' body transform immediately before {@code switchRender} is invoked.
     *
     * <p>The injection target is the 5-arg {@code switchRender} call on the same object. We use
     * {@code ordinal = 0} to disambiguate from any other call site.
     */
    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;FLnet/minecraft/client/renderer/entity/RenderPlayer;[Lnet/minecraft/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                     target = "Lcom/dunk/tfc/Render/RenderClothing;switchRender(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/entity/RenderPlayer;F[Lnet/minecraft/item/ItemStack;)V",
                     ordinal = 0))
    private void mobends_tfcp_compat$beforeSwitchRender(EntityLivingBase entity, ItemStack item,
            float partialRenderTick, RenderPlayer renderer, ItemStack[] armor, CallbackInfo ci) {
        GL11.glPushMatrix();
        MoBendsTransformApplier.apply(renderer, entity);
    }

    /**
     * Pop the matrix pushed by {@link #mobends_tfcp_compat$beforeSwitchRender} after
     * {@code switchRender} returns, restoring balance for the rest of {@code doRender}.
     */
    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;FLnet/minecraft/client/renderer/entity/RenderPlayer;[Lnet/minecraft/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                     target = "Lcom/dunk/tfc/Render/RenderClothing;switchRender(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/entity/RenderPlayer;F[Lnet/minecraft/item/ItemStack;)V",
                     ordinal = 0,
                     shift = At.Shift.AFTER))
    private void mobends_tfcp_compat$afterSwitchRender(EntityLivingBase entity, ItemStack item,
            float partialRenderTick, RenderPlayer renderer, ItemStack[] armor, CallbackInfo ci) {
        GL11.glPopMatrix();
    }
}
