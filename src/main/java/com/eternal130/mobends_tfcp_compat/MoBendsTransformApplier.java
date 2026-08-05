package com.eternal130.mobends_tfcp_compat;

import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

/**
 * Replays Mo'Bends' whole-body transform on the current GL matrix so that post-render passes
 * (TFC+ clothing, in our case) sit in body space rather than vanilla pose space.
 *
 * <p>Mo'Bends applies this transform inside {@code RenderBendsPlayer.rotateCorpse} via
 * {@code ModelBendsPlayer.postRender(scale, entityHeight)}. But that happens inside the push/pop
 * block of {@code RenderLivingEntity.doRender} — by the time {@code RenderPlayerEvent.Post} fires
 * the matrix has already been popped, so any third-party Post handler renders in vanilla space.
 *
 * <p>This class simply re-applies the same {@code postRender} call. {@link ModelBendsPlayer} exposes
 * it publicly, and all its rotation/offset fields ({@code renderOffset}, {@code renderRotation},
 * {@code centerRotation}, {@code centerQuat}) are {@code public} too, so we don't need to reach
 * into private state.</p>
 *
 * <p>Crucially, Mo'Bends keeps the {@link ModelBendsPlayer} instance up to date with per-frame data
 * from {@code Data_Player} inside {@code rotateCorpse} itself (via
 * {@code updateWithEntityData + postRender}), so by the time Post fires the model's smoothed
 * fields already hold the correct values for this frame. We don't recompute.</p>
 */
public final class MoBendsTransformApplier {

    /** Vanilla player scale used everywhere in 1.7.10 ModelBiped rendering. */
    public static final float MODEL_SCALE = 0.0625F;

    private MoBendsTransformApplier() {}

    /**
     * Apply Mo'Bends' body transform to the current GL matrix.
     *
     * <p>No-op (and safe) when the renderer is not Mo'Bends or the model has been swapped to
     * vanilla (e.g. while wearing a Fisk superhero suit, which Mo'Bends explicitly hands off to
     * vanilla animation for).</p>
     */
    public static void applyForPlayer(RenderPlayer renderer, EntityPlayer player, float partialTicks) {
        if (renderer == null || player == null) {
            return;
        }

        ModelBiped model = renderer.modelBipedMain;
        if (!(model instanceof ModelBendsPlayer)) {
            // Vanilla renderer or superhero override — nothing to apply.
            return;
        }

        ModelBendsPlayer bendsModel = (ModelBendsPlayer) model;

        // entityHeight is the same value Mo'Bends uses in rotateCorpse (default 1.8F for players;
        // AbstractClientPlayer.height would be 1.8F too). Hardcoding 1.8F matches Mo'Bends' default
        // overload and avoids relying on a player entity being non-null here.
        float entityHeight = (player != null) ? player.height : 1.8F;
        if (entityHeight <= 0.0F) {
            entityHeight = 1.8F;
        }

        // Reproduce exactly what Mo'Bends did inside rotateCorpse for this frame. This applies, in
        // order: renderOffset translate, then centerRotation/centerQuat pivoting at body mid-height,
        // then renderRotation at the feet/origin. After this the matrix matches body space.
        bendsModel.postRender(MODEL_SCALE, entityHeight);
    }
}
