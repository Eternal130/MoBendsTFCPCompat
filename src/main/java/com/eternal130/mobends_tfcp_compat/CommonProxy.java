package com.eternal130.mobends_tfcp_compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Common (server) proxy. The Mo'Bends/TFC+ rendering integration is client-only, so this is a
 * no-op on the server. The client proxy overrides preInit to wire up the compat handler.
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        if (Loader.isModLoaded(CompatInitializer.MODID_MOBENDS)
            && Loader.isModLoaded(CompatInitializer.MODID_TFCP)) {
            MoBendsTFCPCompat.LOG.info("MoBends TFCP Compat: both mods detected on server side (no-op).");
        } else {
            MoBendsTFCPCompat.LOG.info(
                "MoBends TFCP Compat: inactive (mobends={}, tfcp={}).",
                Loader.isModLoaded(CompatInitializer.MODID_MOBENDS),
                Loader.isModLoaded(CompatInitializer.MODID_TFCP));
        }
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}
}
