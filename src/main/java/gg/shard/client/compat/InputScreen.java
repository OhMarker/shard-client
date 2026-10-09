package gg.shard.client.compat;

// Before 1.21.9 Screen's input methods took loose arguments. DesignScreen (the base of every Shard
// screen) extends this on those versions: the loose calls become the event-style methods 1.21.9
// introduced, and those fall back to vanilla's loose handling, so the screens override and call
// super with events on every version.
//? if <1.21.9 {
/*import net.minecraft.util.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class InputScreen extends Screen {
    /^* 1.21.9's MouseHandler double-click window (same button within 250 ms). ^/
    private static final long DOUBLE_CLICK_MS = 250;
    private long lastClick;
    private int lastButton = -1;

    protected InputScreen(Component title) {
        super(title);
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        long now = Util.getMillis();
        boolean doubleClick = button == lastButton && now - lastClick < DOUBLE_CLICK_MS;
        lastClick = now;
        lastButton = button;
        return mouseClicked(new MouseButtonEvent(x, y, button), doubleClick);
    }

    @Override
    public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return mouseDragged(new MouseButtonEvent(x, y, button), dx, dy);
    }

    @Override
    public final boolean mouseReleased(double x, double y, int button) {
        return mouseReleased(new MouseButtonEvent(x, y, button));
    }

    @Override
    public final boolean keyPressed(int key, int scancode, int modifiers) {
        return keyPressed(new KeyEvent(key, scancode, modifiers));
    }

    @Override
    public final boolean charTyped(char c, int modifiers) {
        return charTyped(new CharacterEvent(c, modifiers));
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(event.x(), event.y(), event.button(), dx, dy);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public boolean charTyped(CharacterEvent event) {
        return super.charTyped((char) event.codepoint(), event.modifiers());
    }
}
*///?}
