package com.eternal130.mobends_tfcp_compat;

import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

/**
 * Applies Mo'Bends' whole-body transform to the current GL matrix.
 *
 * <p>This is the heart of the compat layer. The transform is identical to the one Mo'Bends applies
 * inside {@code RenderBendsPlayer.rotateCorpse} via {@code ModelBendsPlayer.postRender(scale, height)}.
 * The catch: that call happens inside the {@code RenderLivingEntity.doRender} push/pop block, which
 * has already been closed by the time {@code RenderPlayerEvent.Post} fires. So any third-party Post
 * listener that tries to render body-attached geometry (TFC+ clothing) ends up in vanilla pose space.
 *
 * <p>The fix is a Mixin into {@code RenderClothing.doRender} that invokes this method at the correct
 * matrix slot: <b>after</b> TFC+ has finished rebuilding "entity at feet, vanilla model space"
 * (the {@code -cpPos + entityPos} repositioning, scale, 180° flips and yaw), and <b>before</b>
 * {@code switchRender} draws the clothing boxes. Applying the body transform at that slot mirrors
 * the main-pass order exactly: {@code View × T(entityFeet) × R(yaw) × MoBendsPostRender × PartLocal}.
 *
 * <p>{@link ModelBendsPlayer#postRender(float, float)} is public, as are all the fields it reads
 * ({@code renderOffset.vSmooth}, {@code renderRotation.vSmooth}, {@code centerRotation.vSmooth},
 * {@code centerQuat}). Those fields are guaranteed to hold this-frame-this-entity values at Post
 * time, because the main render pass synchronously completes before {@code RenderPlayerEvent.Post}
 * fires for the same entity.
 */
public final class MoBendsTransformApplier {

    /** Vanilla ModelBiped render scale (1/16 of a block per model unit). */
    public static final float MODEL_SCALE = 0.0625F;

    private MoBendsTransformApplier() {}

    /**
     * Apply Mo'Bends' whole-body transform for the player being rendered by {@code renderer}.
     *
     * <p>Safe to call when {@code renderer.modelBipedMain} is not a {@link ModelBendsPlayer} (e.g.
     * Mo'Bends is in vanilla-fallback mode for a Fisk superhero suit) — the call becomes a no-op.
     *
     * @param renderer the RenderPlayer that TFC+ was passed (normally {@code RenderBendsPlayer})
     * @param entity   the entity being rendered (used only for its height; may be null for 1.8F)
     */
    public static void apply(RenderPlayer renderer, Entity entity) {
        if (renderer == null) {
            return;
        }

        ModelBiped model = renderer.modelBipedMain;
        if (!(model instanceof ModelBendsPlayer)) {
            // Mo'Bends not in use, or temporarily swapped to vanilla ModelBiped for a compat case.
            return;
        }

        ModelBendsPlayer bendsModel = (ModelBendsPlayer) model;
        float entityHeight = (entity != null) ? entity.height : 1.8F;
        if (entityHeight <= 0.0F) {
            entityHeight = 1.8F;
        }

        // Reproduce exactly what Mo'Bends does in rotateCorpse for this frame. This applies, in
        // order: renderOffset translate, then centerRotation/centerQuat pivoting at body mid-height,
        // then renderRotation at the feet/origin. After this, the current frame is in "body space".
        bendsModel.postRender(MODEL_SCALE, entityHeight);
    }
}
