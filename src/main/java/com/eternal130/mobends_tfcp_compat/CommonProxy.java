package com.eternal130.mobends_tfcp_compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Common (server) proxy. The Mo'Bends/TFC+ integration is entirely client-side (rendering), so
 * this is a no-op on a dedicated server. The client proxy logs whether both target mods are
 * present so users can spot missing-dependency installs.
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        boolean mobends = Loader.isModLoaded(MoBendsTFCPCompat.MODID_MOBENDS);
        boolean tfcp = Loader.isModLoaded(MoBendsTFCPCompat.MODID_TFCP);
        if (mobends && tfcp) {
            MoBendsTFCPCompat.LOG.info("MoBends TFCP Compat: both mods detected on server (no-op).");
        } else {
            MoBendsTFCPCompat.LOG.info(
                "MoBends TFCP Compat: inactive (mobends={}, tfcp={}). Both mods required for the fix.",
                mobends,
                tfcp);
        }
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}
}
