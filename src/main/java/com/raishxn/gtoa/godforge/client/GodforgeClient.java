package com.raishxn.gtoa.godforge.client;

import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client hooks of the Forge of Gods: star/beam shaders and the late world-render pass of the star. */
public final class GodforgeClient {

    private GodforgeClient() {}

    public static void init(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener(ForgeOfGodsRenderer::onRenderLevelStage);
        modBus.addListener((RegisterShadersEvent event) -> {
            try {
                GodforgeShaders.register(event);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }
}
