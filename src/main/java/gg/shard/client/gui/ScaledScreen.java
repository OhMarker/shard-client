package gg.shard.client.gui;

/** Implemented on every Screen by a mixin: the pose factor its layout was made for (1 = normal). */
public interface ScaledScreen {
    double shard$factor();

    static double factorOf(Object screen) {
        return screen instanceof ScaledScreen s ? s.shard$factor() : 1.0;
    }
}
