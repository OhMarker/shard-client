package gg.shard.client.gui;

import gg.shard.client.ShardClient;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * A screen that lays itself out in design units (see {@link Scale}). Subclasses call
 * {@link #beginFrame} at the top of {@code render}, draw between {@link #pushDesign} and
 * {@link #popDesign}, and receive mouse input already converted to design units through the
 * {@code design*} hooks. Scissor rectangles passed to {@code GuiGraphics.enableScissor} inside
 * the design pose are transformed by the pose too (checked against 1.21.11's
 * {@code ScreenRectangle.transformAxisAligned}), so clipping is also written in design units.
 *
 * <p>While a design screen is open, vanilla's menu blur radius follows Settings → Appearance →
 * Blur strength; the user's own value is restored when the screen goes away.
 */
public abstract class DesignScreen extends Screen {
    protected double pageScale = 1.0;
    protected int designW = 1;
    protected int designH = 1;
    /** Seconds since the previous frame, clamped so a hitch never jumps an animation. */
    protected float dt;
    private long lastFrameNs;
    private Integer savedBlur;
    /** False for screens drawn inside another screen; the host owns the blur setting. */
    protected boolean manageBlur = true;

    protected DesignScreen(Component title) {
        super(title);
    }

    /** Recomputes the page scale from the window and the Appearance settings, and the frame clock. */
    protected void beginFrame() {
        long now = System.nanoTime();
        dt = lastFrameNs == 0 ? 0f : Math.min(0.1f, (now - lastFrameNs) / 1_000_000_000f);
        lastFrameNs = now;
        int guiScale = minecraft.getWindow().getGuiScale();
        double size = ShardClient.modules() == null ? 1.0 : ShardClient.appearance().interfaceScale();
        pageScale = Scale.pageScale(guiScale, size);
        designW = Scale.designSize(width, pageScale);
        designH = Scale.designSize(height, pageScale);
        Render2D.setPixelsPerUnit(Scale.pixelsPerUnit(pageScale, guiScale));
        if (ShardClient.modules() != null) ShardClient.appearance().apply();
        applyBlur();
    }

    public double pageScale() {
        return pageScale;
    }

    public int designWidth() {
        return designW;
    }

    public int designHeight() {
        return designH;
    }

    protected void pushDesign(GuiGraphics g) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.scale((float) pageScale, (float) pageScale);
    }

    protected void popDesign(GuiGraphics g) {
        g.pose().popMatrix();
    }

    protected double toDesign(double guiCoordinate) {
        return Scale.toDesign(guiCoordinate, pageScale);
    }

    // ---- blur ---------------------------------------------------------------------------------

    private void applyBlur() {
        if (ShardClient.modules() == null || !manageBlur) return;
        OptionInstance<Integer> option = minecraft.options.menuBackgroundBlurriness();
        int wanted = ShardClient.appearance().blur.get();
        if (savedBlur == null) savedBlur = option.get();
        if (option.get() != wanted) option.set(wanted);
    }

    private void restoreBlur() {
        if (savedBlur == null) return;
        OptionInstance<Integer> option = minecraft.options.menuBackgroundBlurriness();
        if (option.get() != savedBlur) option.set(savedBlur);
        savedBlur = null;
    }

    @Override
    public void removed() {
        restoreBlur();
        super.removed();
    }

    // ---- mouse in design units ----------------------------------------------------------------

    protected boolean designClicked(double x, double y, int button, boolean doubleClick) {
        return false;
    }

    protected boolean designDragged(double x, double y, int button, double dx, double dy) {
        return false;
    }

    protected boolean designReleased(double x, double y, int button) {
        return false;
    }

    protected boolean designScrolled(double x, double y, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (designClicked(toDesign(event.x()), toDesign(event.y()), event.button(), doubleClick)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (designDragged(toDesign(event.x()), toDesign(event.y()), event.button(), toDesign(dx), toDesign(dy))) return true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (designReleased(toDesign(event.x()), toDesign(event.y()), event.button())) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (designScrolled(toDesign(mx), toDesign(my), sx, sy)) return true;
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
