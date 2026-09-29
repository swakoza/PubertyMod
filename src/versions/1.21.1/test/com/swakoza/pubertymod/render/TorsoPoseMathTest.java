package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.compat.PalPhysicsCompat;
import com.swakoza.pubertymod.compat.TorsoPoseMath;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class TorsoPoseMathTest {
    public static void main(String[] args) {
        int cases = 0;
        for (float pitch : new float[]{-1.3F, -0.6F, 0, 0.6F, 1.3F}) {
            PalPhysicsCompat.AttachmentPose pose = TorsoPoseMath.sample(new Matrix4f().rotateX(pitch));
            check(pose != null, "Valid torso pose rejected");
            check(Math.abs(pose.gravityX()) < 1.0E-5F, "Forward bend produced lateral force");
            check(Math.abs(pose.gravityY() - Math.cos(pitch)) < 1.0E-5F, "Vertical gravity differs from torso orientation");
            check(Math.abs(pose.gravityZ() + Math.sin(pitch)) < 1.0E-5F, "Forward gravity differs from torso orientation");
            cases++;
        }
        for (float roll : new float[]{-1.3F, -0.6F, 0, 0.6F, 1.3F}) {
            PalPhysicsCompat.AttachmentPose pose = TorsoPoseMath.sample(new Matrix4f().rotateZ(roll));
            check(pose != null && Math.abs(pose.gravityX() - Math.sin(roll)) < 1.0E-5F, "Side bend projects gravity incorrectly");
            check(Math.abs(pose.gravityZ()) < 1.0E-5F, "Side bend produced forward force");
            cases++;
        }
        check(TorsoPoseMath.sample(new Matrix4f().scale(0, 1, 1)) == null, "Singular scale accepted");
        check(TorsoPoseMath.sample(new Matrix4f().translation(Float.NaN, 0, 0)) == null, "Invalid translation accepted");
        check(TorsoPoseMath.sample(new Matrix4f().scale(1, 0.5F, 2)) != null, "Valid scaled emote rejected");
        PalPhysicsCompat.AttachmentPose translated = TorsoPoseMath.sample(new Matrix4f().translation(0.25F, -0.5F, 0.75F));
        check(Math.abs(translated.x() - 0.25F) < 1.0E-5F, "Lateral animation displacement changed sign");
        check(Math.abs(translated.y() - 0.5F) < 1.0E-5F, "Model Y was not converted to physical Y");
        check(Math.abs(translated.z() - 0.75F) < 1.0E-5F, "Forward animation displacement changed sign");
        // The torso center can stay fixed while an emote rotates the chest.
        PalPhysicsCompat.AttachmentPose still = new PalPhysicsCompat.AttachmentPose(
                0, 0, 0, 0, 1, 0, new Matrix3f());
        PalPhysicsCompat.AttachmentPose yawed = new PalPhysicsCompat.AttachmentPose(
                0, 0, 0, 0, 1, 0, new Matrix3f().rotateY(-0.6F));
        check(yawed.breastProbe(true).distance(still.breastProbe(true)) > 0.1F,
                "Torso yaw did not move the left attachment point");
        check(yawed.breastProbe(false).distance(still.breastProbe(false)) > 0.1F,
                "Torso yaw did not move the right attachment point");
        PalPhysicsCompat.AttachmentPose rolled = new PalPhysicsCompat.AttachmentPose(
                0, 0, 0, 0, 1, 0, new Matrix3f().rotateZ(-0.6F));
        check(rolled.breastProbe(true).y * rolled.breastProbe(false).y < 0,
                "Torso roll did not move breasts in opposite directions");
        System.out.println("Torso gravity and animation projection: " + (cases + 9) + " cases passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
