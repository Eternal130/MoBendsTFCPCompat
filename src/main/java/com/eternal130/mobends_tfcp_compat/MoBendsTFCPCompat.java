package com.eternal130.mobends_tfcp_compat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = MoBendsTFCPCompat.MODID, version = Tags.VERSION, name = "MoBends TFCP Compat",
        acceptedMinecraftVersions = "[1.7.10]")
public class MoBendsTFCPCompat {

    public static final String MODID = "mobends_tfcp_compat";
    public static final String MODID_MOBENDS = "mobends";
    public static final String MODID_TFCP = "terrafirmacraftplus";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.eternal130.mobends_tfcp_compat.ClientProxy",
            serverSide = "com.eternal130.mobends_tfcp_compat.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
