package gg.shard.client.module;

public enum ModuleCategory {
    HUD("HUD", "Information overlays"),
    VISUALS("Visuals", "How the fight looks"),
    COMBAT("Combat", "Crystal and anchor tools. The server stays in charge."),
    INPUT("Input", "Keys and mouse"),
    PERFORMANCE("Performance", "Measured optimizers"),
    UTILITY("Utility", "Window, scales and quality of life");

    private final String displayName;
    private final String description;

    ModuleCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
