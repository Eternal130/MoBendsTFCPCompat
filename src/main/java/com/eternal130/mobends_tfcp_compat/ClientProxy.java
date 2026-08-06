package com.eternal130.mobends_tfcp_compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Client proxy. Registers {@link MobendsClothingRenderer} on the Forge event bus so it can
 * render TFC+ clothing inside MoBends' coordinate frame at {@code RenderPlayerEvent.Specials.Post}.
 *
 * <p>The Mixin on {@code RenderClothing.doRender} auto-registers via the mixin config and
 * disables TFC+'s own clothing rendering.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        boolean mobends = Loader.isModLoaded(MoBendsTFCPCompat.MODID_MOBENDS);
        boolean tfcp = Loader.isModLoaded(MoBendsTFCPCompat.MODID_TFCP);
        if (mobends && tfcp) {
            MoBendsTFCPCompat.LOG.info("MoBends TFCP Compat: both mods present, clothing will render in MoBends frame.");
        } else {
            MoBendsTFCPCompat.LOG.warn(
                "MoBends TFCP Compat: inactive (mobends={}, tfcp={}). The mod needs BOTH to do anything.",
                mobends,
                tfcp);
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Register the clothing renderer on the Forge event bus. Done in init (not preInit) so all
        // mods' classes are guaranteed loaded — MobendsClothingRenderer references TFC+ classes.
        MinecraftForge.EVENT_BUS.register(new MobendsClothingRenderer());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {}
}
