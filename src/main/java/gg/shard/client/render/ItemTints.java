package gg.shard.client.render;

/**
 * Colour maths for the Shield tint/opacity and Hit Color on armour, plus the "shield being drawn
 * now" context the shield and banner mixins read. Pure Java so it can be unit-tested; the render
 * thread is the only writer and reader of the context.
 */
public final class ItemTints {
    /** No tint: vanilla's white, opaque. */
    public static final int NONE = 0xFFFFFFFF;

    /** ARGB multiplier for the first-person shield being submitted right now, or {@link #NONE}. */
    private static int shield = NONE;

    private ItemTints() {}

    /** Called around the first-person shield's submit; {@link #NONE} ends it. */
    public static void beginShield(int argb) {
        shield = argb;
    }

    public static void endShield() {
        shield = NONE;
    }

    /** The current shield multiplier ({@link #NONE} outside a first-person shield). */
    public static int shield() {
        return shield;
    }

    /**
     * Before 1.21.9 armour is drawn while its wearer renders, without the wearer's render state:
     * LivingEntityRendererMixin marks a wearer with the hurt overlay here for EquipmentLayerRendererMixin.
     */
    private static boolean wearerHurt;

    public static void setWearerHurt(boolean hurt) {
        wearerHurt = hurt;
    }

    public static boolean wearerHurt() {
        return wearerHurt;
    }

    /** True while a see-through shield is being drawn (needs a translucent render type). */
    public static boolean shieldTranslucent() {
        return alpha(shield) < 255;
    }

    /** Shield multiplier: the tint's RGB with alpha from the opacity percent (clamped 10-100). */
    public static int shieldColor(int opacityPercent, int tintArgb) {
        int pct = Math.max(10, Math.min(100, opacityPercent));
        int a = Math.round(255 * pct / 100f);
        return (a << 24) | (tintArgb & 0xFFFFFF);
    }

    /**
     * Hit tint for layers that ignore the overlay texture (armour): white blended towards the hit
     * colour by the strength percent, opaque, so multiplying a texture by it reddens it the way
     * vanilla's overlay reddens the body.
     */
    public static int hitTint(int colorArgb, int strengthPercent) {
        float t = Math.max(0, Math.min(100, strengthPercent)) / 100f;
        int r = Math.round(255 + (((colorArgb >> 16) & 0xFF) - 255) * t);
        int g = Math.round(255 + (((colorArgb >> 8) & 0xFF) - 255) * t);
        int b = Math.round(255 + ((colorArgb & 0xFF) - 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Channel-wise ARGB multiply (what vanilla's ARGB.multiply does). */
    public static int multiply(int a, int b) {
        if (a == NONE) return b;
        if (b == NONE) return a;
        return (mul(a >>> 24, b >>> 24) << 24) | (mul((a >> 16) & 0xFF, (b >> 16) & 0xFF) << 16)
                | (mul((a >> 8) & 0xFF, (b >> 8) & 0xFF) << 8) | mul(a & 0xFF, b & 0xFF);
    }

    /** Held-item size factor from a percent, never below 50 % or above 100 %. */
    public static float itemScale(int percent) {
        return Math.max(50, Math.min(100, percent)) / 100f;
    }

    private static int mul(int x, int y) {
        return x * y / 255;
    }

    private static int alpha(int argb) {
        return argb >>> 24;
    }
}
