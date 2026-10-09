package com.raishxn.gtoa.godforge.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import com.mojang.blaze3d.systems.RenderSystem;

import java.lang.reflect.Method;

/**
 * Optional Oculus (Iris) bridge, by reflection so the addon runs without it. With a shader pack the star is drawn
 * after the pack's final pass (hooked by the {@code gtoa_godforge_oculus_final} coremod), straight onto the main
 * target, as GTLAdditions does for its Forge of the Antichrist.
 */
final class GodforgeOculus {

    private GodforgeOculus() {}

    private static boolean resolved;
    private static Object irisApi;
    private static Method isShaderPackInUse;
    private static Method isDepthColorLocked, unlockDepthColor, isBlendLocked, restoreBlend;

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!ModList.get().isLoaded("oculus")) return;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApi = api.getMethod("getInstance").invoke(null);
            isShaderPackInUse = api.getMethod("isShaderPackInUse");
            Class<?> depth = Class.forName("net.irisshaders.iris.gl.blending.DepthColorStorage");
            isDepthColorLocked = depth.getMethod("isDepthColorLocked");
            unlockDepthColor = depth.getMethod("unlockDepthColor");
            Class<?> blend = Class.forName("net.irisshaders.iris.gl.blending.BlendModeStorage");
            isBlendLocked = blend.getMethod("isBlendLocked");
            restoreBlend = blend.getMethod("restoreBlend");
        } catch (ReflectiveOperationException | LinkageError e) {
            irisApi = null;
            isShaderPackInUse = null;
        }
    }

    static boolean shaderPackInUse() {
        resolve();
        if (isShaderPackInUse == null) return false;
        try {
            return (boolean) isShaderPackInUse.invoke(irisApi);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** Binds the main target and lifts Iris' depth/colour and blend locks for a plain vanilla-style draw. */
    static void beginMainTarget() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        resolve();
        try {
            if (isDepthColorLocked != null && (boolean) isDepthColorLocked.invoke(null)) unlockDepthColor.invoke(null);
            if (isBlendLocked != null && (boolean) isBlendLocked.invoke(null)) restoreBlend.invoke(null);
        } catch (ReflectiveOperationException ignored) {}
        RenderSystem.defaultBlendFunc();
    }
}
