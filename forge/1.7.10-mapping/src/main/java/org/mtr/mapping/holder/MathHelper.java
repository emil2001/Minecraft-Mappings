package org.mtr.mapping.holder;

public final class MathHelper {
    private MathHelper() {}
    public static int floor(double value) { return net.minecraft.util.MathHelper.floor_double(value); }
    public static double fractionalPart(double value) { return value - Math.floor(value); }
    public static int clamp(int value, int min, int max) { return net.minecraft.util.MathHelper.clamp_int(value, min, max); }
    public static float clamp(float value, float min, float max) { return net.minecraft.util.MathHelper.clamp_float(value, min, max); }
    public static double clamp(double value, double min, double max) { return net.minecraft.util.MathHelper.clamp_double(value, min, max); }
    public static float getSquareRootOfTwoMapped() { return (float) Math.sqrt(2); }
}
