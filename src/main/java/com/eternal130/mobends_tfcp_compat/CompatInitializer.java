package com.eternal130.mobends_tfcp_compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.IEventListener;
import net.minecraftforge.common.MinecraftForge;

/**
 * Detects both Mo'Bends and TFC+ at startup, then replaces TFC+'s {@code PlayerRenderHandler} with
 * a compatible version ({@link CompatPlayerRenderHandler}) that applies Mo'Bends' whole-body
 * transform before rendering clothing.
 *
 * The replacement is done by reflection because TFC+'s handler is registered as an anonymous
 * instance and Forge 1.7.10 has no public unregister-by-instance API.
 */
public final class CompatInitializer {

    public static final String MODID_MOBENDS = "mobends";
    public static final String MODID_TFCP = "terrafirmacraftplus";

    private static final String TFC_HANDLER_CLASS = "com.dunk.tfc.Handlers.Client.PlayerRenderHandler";

    private CompatInitializer() {}

    public static void initialize() {
        boolean mobendsPresent = Loader.isModLoaded(MODID_MOBENDS);
        boolean tfcpPresent = Loader.isModLoaded(MODID_TFCP);

        if (!mobendsPresent || !tfcpPresent) {
            MoBendsTFCPCompat.LOG.info(
                "MoBends TFCP Compat: skipping (mobends={}, tfcp={}). Both mods must be present.",
                mobendsPresent,
                tfcpPresent);
            return;
        }

        unregisterTfcHandler();
        MinecraftForge.EVENT_BUS.register(new CompatPlayerRenderHandler());
        MoBendsTFCPCompat.LOG.info("MoBends TFCP Compat: active. TFC+'s PlayerRenderHandler replaced.");
    }

    /**
     * Remove every Forge listener whose target class is TFC+'s PlayerRenderHandler.
     *
     * Forge 1.7.10 keeps listeners in per-event-type arrays of {@link IEventListener}; we walk them
     * and drop matching ones. This is the cleanest "unregister by type" available without poking
     * Forge internals.
     */
    private static void unregisterTfcHandler() {
        Class<?> handlerClass;
        try {
            handlerClass = Class.forName(TFC_HANDLER_CLASS);
        } catch (ClassNotFoundException e) {
            MoBendsTFCPCompat.LOG.warn(
                "MoBends TFCP Compat: TFC+ PlayerRenderHandler class not found, no replacement done.");
            return;
        }

        int removed = EventBusReflection.unregisterByTargetClass(MinecraftForge.EVENT_BUS, handlerClass);
        MoBendsTFCPCompat.LOG
            .info("MoBends TFCP Compat: removed {} TFC+ listener(s) from EVENT_BUS.", removed);
    }
}
