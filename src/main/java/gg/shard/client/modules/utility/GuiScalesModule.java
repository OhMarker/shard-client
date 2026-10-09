package gg.shard.client.modules.utility;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ScreenScale;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.Labeled;
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.gui.LayeredDraw;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

import java.util.function.IntSupplier;

/**
 * GUI Scales: your inventory and containers at their own GUI scale (bigger for fast refills,
 * smaller to see more), and separate sizes for the hotbar with its bars, the scoreboard, the tab
 * list, titles and the action bar, the boss bar and chat. Containers are laid out at a virtual
 * size and drawn scaled; the mouse is converted with the same factor, so every click lands on the
 * slot under it ({@link ScreenScale}). HUD parts are scaled around their own anchor (the hotbar
 * around the bottom centre, the scoreboard around the right edge) so they stay in place.
 */
public final class GuiScalesModule extends Module {
    public enum InventoryScale implements Labeled {
        GAME("Same as game", 0), S1("1", 1), S2("2", 2), S3("3", 3), S4("4", 4), S5("5", 5), S6("6", 6);

        private final String label;
        final int value;

        InventoryScale(String label, int value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String label() {
            return label;
        }
    }

    private final EnumSetting<InventoryScale> inventory = add(new EnumSetting<>("Inventory scale", "GUI scale for your inventory, chests, shulkers and other containers", InventoryScale.GAME)
            .details("Capped at the largest scale that fits your window. Clicks land exactly on the slot drawn under the pointer."));
    private final IntSetting hotbar = add(new IntSetting("Hotbar scale", "Hotbar, hearts, hunger, armour and XP", 100, 50, 200, 5, "%").group("Parts of the screen"));
    private final IntSetting scoreboard = add(new IntSetting("Scoreboard scale", "The sidebar scoreboard", 100, 50, 200, 5, "%").group("Parts of the screen"));
    private final IntSetting tabList = add(new IntSetting("Tab list scale", "The player list while you hold Tab", 100, 50, 200, 5, "%").group("Parts of the screen"));
    private final IntSetting titles = add(new IntSetting("Title scale", "Titles, subtitles and the action bar", 100, 50, 200, 5, "%").group("Parts of the screen"));
    private final IntSetting bossBar = add(new IntSetting("Boss bar scale", "Boss bars at the top", 100, 50, 200, 5, "%").group("Parts of the screen"));
    private final IntSetting chat = add(new IntSetting("Chat scale", "Chat text size (the same as vanilla's Chat Settings option)", 100, 25, 100, 5, "%").group("Parts of the screen"));

    public GuiScalesModule() {
        super("GUI Scales", "Your inventory at its own GUI scale, and separate sizes for the hotbar, scoreboard, tab list, titles and boss bar.", ModuleCategory.UTILITY);
        chat.onChange(v -> applyChat());
    }

    @Override
    public String icon() {
        return "scale";
    }

    @Override
    public String about() {
        return "Lets you play at one GUI scale while your inventory and containers open at another, so refilling totems and crystals is quick, "
                + "and sizes the hotbar (with hearts, hunger and XP), scoreboard, tab list, titles, boss bar and chat separately. "
                + "Interface size in Settings sizes Shard's own menu. Only how the screen is drawn changes.";
    }

    @Override
    protected void onEnable() {
        applyChat();
    }

    private void applyChat() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) mc.options.chatScale().set(chat.get() / 100.0);
    }

    /** Pose factor a screen should use; 1 for everything but containers. */
    public double factorFor(Screen screen) {
        if (!isEnabled() || !(screen instanceof AbstractContainerScreen<?>) || inventory.get() == InventoryScale.GAME) return 1.0;
        var window = Minecraft.getInstance().getWindow();
        int max = window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode());
        return ScreenScale.factor(inventory.get().value, window.getGuiScale(), max);
    }

    /** Wraps the vanilla HUD parts once at start-up; each reads its scale every frame. */
    public static void registerHudScaling() {
        GuiScalesModule[] self = new GuiScalesModule[1];
        java.util.function.Supplier<GuiScalesModule> mod = () -> {
            if (self[0] == null && ShardClient.isReady()) self[0] = ShardClient.modules().get(GuiScalesModule.class);
            return self[0];
        };
        IntSupplier hot = () -> pct(mod.get(), m -> m.hotbar.get());
        //? if >=1.21.6 {
        for (Identifier id : new Identifier[]{VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR,
                VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.INFO_BAR,
                VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.HELD_ITEM_TOOLTIP}) {
            wrap(id, hot, 0.5f, 1f);
        }
        wrap(VanillaHudElements.SCOREBOARD, () -> pct(mod.get(), m -> m.scoreboard.get()), 1f, 0.5f);
        wrap(VanillaHudElements.PLAYER_LIST, () -> pct(mod.get(), m -> m.tabList.get()), 0.5f, 0f);
        wrap(VanillaHudElements.TITLE_AND_SUBTITLE, () -> pct(mod.get(), m -> m.titles.get()), 0.5f, 0.5f);
        wrap(VanillaHudElements.OVERLAY_MESSAGE, () -> pct(mod.get(), m -> m.titles.get()), 0.5f, 1f);
        wrap(VanillaHudElements.BOSS_BAR, () -> pct(mod.get(), m -> m.bossBar.get()), 0.5f, 0f);
        //?} else {
        /*// Before 1.21.6 Fabric names Gui's coarser layers: the hotbar, its bars, the mount health and
        // the held item name are one layer (the XP level number is its own).
        HudLayerRegistrationCallback.EVENT.register(drawer -> {
            wrap(drawer, IdentifiedLayer.HOTBAR_AND_BARS, hot, 0.5f, 1f);
            wrap(drawer, IdentifiedLayer.EXPERIENCE_LEVEL, hot, 0.5f, 1f);
            wrap(drawer, IdentifiedLayer.SCOREBOARD, () -> pct(mod.get(), m -> m.scoreboard.get()), 1f, 0.5f);
            wrap(drawer, IdentifiedLayer.PLAYER_LIST, () -> pct(mod.get(), m -> m.tabList.get()), 0.5f, 0f);
            wrap(drawer, IdentifiedLayer.TITLE_AND_SUBTITLE, () -> pct(mod.get(), m -> m.titles.get()), 0.5f, 0.5f);
            wrap(drawer, IdentifiedLayer.OVERLAY_MESSAGE, () -> pct(mod.get(), m -> m.titles.get()), 0.5f, 1f);
            wrap(drawer, IdentifiedLayer.BOSS_BAR, () -> pct(mod.get(), m -> m.bossBar.get()), 0.5f, 0f);
        });
        *///?}
    }

    private static int pct(GuiScalesModule m, java.util.function.ToIntFunction<GuiScalesModule> f) {
        return m == null || !m.isEnabled() ? 100 : f.applyAsInt(m);
    }

    /** Replaces a vanilla element with one drawn scaled around the anchor (fractions of the screen). */
    //? if >=1.21.6 {
    private static void wrap(Identifier id, IntSupplier percent, float ax, float ay) {
        HudElementRegistry.replaceElement(id, original -> (HudElement) (g, delta) -> {
    //?} else {
    /*private static void wrap(net.fabricmc.fabric.api.client.rendering.v1.LayeredDrawerWrapper drawer, Identifier id, IntSupplier percent, float ax, float ay) {
        drawer.replaceLayer(id, original -> IdentifiedLayer.of(id, (g, delta) -> {
    *///?}
            int p = percent.getAsInt();
            if (p == 100) {
                draw(original, g, delta);
                return;
            }
            float s = p / 100f;
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate((float) ScreenScale.anchorShift(g.guiWidth() * ax, s), (float) ScreenScale.anchorShift(g.guiHeight() * ay, s));
            pose.scale(s, s);
            draw(original, g, delta);
            pose.popMatrix();
        //? if >=1.21.6 {
        });
        //?} else {
        /*}));
        *///?}
    }

    //? if >=1.21.6 {
    private static void draw(HudElement element, net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.DeltaTracker delta) {
    //?} else {
    /*private static void draw(LayeredDraw.Layer element, net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.DeltaTracker delta) {
    *///?}
        //? if >=26.1 {
        /*element.extractRenderState(g, delta);
        *///?} else {
        element.render(g, delta);
        //?}
    }

}
