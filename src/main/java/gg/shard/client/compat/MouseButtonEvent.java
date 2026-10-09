package gg.shard.client.compat;

// Before 1.21.9 Screen input took loose arguments (x, y, button / key, scancode, modifiers). This
// stand-in for net.minecraft.client.input.MouseButtonEvent keeps Shard's screens written against
// the event records; compat.InputScreen turns the loose calls into events. The stonecutter
// replacement points imports of the vanilla record here on older versions.
//? if <1.21.9 {
/*public record MouseButtonEvent(double x, double y, int button, int modifiers) {
    public MouseButtonEvent(double x, double y, int button) {
        this(x, y, button, KeyEvent.currentModifiers());
    }
}
*///?}
