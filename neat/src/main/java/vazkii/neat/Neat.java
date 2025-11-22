package vazkii.neat;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(
    modid = "Neat",
    name = "Neat",
    version = "{{version}}"
)
public class Neat {
    public static final String MOD_ID = "Neat";
    public static final String MOD_NAME = "Neat";
    public static final String BUILD = "GRADLE:BUILD";
    public static final String VERSION = "{{version}}";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NeatConfig.init(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new HealthBarRenderer());
    }
}
