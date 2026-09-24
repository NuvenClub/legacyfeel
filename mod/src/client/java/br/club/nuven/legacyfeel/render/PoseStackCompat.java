package br.club.nuven.legacyfeel.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionfc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Bridges the rotation method renamed in Minecraft 26.3. */
public final class PoseStackCompat {
    private static final Method ROTATE = findRotationMethod();

    private PoseStackCompat() {}

    public static void rotate(PoseStack poseStack, Quaternionfc rotation) {
        try {
            ROTATE.invoke(poseStack, rotation);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Não foi possível aplicar uma rotação à pose", exception);
        }
    }

    private static Method findRotationMethod() {
        for (String name : new String[] {"rotate", "mulPose"}) {
            for (Method method : PoseStack.class.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(org.joml.Quaternionf.class)) {
                    return method;
                }
            }
        }
        throw new IllegalStateException("PoseStack não oferece um método de rotação compatível");
    }
}
