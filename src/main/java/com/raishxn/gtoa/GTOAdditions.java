package com.raishxn.gtoa;

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.addon.AddonFinder;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GTOAdditions.MOD_ID)
public final class GTOAdditions implements IGTAddon {
    public static final String MOD_ID = "gtoa";
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(MOD_ID);

    public static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public GTOAdditions(FMLJavaModLoadingContext context) {
        REGISTRATE.registerEventListeners(context.getModEventBus());
        GTOAItems.register(context.getModEventBus());
        com.raishxn.gtoa.godforge.GodforgeItems.register(context.getModEventBus());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.raishxn.gtoa.godforge.client.GodforgeClient.init(context.getModEventBus());
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(com.raishxn.gtoa.godforge.GodforgeCommands::register);
        // Discovery supplies our resource namespace; it does not register machines.
        AddonFinder.add(this);
        context.getModEventBus().addListener(this::onLoadComplete);
    }

    @Override
    public GTRegistrate getRegistrate() { return REGISTRATE; }

    @Override
    public String addonModId() { return MOD_ID; }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        MachineRegistrationState.requireRegistered();
        GTOAMachines.validateLoaded();
    }
}
