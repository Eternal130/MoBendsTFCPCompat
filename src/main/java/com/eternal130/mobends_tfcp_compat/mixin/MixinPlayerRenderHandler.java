package com.eternal130.mobends_tfcp_compat.mixin;

import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.renderer.entity.RenderBendsPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dunk.tfc.Handlers.Client.PlayerRenderHandler;

import cpw.mods.fml.common.Loader;
import net.minecraftforge.client.event.RenderPlayerEvent;

/**
 * Neutralises the {@code e.renderer.modelArmor = new ModelBiped(0.75f)} line in TFC+'s
 * {@code PlayerRenderHandler.onPlayerRenderTick}. That line unconditionally replaces the
 * renderer's armor-leggings model with a vanilla {@code ModelBiped} the first time it runs,
 * which destroys Mo'Bends' {@code ModelBendsPlayer(0.5F)} — without it, leggings render via
 * vanilla {@code ModelBox} geometry (no bend animation, body box renders at the wrong
 * position producing the "belt box at the feet" symptom) and never receive Mo'Bends'
 * per-part pose sync.
 *
 * <p>This mixin restores {@code modelArmor} to a cached {@code ModelBendsPlayer(0.5F)}
 * immediately after {@code onPlayerRenderTick} returns, but only when:
 * <ul>
 *   <li>Mo'Bends is loaded (otherwise the vanilla ModelBiped is correct), and</li>
 *   <li>the renderer is a {@code RenderBendsPlayer} (otherwise there's nothing to fix).</li>
 * </ul>
 * The replacement model is created once and cached per-{@code RenderBendsPlayer} instance so
 * Mo'Bends' animation state (smoothed rotations, etc.) persists across frames.
 */
@Mixin(value = PlayerRenderHandler.class, remap = false)
public abstract class MixinPlayerRenderHandler {

    private static final String MODID_MOBENDS = "mobends";
    private static final float LEGGINGS_INFLATE = 0.5F;

    @Inject(method = "onPlayerRenderTick(Lnet/minecraftforge/client/event/RenderPlayerEvent$Pre;)V", at = @At("RETURN"), remap = false)
    private void mobends_tfcp_compat$restoreBendsArmorModel(RenderPlayerEvent.Pre e, CallbackInfo ci) {
        if (!Loader.isModLoaded(MODID_MOBENDS)) return;
        if (!(e.renderer instanceof RenderBendsPlayer)) return;
        RenderBendsPlayer renderer = (RenderBendsPlayer) e.renderer;
        if (renderer.modelArmor instanceof ModelBendsPlayer) return;
        renderer.modelArmor = new ModelBendsPlayer(LEGGINGS_INFLATE);
    }
}
