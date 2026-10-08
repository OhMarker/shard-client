package gg.shard.client.gui;

/**
 * Design-space maths for the settings page, the popovers and the HUD. Pure Java so the
 * numbers that decide how the page looks at every GUI scale are unit-tested.
 *
 * <p>The page is laid out in <em>design units</em>. One design unit is always
 * {@link #DESIGN_PX_PER_UNIT} physical pixels (times the user's interface size), whatever the
 * game's GUI scale is: at GUI scale 1 a unit is two GUI pixels, at GUI scale 4 it is half a GUI
 * pixel. The screen pushes {@code pageScale} on the pose before drawing and divides mouse
 * coordinates by it, so nothing else needs to know the GUI scale.
 */
public final class Scale {
    private Scale() {}

    /** Physical pixels per design unit at 100% interface size: the page looks like GUI scale 2. */
    public static final double DESIGN_PX_PER_UNIT = 2.0;
    /** Below this many design units of width the page uses the narrow (tab strip) layout. */
    public static final int NARROW_UNITS = 460;
    public static final double MIN_INTERFACE = 0.5;
    public static final double MAX_INTERFACE = 2.0;

    /** Pose scale that maps design units to GUI units for the given GUI scale and interface size (1.0 = 100%). */
    public static double pageScale(int guiScale, double interfaceSize) {
        int scale = Math.max(1, guiScale);
        double size = Math.max(MIN_INTERFACE, Math.min(MAX_INTERFACE, interfaceSize));
        return DESIGN_PX_PER_UNIT / scale * size;
    }

    /** Physical pixels per design unit for the given page scale and GUI scale. */
    public static double pixelsPerUnit(double pageScale, int guiScale) {
        return pageScale * Math.max(1, guiScale);
    }

    /** Size of the window in design units. */
    public static int designSize(int guiSize, double pageScale) {
        return Math.max(1, (int) Math.floor(guiSize / pageScale + 1e-6));
    }

    /** Mouse or scroll coordinate from GUI units to design units. */
    public static double toDesign(double guiCoordinate, double pageScale) {
        return guiCoordinate / pageScale;
    }

    public static double toGui(double designCoordinate, double pageScale) {
        return designCoordinate * pageScale;
    }

    /** Design units to whole physical pixels (textures are generated at this resolution). */
    public static int pixels(double units, double pixelsPerUnit) {
        return Math.max(1, (int) Math.round(units * pixelsPerUnit));
    }

    /** Columns of cards that fit, never fewer than one. */
    public static int gridColumns(int contentWidth, int minCardWidth, int gap) {
        return Math.max(1, (contentWidth + gap) / (Math.max(1, minCardWidth) + gap));
    }

    public static int cardWidth(int contentWidth, int columns, int gap) {
        int cols = Math.max(1, columns);
        return Math.max(1, (contentWidth - (cols - 1) * gap) / cols);
    }

    /** Narrow layout: tab strip instead of a sidebar, panel replaces the grid. */
    public static boolean narrow(int designWidth) {
        return designWidth < NARROW_UNITS;
    }

    /** Pose scale for HUD elements: like {@link #pageScale} but driven by the HUD size setting (percent). */
    public static double hudScale(int guiScale, int hudSizePercent) {
        return pageScale(guiScale, Math.max(25, Math.min(300, hudSizePercent)) / 100.0);
    }
}
