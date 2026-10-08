package gg.shard.client.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure maths for HUD layout, unit-tested: anchoring an element to the nearest corner, edge or
 * centre so it keeps its distance from that point when the window or GUI scale changes;
 * snapping a dragged element to the screen and to other elements (with the guide lines to
 * draw); and aligning or distributing a selection. Positions and sizes are GUI units;
 * offsets are HUD units ({@code unit} GUI units each), so a distance on screen survives a GUI
 * scale change.
 */
public final class HudGeometry {
    private HudGeometry() {}

    public static final int START = 0;
    public static final int CENTER = 1;
    public static final int END = 2;

    public record Rect(double x, double y, double w, double h) {
        double right() {
            return x + w;
        }

        double bottom() {
            return y + h;
        }

        double cx() {
            return x + w / 2;
        }

        double cy() {
            return y + h / 2;
        }
    }

    /** Snapped position plus the guide lines that caused it (GUI units; empty when nothing snapped). */
    public record Snap(double x, double y, List<Double> verticalGuides, List<Double> horizontalGuides) {}

    /** Start, centre or end, by which third of the screen the element's centre is in. */
    public static int anchorFor(double pos, double size, double screen) {
        double c = pos + size / 2;
        if (c < screen / 3) return START;
        if (c > screen * 2 / 3) return END;
        return CENTER;
    }

    /** Offset in HUD units from the anchor point to the element's matching edge (or centre). */
    public static double offsetFor(int anchor, double pos, double size, double screen, double unit) {
        double u = unit <= 0 ? 1 : unit;
        return switch (anchor) {
            case START -> pos / u;
            case END -> (screen - pos - size) / u;
            default -> (pos + size / 2 - screen / 2) / u;
        };
    }

    /** Inverse of {@link #offsetFor}: the element's position in GUI units for this screen. */
    public static double positionFor(int anchor, double offset, double size, double screen, double unit) {
        double u = unit <= 0 ? 1 : unit;
        return switch (anchor) {
            case START -> offset * u;
            case END -> screen - size - offset * u;
            default -> screen / 2 + offset * u - size / 2;
        };
    }

    public static double clamp(double pos, double size, double screen) {
        return Math.max(0, Math.min(Math.max(0, screen - size), pos));
    }

    /**
     * Snaps {@code moving} to the screen edges and centre lines and to the edges and centres of
     * {@code others}, separately per axis, when within {@code threshold} GUI units.
     */
    public static Snap snap(Rect moving, double screenW, double screenH, List<Rect> others, double threshold) {
        List<double[]> xTargets = new ArrayList<>();
        List<double[]> yTargets = new ArrayList<>();
        // {target line, which edge of the moving rect: 0 start, 1 centre, 2 end}
        xTargets.add(new double[]{0, 0});
        xTargets.add(new double[]{screenW, 2});
        xTargets.add(new double[]{screenW / 2, 1});
        yTargets.add(new double[]{0, 0});
        yTargets.add(new double[]{screenH, 2});
        yTargets.add(new double[]{screenH / 2, 1});
        for (Rect o : others) {
            for (double line : new double[]{o.x, o.right(), o.cx()}) {
                for (int edge = 0; edge < 3; edge++) xTargets.add(new double[]{line, edge});
            }
            for (double line : new double[]{o.y, o.bottom(), o.cy()}) {
                for (int edge = 0; edge < 3; edge++) yTargets.add(new double[]{line, edge});
            }
        }
        double[] bx = best(moving.x, moving.w, xTargets, threshold);
        double[] by = best(moving.y, moving.h, yTargets, threshold);
        double nx = bx == null ? moving.x : bx[0];
        double ny = by == null ? moving.y : by[0];
        List<Double> vg = new ArrayList<>();
        List<Double> hg = new ArrayList<>();
        if (bx != null) vg.add(bx[1]);
        if (by != null) hg.add(by[1]);
        return new Snap(nx, ny, vg, hg);
    }

    /** {new position, guide line} of the closest target within the threshold, or null. */
    private static double[] best(double pos, double size, List<double[]> targets, double threshold) {
        double bestDist = threshold + 1e-9;
        double[] out = null;
        for (double[] t : targets) {
            double edgePos = t[1] == 0 ? pos : t[1] == 1 ? pos + size / 2 : pos + size;
            double d = Math.abs(edgePos - t[0]);
            if (d < bestDist) {
                bestDist = d;
                double moved = pos + (t[0] - edgePos);
                out = new double[]{moved, t[0]};
            }
        }
        return out;
    }

    public enum Align { LEFT, CENTER_X, RIGHT, TOP, CENTER_Y, BOTTOM }

    /** New top-left positions aligning every rect to the selection's bounding box. */
    public static List<double[]> align(List<Rect> rects, Align mode) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Rect r : rects) {
            minX = Math.min(minX, r.x);
            minY = Math.min(minY, r.y);
            maxX = Math.max(maxX, r.right());
            maxY = Math.max(maxY, r.bottom());
        }
        List<double[]> out = new ArrayList<>();
        for (Rect r : rects) {
            double x = r.x, y = r.y;
            switch (mode) {
                case LEFT -> x = minX;
                case RIGHT -> x = maxX - r.w;
                case CENTER_X -> x = (minX + maxX) / 2 - r.w / 2;
                case TOP -> y = minY;
                case BOTTOM -> y = maxY - r.h;
                case CENTER_Y -> y = (minY + maxY) / 2 - r.h / 2;
            }
            out.add(new double[]{x, y});
        }
        return out;
    }

    /** Equal gaps between rects along one axis, keeping the first and last in place. */
    public static List<double[]> distribute(List<Rect> rects, boolean horizontal) {
        List<double[]> out = new ArrayList<>();
        for (Rect r : rects) out.add(new double[]{r.x, r.y});
        if (rects.size() < 3) return out;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rects.size(); i++) order.add(i);
        order.sort(Comparator.comparingDouble(i -> horizontal ? rects.get(i).x : rects.get(i).y));
        Rect first = rects.get(order.get(0));
        Rect last = rects.get(order.get(order.size() - 1));
        double span = horizontal ? last.right() - first.x : last.bottom() - first.y;
        double used = 0;
        for (Rect r : rects) used += horizontal ? r.w : r.h;
        double gap = (span - used) / (rects.size() - 1);
        double cursor = horizontal ? first.x : first.y;
        for (int i : order) {
            Rect r = rects.get(i);
            if (horizontal) out.get(i)[0] = cursor;
            else out.get(i)[1] = cursor;
            cursor += (horizontal ? r.w : r.h) + gap;
        }
        return out;
    }
}
