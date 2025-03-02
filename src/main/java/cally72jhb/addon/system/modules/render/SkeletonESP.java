package cally72jhb.addon.system.modules.render;

import cally72jhb.addon.system.categories.Categories;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;

public class SkeletonESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> skeletonColorSetting = sgGeneral.add(new ColorSetting.Builder()
        .name("players-color")
        .description("The other player's color.")
        .defaultValue(new SettingColor(140, 245, 165))
        .build()
    );

    public final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder()
        .name("distance-colors")
        .description("Changes the color of skeletons depending on distance.")
        .defaultValue(true)
        .build()
    );

    private Freecam freecam;

    public SkeletonESP() {
        super(Categories.Misc, "skeleton-esp", "Renders the skeleton of players.");
    }

    @Override
    public void onActivate() {
        freecam = Modules.get().get(Freecam.class);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        MatrixStack matrixStack = event.matrices;

        float delta = event.tickDelta;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
        RenderSystem.enableCull();

        mc.world.getEntities().forEach(entity -> {
            if (!(entity instanceof PlayerEntity player)) return;
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !freecam.isActive() && mc.player == entity) return;
            int rotationHoldTicks = Config.get().rotationHoldTicks.get();

            Color skeletonColor = PlayerUtils.getPlayerColor((PlayerEntity) entity, skeletonColorSetting.get());
            if (distance.get()) skeletonColor = getColorFromDistance(entity);

            Vec3d footPos = getEntityRenderPosition(player, delta);
            PlayerEntityRenderer livingEntityRenderer = (PlayerEntityRenderer) (LivingEntityRenderer<?, ?>) mc.getEntityRenderDispatcher().getRenderer(player);
            PlayerEntityModel playerEntityModel = livingEntityRenderer.getModel();

            float h = MathHelper.lerpAngleDegrees(delta, player.prevBodyYaw, player.bodyYaw);
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) h = Rotations.serverYaw;
            float j = MathHelper.lerpAngleDegrees(delta, player.prevHeadYaw, player.headYaw);
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) j = Rotations.serverYaw;

            float q = player.limbAngle - player.limbDistance * (1.0F - delta);
            float p = MathHelper.lerp(delta, player.lastLimbDistance, player.limbDistance);
            float o = (float) player.age + delta;
            float k = j - h;
            float m = player.getPitch(delta);
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) m = Rotations.serverPitch;

            playerEntityModel.animateModel(player, q, p, delta);
            playerEntityModel.setAngles(player, q, p, o, k, m);

            boolean swimming = player.isInSwimmingPose();
            boolean sneaking = player.isSneaking();
            boolean flying = player.isFallFlying();

            ModelPart head = playerEntityModel.head;
            ModelPart leftArm = playerEntityModel.leftArm;
            ModelPart rightArm = playerEntityModel.rightArm;
            ModelPart leftLeg = playerEntityModel.leftLeg;
            ModelPart rightLeg = playerEntityModel.rightLeg;

            matrixStack.translate(footPos.x, footPos.y, footPos.z);
            if (swimming) matrixStack.translate(0, 0.35f, 0);

            matrixStack.multiply(new Quaternion(new Vec3f(0, -1, 0), h + 180, true));
            if (swimming || flying) matrixStack.multiply(new Quaternion(new Vec3f(-1, 0, 0), 90 + m, true));
            if (swimming) matrixStack.translate(0, -0.95f, 0);

            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, sneaking ? 1.05f : 1.4f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//spine

            bufferBuilder.vertex(matrix4f, -0.37f, sneaking ? 1.05f : 1.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//shoulders
            bufferBuilder.vertex(matrix4f, 0.37f, sneaking ? 1.05f : 1.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//pelvis
            bufferBuilder.vertex(matrix4f, 0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            // Head
            matrixStack.push();
            matrixStack.translate(0, sneaking ? 1.05f : 1.4f, 0);
            rotate(matrixStack, head);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, 0.15f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            // Right Leg
            matrixStack.push();
            matrixStack.translate(0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
            rotate(matrixStack, rightLeg);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            // Left Leg
            matrixStack.push();
            matrixStack.translate(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
            rotate(matrixStack, leftLeg);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            // Right Arm
            matrixStack.push();
            matrixStack.translate(0.37f, sneaking ? 1.05f : 1.35f, 0);
            rotate(matrixStack, rightArm);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            // Left Arm
            matrixStack.push();
            matrixStack.translate(-0.37f, sneaking ? 1.05f : 1.35f, 0);
            rotate(matrixStack, leftArm);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);

            if (swimming) matrixStack.translate(0, 0.95f, 0);
            if (swimming || flying) matrixStack.multiply(new Quaternion(new Vec3f(1, 0, 0), 90 + m, true));
            if (swimming) matrixStack.translate(0, -0.35f, 0);

            matrixStack.multiply(new Quaternion(new Vec3f(0, 1, 0), h + 180, true));
            matrixStack.translate(-footPos.x, -footPos.y, -footPos.z);
        });

        RenderSystem.enableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void rotate(MatrixStack matrix, ModelPart modelPart) {
        if (modelPart.roll != 0.0F) {
            matrix.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(modelPart.roll));
        }

        if (modelPart.yaw != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_Y.getRadialQuaternion(modelPart.yaw));
        }

        if (modelPart.pitch != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_X.getRadialQuaternion(modelPart.pitch));
        }
    }

    private Vec3d getEntityRenderPosition(Entity entity, double partial) {
        double x = entity.prevX + ((entity.getX() - entity.prevX) * partial) - mc.getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.prevY + ((entity.getY() - entity.prevY) * partial) - mc.getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.prevZ + ((entity.getZ() - entity.prevZ) * partial) - mc.getEntityRenderDispatcher().camera.getPos().z;
        return new Vec3d(x, y, z);
    }

    private Color getColorFromDistance(Entity entity) {
        double distance = mc.gameRenderer.getCamera().getPos().distanceTo(entity.getPos());
        double percent = distance / 60;

        if (percent < 0 || percent > 1) {
            color.set(0, 255, 0, 255);
            return color;
        }

        int r, g;

        if (percent < 0.5) {
            r = 255;
            g = (int) (255 * percent / 0.5);
        }
        else {
            g = 255;
            r = 255 - (int) (255 * (percent - 0.5) / 0.5);
        }

        color.set(r, g, 0, 255);
        return color;
    }
}
