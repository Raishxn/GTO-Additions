package com.raishxn.gtoa.godforge.client;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.raishxn.gtoa.godforge.api.GodforgeStarColor;
import com.raishxn.gtoa.godforge.ForgeOfGodsMachine;
import com.raishxn.gtoa.godforge.GodforgeRings;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Forge of Gods star, rings and beam, ported from GTNH {@code RenderForgeOfGods} and {@code TileEntityForgeOfGods}
 * (GT5-Unofficial a3e1e112). The star sits 122 blocks behind the controller, as the GTNH render block does; the rings
 * are the real ring blocks taken into the controller, spinning around the beam axis.
 */
public class ForgeOfGodsRenderer extends WorkableCasingMachineRenderer {

    public ForgeOfGodsRenderer() {
        super(com.gtolib.GTOCore.id("block/transcendentally_amplified_magnetic_confinement_casing"),
                com.raishxn.gtoa.GTOAdditions.id("block/machines/forge_of_gods"));
    }

    public static ForgeOfGodsRenderer create() {
        return new ForgeOfGodsRenderer();
    }

    private static final ResourceLocation STAR_LAYER_0 = com.raishxn.gtoa.GTOAdditions.id("textures/render/godforge/star_layer_0.png");
    private static final ResourceLocation STAR_LAYER_1 = com.raishxn.gtoa.GTOAdditions.id("textures/render/godforge/star_layer_1.png");
    private static final ResourceLocation STAR_LAYER_2 = com.raishxn.gtoa.GTOAdditions.id("textures/render/godforge/star_layer_2.png");
    private static final ResourceLocation BEAM_TEXTURE = com.raishxn.gtoa.GTOAdditions.id("textures/render/godforge/space_layer.png");

    public static final int STAR_DISTANCE = 122;

    private record StarKey(BlockPos pos, Direction front, Direction up, boolean flipped) {}

    private static final Map<StarKey, BlockPos> STARS = new HashMap<>();

    /**
     * The star sits in the middle of the first ring. GTNH places it 122 blocks behind the controller; GTO's pattern
     * orientation differs, so the centre is taken from the real ring 1 blocks instead.
     */
    static BlockPos star(ForgeOfGodsMachine machine) {
        var key = new StarKey(machine.getPos(), machine.getFrontFacing(), machine.getUpwardsFacing(), machine.isFlipped());
        return STARS.computeIfAbsent(key, k -> {
            var blocks = GodforgeRings.positions(machine, machine.getLevel(), machine.getPos(), machine.getFrontFacing(),
                    machine.getUpwardsFacing(), machine.isFlipped(), 1).keySet();
            if (blocks.isEmpty()) return machine.getPos().relative(machine.getFrontFacing().getOpposite(), STAR_DISTANCE);
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos p : blocks) {
                minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
                minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
                minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
            }
            return new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        });
    }

    /** The beam and ring spin axis, as in GTNA: from the controller towards the star. */
    static Direction starDirection(ForgeOfGodsMachine machine) {
        BlockPos d = star(machine).subtract(machine.getPos());
        return Direction.getNearest(d.getX(), d.getY(), d.getZ());
    }

    private static final int STAR_TESSELLATION = 128;
    private static final int BEAM_SEGMENT_QUADS = 16;
    private static final float BACK_PLATE_DISTANCE = -121.5f, BACK_PLATE_RADIUS = 13f;
    private static final float COLOR_CYCLE_SPEED = 16f;
    private static final double RING_RADIUS = 63;
    private static final double BEAM_LENGTH = 59;

    @Nullable
    private static VertexBuffer sphere;
    @Nullable
    private static VertexBuffer beamBuffer;
    private static final Map<BlockPos, ClientState> STATES = new HashMap<>();
    @Nullable
    private static Level statesLevel;

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasTESR(BlockEntity blockEntity) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getViewDistance() {
        return 512;
    }

    /**
     * Forges seen by the block entity pass. They stay here while their controller is loaded, so the star (122 blocks
     * away) keeps drawing when the controller itself is out of the block entity render distance or frustum.
     */
    private static final Map<BlockPos, ForgeOfGodsMachine> FORGES = new java.util.LinkedHashMap<>();

    /** Frame captured while a shader pack is active, drawn once the pack has composed the final image. */
    private record PostFinal(Vec3 camera, Matrix4f pose, Matrix4f projection, float partialTicks) {}

    @Nullable
    private static PostFinal postFinal;

    /**
     * The block entity pass only registers the forge (as GTLAdditions' Forge of the Antichrist does); drawing happens
     * in {@link #onRenderLevelStage}, where the depth buffer and GL state belong to the world render.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        if (!(blockEntity instanceof MetaMachineBlockEntity holder) ||
                !(holder.getMetaMachine() instanceof ForgeOfGodsMachine machine)) return;
        if (machine.getLevel() == null) return;
        FORGES.put(machine.getPos().immutable(), machine);
    }

    public static void onRenderLevelStage(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS ||
                FORGES.isEmpty()) {
            return;
        }
        float partialTicks = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        if (GodforgeOculus.shaderPackInUse()) {
            // Shader packs draw the world into their own buffers and compose it afterwards; anything drawn here with
            // our own shaders never reaches the screen. The rings use the terrain shader, which the pack replaces, so
            // they are drawn now and lit by the pack like the rest of the forge; star and beam come after the pack's
            // final pass.
            drawAll(event.getCamera().getPosition(), poseStack, new Matrix4f(event.getProjectionMatrix()),
                    partialTicks, Pass.RINGS);
            postFinal = new PostFinal(event.getCamera().getPosition(), new Matrix4f(poseStack.last().pose()),
                    new Matrix4f(event.getProjectionMatrix()), partialTicks);
            return;
        }
        drawAll(event.getCamera().getPosition(), poseStack, new Matrix4f(event.getProjectionMatrix()), partialTicks,
                Pass.ALL);
    }

    /** Called by the coremod hook at the end of Oculus' {@code IrisRenderingPipeline.finalizeLevelRendering}. */
    public static void afterShaderPackFinal() {
        PostFinal frame = postFinal;
        postFinal = null;
        if (frame == null || FORGES.isEmpty()) return;
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var previousSorting = RenderSystem.getVertexSorting();
        GodforgeOculus.beginMainTarget();
        try {
            RenderSystem.setProjectionMatrix(frame.projection(), com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
            PoseStack poseStack = new PoseStack();
            poseStack.mulPoseMatrix(frame.pose());
            drawAll(frame.camera(), poseStack, frame.projection(), frame.partialTicks(), Pass.STAR);
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    private enum Pass {
        ALL, RINGS, STAR;

        boolean rings() {
            return this != STAR;
        }

        boolean star() {
            return this != RINGS;
        }
    }

    private static void drawAll(Vec3 camera, PoseStack poseStack, Matrix4f projection, float partialTicks, Pass pass) {
        Level current = Minecraft.getInstance().level;
        try {
            var it = FORGES.values().iterator();
            while (it.hasNext()) {
                ForgeOfGodsMachine machine = it.next();
                if (machine.isRemoved() || machine.getLevel() != current) {
                    it.remove();
                    continue;
                }
                if (!machine.isRenderActive() || !machine.isFormed()) continue;
                BlockPos pos = machine.getPos();
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                drawForge(machine, partialTicks, poseStack, projection, pass);
                poseStack.popPose();
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
        }
    }

    /** GTNH {@code RenderForgeOfGods.renderTileEntityAt}: opaque star, rings, transparent star shells, beam. */
    private static void drawForge(ForgeOfGodsMachine machine, float partialTicks, PoseStack poseStack,
                                  Matrix4f projection, Pass pass) {
        ShaderInstance starShader = GodforgeShaders.star();
        ShaderInstance beamShader = GodforgeShaders.beam();
        Level level = machine.getLevel();
        if (starShader == null || beamShader == null || level == null) return;

        if (pass.star()) ForgeOfGodsSound.ensurePlaying(machine);
        ClientState state = state(machine, level);
        state.updateColor(machine.starColor());
        // Continuous client clock (ticks). The level game time is re-synced by the server and could step back, which
        // made the rings stutter. The 432000-tick period keeps every ring spin (7/6, 5/4, 3 deg/tick) a whole number
        // of turns, so the wrap is seamless.
        float timer = (float) ((net.minecraft.Util.getMillis() % (432_000L * 50L)) / 50.0);
        Direction back = starDirection(machine);
        Vector3f axis = new Vector3f(back.getStepX(), back.getStepY(), back.getStepZ());
        BlockPos starPos = star(machine).subtract(machine.getPos());
        Quaternionf beamRotation = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), axis);
        float radius = machine.starSize();
        int rings = Math.max(1, Math.min(3, machine.ringAmount()));

        poseStack.pushPose();
        poseStack.translate(0.5 + starPos.getX(), 0.5 + starPos.getY(), 0.5 + starPos.getZ());
        Matrix4f center = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();
        float starTimer = timer * machine.rotationSpeed();

        // 1) Opaque star writes depth.
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        if (pass.star()) {
            starShader.safeGetUniform("Gamma").set(state.gamma);
            renderStarLayer(starShader, center, projection, state, 1f, STAR_LAYER_0, radius,
                    new Vector3f(0, 1, 1).normalize(), 130 + starTimer % 360000);
        }

        // 2) Rings: solid and depth writing.
        RenderSystem.enableCull();
        if (pass.rings()) renderRings(machine, level, state, center, projection, axis, timer, rings);
        if (!pass.star()) return;

        // 3) Transparent star shells: depth tested, no depth write.
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        renderStarLayer(starShader, center, projection, state, 0.4f, STAR_LAYER_1, radius * 1.02f,
                new Vector3f(1, 1, 0).normalize(), -49 + starTimer % 360000);
        renderStarLayer(starShader, center, projection, state, 0.2f, STAR_LAYER_2, radius * 1.04f,
                new Vector3f(1, 0, 1).normalize(), 67 + starTimer % 360000);

        // 4) Beam.
        renderBeam(machine, beamShader, center, projection, beamRotation, state, timer, radius, rings);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    // ---- Star ----

    private static void renderStarLayer(ShaderInstance shader, Matrix4f center, Matrix4f projection,
                                        ClientState state, float alpha, ResourceLocation texture, float scale,
                                        Vector3f rotationAxis, float degrees) {
        VertexBuffer mesh = sphere();
        Matrix4f model = new Matrix4f(center)
                .rotate((float) Math.toRadians(degrees), rotationAxis)
                .scale(scale);
        RenderSystem.setShaderTexture(0, texture);
        shader.safeGetUniform("StarColor").set(state.r, state.g, state.b, alpha);
        mesh.bind();
        mesh.drawWithShader(model, projection, shader);
        VertexBuffer.unbind();
    }

    /** {@code EOHRenderingUtils.buildSphere}: a unit UV sphere. */
    private static VertexBuffer sphere() {
        if (sphere != null) return sphere;
        BufferBuilder builder = new BufferBuilder(STAR_TESSELLATION * STAR_TESSELLATION * 6 * 20);
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        int slices = STAR_TESSELLATION, stacks = STAR_TESSELLATION;
        for (int i = 0; i < stacks; i++) {
            float v0 = (float) i / stacks, v1 = (float) (i + 1) / stacks;
            double phi0 = Math.PI / 2.0 - i * Math.PI / stacks;
            double phi1 = Math.PI / 2.0 - (i + 1) * Math.PI / stacks;
            float y0 = (float) Math.sin(phi0), y1 = (float) Math.sin(phi1);
            double r0 = Math.cos(phi0), r1 = Math.cos(phi1);
            for (int j = 0; j < slices; j++) {
                float u0 = 1f - (float) j / slices, u1 = 1f - (float) (j + 1) / slices;
                double th0 = j * 2.0 * Math.PI / slices, th1 = (j + 1) * 2.0 * Math.PI / slices;
                float x00 = (float) (r0 * Math.cos(th0)), z00 = (float) (r0 * Math.sin(th0));
                float x10 = (float) (r1 * Math.cos(th0)), z10 = (float) (r1 * Math.sin(th0));
                float x11 = (float) (r1 * Math.cos(th1)), z11 = (float) (r1 * Math.sin(th1));
                float x01 = (float) (r0 * Math.cos(th1)), z01 = (float) (r0 * Math.sin(th1));
                builder.vertex(x00, y0, z00).uv(u0, v0).endVertex();
                builder.vertex(x10, y1, z10).uv(u0, v1).endVertex();
                builder.vertex(x11, y1, z11).uv(u1, v1).endVertex();
                builder.vertex(x00, y0, z00).uv(u0, v0).endVertex();
                builder.vertex(x11, y1, z11).uv(u1, v1).endVertex();
                builder.vertex(x01, y0, z01).uv(u1, v0).endVertex();
            }
        }
        sphere = new VertexBuffer(VertexBuffer.Usage.STATIC);
        sphere.bind();
        sphere.upload(builder.end());
        VertexBuffer.unbind();
        return sphere;
    }

    // ---- Rings ----

    private static void renderRings(ForgeOfGodsMachine machine, Level level, ClientState state, Matrix4f center,
                                    Matrix4f projection, Vector3f axis, float timer, int rings) {
        ShaderInstance shader = GameRenderer.getRendertypeCutoutShader();
        if (shader == null) return;
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        float[] spins = { timer / 6 * 7, -timer / 4 * 5, timer * 3 };
        for (int ring = 1; ring <= rings; ring++) {
            VertexBuffer mesh = state.ring(machine, level, ring);
            if (mesh == null) continue;
            Matrix4f model = new Matrix4f(center)
                    .rotate((float) Math.toRadians(spins[ring - 1]), axis)
                    .translate(-0.5f, -0.5f, -0.5f);
            mesh.bind();
            mesh.drawWithShader(model, projection, shader);
        }
        VertexBuffer.unbind();
    }

    /** {@code StructureVBO.build}: the ring blocks around the star, hidden faces between ring blocks culled. */
    @Nullable
    private static VertexBuffer buildRing(ForgeOfGodsMachine machine, Level level, int ring) {
        Map<BlockPos, Character> blocks = GodforgeRings.positions(machine, level, machine.getPos(), machine.getFrontFacing(),
                machine.getUpwardsFacing(), machine.isFlipped(), ring);
        if (blocks.isEmpty()) return null;
        BlockPos star = star(machine);
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create(42);
        BufferBuilder builder = new BufferBuilder(blocks.size() * 4 * DefaultVertexFormat.BLOCK.getVertexSize());
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        PoseStack poseStack = new PoseStack();
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState blockState = GodforgeRings.blockFor(entry.getValue()).defaultBlockState();
            BakedModel model = dispatcher.getBlockModel(blockState);
            poseStack.pushPose();
            poseStack.translate(pos.getX() - star.getX(), pos.getY() - star.getY(), pos.getZ() - star.getZ());
            // Lit like the terrain around it: the world light where the ring block stands (sky and block light,
            // which the light map turns into day/night) and the vanilla per-face shade.
            int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
            for (Direction side : Direction.values()) {
                if (blocks.containsKey(pos.relative(side))) continue;
                random.setSeed(42);
                for (BakedQuad quad : model.getQuads(blockState, side, random, ModelData.EMPTY, null)) {
                    float shade = level.getShade(quad.getDirection(), quad.isShade());
                    builder.putBulkData(poseStack.last(), quad, shade, shade, shade, light, OverlayTexture.NO_OVERLAY);
                }
            }
            random.setSeed(42);
            for (BakedQuad quad : model.getQuads(blockState, null, random, ModelData.EMPTY, null)) {
                float shade = level.getShade(quad.getDirection(), quad.isShade());
                builder.putBulkData(poseStack.last(), quad, shade, shade, shade, light, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
        VertexBuffer mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
        mesh.bind();
        mesh.upload(builder.end());
        VertexBuffer.unbind();
        return mesh;
    }

    // ---- Beam ----

    private static void renderBeam(ForgeOfGodsMachine machine, ShaderInstance shader, Matrix4f center,
                                   Matrix4f projection, Quaternionf beamRotation, ClientState state, float timer,
                                   float radius, int rings) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 starCenter = Vec3.atCenterOf(star(machine));
        Vec3 relative = camera.getPosition().subtract(starCenter);
        Vector3f cameraLocal = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z)
                .rotate(new Quaternionf(beamRotation).conjugate());
        float cameraAngle = (float) Math.atan2(cameraLocal.y, cameraLocal.x);
        Matrix4f model = new Matrix4f(center).rotate(beamRotation);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, BEAM_TEXTURE);
        shader.safeGetUniform("CameraLocal").set(cameraLocal.x, cameraLocal.y, cameraLocal.z);

        // Soft beam in the star color, then the intense white core.
        shader.safeGetUniform("BeamColor").set(state.r, state.g, state.b);
        shader.safeGetUniform("Intensity").set(2f);
        drawBeam(shader, model, projection, softBeam(radius, rings), cameraAngle, timer);
        shader.safeGetUniform("BeamColor").set(1f, 1f, 1f);
        shader.safeGetUniform("Intensity").set(4f);
        drawBeam(shader, model, projection, intenseBeam(radius, rings), cameraAngle, timer);
    }

    /** One segment per row: radius, z offset along the beam, transparency ({@code u_SegmentArray}). */
    private static float[][] softBeam(float starRadius, int rings) {
        float angle = startAngle(starRadius, rings);
        float radius = starRadius * 1.1f;
        float startX = -radius * (float) Math.cos(angle);
        float startY = radius * (float) Math.sin(angle);
        float[][] segments = new float[rings + 2][];
        int i = 0;
        segments[i++] = new float[] { startY, startX, 0 };
        for (int lens = rings - 1; lens >= 0; lens--) {
            segments[i++] = new float[] { lensRadius(lens), lensDistance(lens), 1f };
        }
        segments[i] = new float[] { BACK_PLATE_RADIUS, BACK_PLATE_DISTANCE, -.05f };
        return segments;
    }

    private static float[][] intenseBeam(float starRadius, int rings) {
        float angle = startAngle(starRadius, rings);
        float radius = starRadius * 1.05f;
        float startX = -radius * (float) Math.cos(angle);
        float startY = radius * (float) Math.sin(angle);
        int firstLens = rings - 1;
        float nextX = lensDistance(firstLens);
        float nextY = lensRadius(firstLens) * .75f;
        float backX = Math.max(-radius, (nextX + radius) / 2);
        float backY = interpolate(startX, nextX, startY, nextY, backX);
        float[][] segments = new float[rings + 3][];
        int i = 0;
        segments[i++] = new float[] { backY, backX, 0 };
        float transparency = .2f;
        for (int lens = rings - 1; lens >= 0; lens--) {
            segments[i++] = new float[] { lensRadius(lens) / 2, lensDistance(lens), transparency };
            transparency += .3f;
        }
        float currX = lensDistance(0);
        float currY = lensRadius(0) / 2;
        float lastX = BACK_PLATE_DISTANCE;
        float lastY = Math.min(lensRadius(firstLens), BACK_PLATE_RADIUS);
        float midX = lastX + 8f;
        float midY = interpolate(currX, lastX, currY, lastY, midX);
        segments[i++] = new float[] { midY, midX, transparency };
        segments[i] = new float[] { lastY, lastX, 0f };
        return segments;
    }

    /** {@code gorgeBeam.vert}: a half cylinder per segment, turned towards the camera. */
    private static void drawBeam(ShaderInstance shader, Matrix4f model, Matrix4f projection, float[][] segments,
                                 float cameraAngle, float time) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        float shift = cameraAngle - (float) Math.PI / 2;
        float timer = time / 240f;
        for (int s = 0; s + 1 < segments.length; s++) {
            float[] seg0 = segments[s], seg1 = segments[s + 1];
            for (int q = 0; q < BEAM_SEGMENT_QUADS; q++) {
                float a0 = (float) Math.PI * q / BEAM_SEGMENT_QUADS + shift;
                float a1 = (float) Math.PI * (q + 1) / BEAM_SEGMENT_QUADS + shift;
                beamVertex(builder, a1, seg1, timer);
                beamVertex(builder, a1, seg0, timer);
                beamVertex(builder, a0, seg0, timer);
                beamVertex(builder, a0, seg1, timer);
            }
        }
        if (beamBuffer == null) beamBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        beamBuffer.bind();
        beamBuffer.upload(builder.end());
        beamBuffer.drawWithShader(model, projection, shader);
        VertexBuffer.unbind();
    }

    private static void beamVertex(BufferBuilder builder, float angle, float[] segment, float timer) {
        float radius = segment[0], offset = segment[1], transparency = segment[2];
        float x = (float) Math.cos(angle) * radius, y = (float) Math.sin(angle) * radius;
        float heightOffset = offset / 256 + timer;
        float v = angle / (2 * (float) Math.PI) + heightOffset / 3 + timer;
        builder.vertex(x, y, offset).uv(heightOffset, v)
                .color(1f, 1f, 1f, Math.max(0f, Math.min(1f, transparency)))
                .normal((float) Math.cos(angle), (float) Math.sin(angle), 0).endVertex();
    }

    private static float lensDistance(int lens) {
        return switch (lens) {
            case 0 -> -61.5f;
            case 1 -> -54.5f;
            default -> -44.5f;
        };
    }

    private static float lensRadius(int lens) {
        return switch (lens) {
            case 0 -> 1.1f;
            case 1 -> 3.5f;
            default -> 5f;
        };
    }

    private static float startAngle(float radius, int rings) {
        float x = -lensDistance(rings - 1);
        float y = lensRadius(rings - 1);
        float alpha = (float) Math.atan2(y, x);
        float beta = (float) Math.asin(Math.min(1, radius / Math.sqrt(x * x + y * y)));
        return alpha + ((float) Math.PI / 2 - beta);
    }

    private static float interpolate(float x0, float x1, float y0, float y1, float x) {
        return y0 + ((x - x0) * (y1 - y0)) / (x1 - x0);
    }

    // ---- Client state ----

    private static ClientState state(ForgeOfGodsMachine machine, Level level) {
        if (statesLevel != level) {
            STATES.values().forEach(ClientState::close);
            STATES.clear();
            statesLevel = level;
        }
        return STATES.computeIfAbsent(machine.getPos().immutable(), pos -> new ClientState());
    }

    /** Ring meshes and the color cycle of {@code TileEntityForgeOfGods}. */
    private static final class ClientState {

        private final VertexBuffer[] rings = new VertexBuffer[3];
        private Direction ringFacing;
        private Direction ringUp;
        private long ringsBuiltAt;
        private String colorKey = "";
        private GodforgeStarColor color = GodforgeStarColor.DEFAULT;
        private float r, g, b, gamma = GodforgeStarColor.DEFAULT_GAMMA;
        private long lastColorUpdate;
        private float cycleStep;
        private int interpIndex;
        private int interpA, interpB;
        private float gammaA, gammaB;

        @Nullable
        VertexBuffer ring(ForgeOfGodsMachine machine, Level level, int ring) {
            // The baked world light follows lighting changes (and chunks loading in) every ten seconds.
            long now = level.getGameTime();
            if (ringFacing != machine.getFrontFacing() || ringUp != machine.getUpwardsFacing() ||
                    Math.abs(now - ringsBuiltAt) > 200) {
                close();
                ringsBuiltAt = now;
                ringFacing = machine.getFrontFacing();
                ringUp = machine.getUpwardsFacing();
            }
            if (rings[ring - 1] == null) rings[ring - 1] = buildRing(machine, level, ring);
            return rings[ring - 1];
        }

        void close() {
            for (int i = 0; i < rings.length; i++) {
                if (rings[i] != null) rings[i].close();
                rings[i] = null;
            }
        }

        void updateColor(String serialized) {
            if (!serialized.equals(colorKey)) {
                colorKey = serialized;
                GodforgeStarColor parsed = GodforgeStarColor.deserialize(serialized);
                setColor(parsed == null || parsed.numColors() == 0 ? GodforgeStarColor.DEFAULT : parsed);
            }
            incrementColors();
        }

        private void setColor(GodforgeStarColor color) {
            this.color = color;
            var first = color.color(0);
            apply(rgb(first), first.gamma());
            lastColorUpdate = 0;
            interpIndex = 0;
            if (color.numColors() > 1) {
                cycleStep = 0;
                interpA = rgb(first);
                gammaA = first.gamma();
                var next = color.color(1);
                interpB = rgb(next);
                gammaB = next.gamma();
                interpIndex = 1;
            }
        }

        private void incrementColors() {
            if (color.numColors() <= 1) return;
            long now = System.currentTimeMillis();
            if (lastColorUpdate == 0) {
                lastColorUpdate = now;
                return;
            }
            long delta = now - lastColorUpdate;
            lastColorUpdate = now;
            cycleStep += color.cycleSpeed() * (delta / COLOR_CYCLE_SPEED);
            while (cycleStep >= 255f) {
                cycleStep -= 255f;
                interpA = interpB;
                gammaA = gammaB;
                interpIndex = (interpIndex + 1) % color.numColors();
                var next = color.color(interpIndex);
                interpB = rgb(next);
                gammaB = next.gamma();
            }
            float position = cycleStep / 255f;
            int red = lerp(interpA >> 16 & 255, interpB >> 16 & 255, position);
            int green = lerp(interpA >> 8 & 255, interpB >> 8 & 255, position);
            int blue = lerp(interpA & 255, interpB & 255, position);
            apply(red << 16 | green << 8 | blue, gammaA + (gammaB - gammaA) * position);
        }

        private void apply(int rgb, float gamma) {
            r = (rgb >> 16 & 255) / 255f;
            g = (rgb >> 8 & 255) / 255f;
            b = (rgb & 255) / 255f;
            this.gamma = gamma;
        }

        private static int rgb(GodforgeStarColor.Setting setting) {
            return setting.r() << 16 | setting.g() << 8 | setting.b();
        }

        private static int lerp(int a, int b, float t) {
            return Math.round(a + (b - a) * t);
        }
    }
}
