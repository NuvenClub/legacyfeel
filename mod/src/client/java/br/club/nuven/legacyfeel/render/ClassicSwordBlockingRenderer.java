package br.club.nuven.legacyfeel.render;

import br.club.nuven.legacyfeel.mixin.ItemStackRenderStateAccessor;
import br.club.nuven.legacyfeel.mixin.LayerRenderStateAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import br.club.nuven.legacyfeel.render.PoseStackCompat;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;

public final class ClassicSwordBlockingRenderer {
    private ClassicSwordBlockingRenderer() {}

    public static <S extends EntityRenderState> void submit(S state, ArmedModel<S> model,
                                                             ItemStackRenderState item, HumanoidArm arm,
                                                             PoseStack poseStack,
                                                             SubmitNodeCollector submitNodeCollector,
                                                             int lightCoords, int outlineColor) {
        poseStack.pushPose();
        model.translateToHand(state, arm, poseStack);
        boolean leftHand = arm == HumanoidArm.LEFT;
        applyBlockingTransform(poseStack, leftHand);
        ItemStackRenderState.LayerRenderState firstLayer =
            ((ItemStackRenderStateAccessor)(Object)item).legacyfeel$firstLayer();
        ItemTransform transform = ((LayerRenderStateAccessor)(Object)firstLayer).legacyfeel$itemTransform();
        undoModernItemTransform(transform, leftHand, poseStack);
        item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    private static void applyBlockingTransform(PoseStack poseStack, boolean leftHand) {
        poseStack.translate((leftHand ? 1.0F : -1.0F) / 16.0F, 0.4375F, 0.0625F);
        poseStack.translate(leftHand ? -0.035F : 0.05F, leftHand ? 0.045F : 0.0F,
            leftHand ? -0.135F : -0.1F);
        PoseStackCompat.rotate(poseStack, Axis.YP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -50.0F));
        PoseStackCompat.rotate(poseStack, Axis.XP.rotationDegrees(-10.0F));
        PoseStackCompat.rotate(poseStack, Axis.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -60.0F));

        poseStack.translate(0.0F, 0.1875F, 0.0F);
        poseStack.scale(0.625F, 0.625F, 0.625F);
        PoseStackCompat.rotate(poseStack, Axis.XP.rotationDegrees(180.0F));
        PoseStackCompat.rotate(poseStack, Axis.XN.rotationDegrees(-100.0F));
        PoseStackCompat.rotate(poseStack, Axis.YN.rotationDegrees(leftHand ? 35.0F : 45.0F));

        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        PoseStackCompat.rotate(poseStack, Axis.YN.rotationDegrees(50.0F));
        PoseStackCompat.rotate(poseStack, Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
        poseStack.translate(0.5F, 0.5F, 0.25F);
        PoseStackCompat.rotate(poseStack, Axis.YN.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.28125F);
    }

    private static void undoModernItemTransform(ItemTransform transform, boolean leftHand, PoseStack poseStack) {
        if (transform == ItemTransform.NO_TRANSFORM) return;

        float angleX = transform.rotation().x();
        float angleY = leftHand ? -transform.rotation().y() : transform.rotation().y();
        float angleZ = leftHand ? -transform.rotation().z() : transform.rotation().z();
        Quaternionf inverseRotation = new Quaternionf().rotationXYZ(
            angleX * ((float)Math.PI / 180.0F),
            angleY * ((float)Math.PI / 180.0F),
            angleZ * ((float)Math.PI / 180.0F)).conjugate();

        poseStack.scale(1.0F / transform.scale().x(), 1.0F / transform.scale().y(),
            1.0F / transform.scale().z());
        PoseStackCompat.rotate(poseStack, inverseRotation);
        poseStack.translate((leftHand ? -1.0F : 1.0F) * -transform.translation().x(),
            -transform.translation().y(), -transform.translation().z());
    }
}
