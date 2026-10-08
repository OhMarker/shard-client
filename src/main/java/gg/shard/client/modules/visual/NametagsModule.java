package gg.shard.client.modules.visual;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

/**
 * Adds the health, armour and session pop count the vanilla client already knows to the name
 * tag above players. Nothing hidden is revealed: health comes from the same synced entity data
 * vanilla uses for the hearts in the tab list, armour from the equipment everyone can see.
 */
public final class NametagsModule extends Module {
    public enum HealthStyle { NUMBER, HEARTS, BOTH }

    private final BoolSetting health = add(new BoolSetting("Show health", "Append the player's health", true).group("Health"));
    private final EnumSetting<HealthStyle> style = add(new EnumSetting<>("Health style", "Number of hit points, hearts, or both", HealthStyle.HEARTS).group("Health"));
    private final BoolSetting colorByHealth = add(new BoolSetting("Colour by health", "Green when healthy, red when low", true).group("Health"));
    private final BoolSetting armor = add(new BoolSetting("Show armour", "Append armour points from the pieces they wear", false).group("Extras"));
    private final BoolSetting pops = add(new BoolSetting("Show pops", "Append totems popped this session", true).group("Extras"));
    private final DoubleSetting scale = add(new DoubleSetting("Scale", "Size of every name tag (vanilla is 1.0x)", 1.0, 0.5, 2.0, 0.05, "x").group("Look")
            .details("Applies to all name tags while the module is on, since they share one renderer."));
    private final IntSetting background = add(new IntSetting("Background opacity", "Darkness of the box behind name tags (vanilla is 25%)", 25, 0, 100, 5, "%").group("Look"));
    private final BoolSetting hideOwn = add(new BoolSetting("Hide own", "Never draw a name tag above yourself, even when a server or mod forces one", true).group("Look"));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", "Leave mob name tags alone", true).group("Extras"));

    public NametagsModule() {
        super("Nametags", "Health, armour and pop count on player name tags.", ModuleCategory.VISUALS);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "item:name_tag";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("totemcounter");
    }

    @Override
    public String about() {
        return "Appends health, armour and session pops to player name tags, and lets you size them and set their background. "
                + "Health and armour come from data the vanilla client already receives; tags that vanilla hides stay hidden.";
    }

    public boolean hidesOwn() {
        return isEnabled() && hideOwn.get();
    }

    public float scale(float original) {
        return isEnabled() ? original * scale.getFloat() : original;
    }

    public float backgroundOpacity(float original) {
        return isEnabled() ? background.get() / 100f : original;
    }

    /** Returns the decorated name tag, or {@code original} when nothing applies. */
    public Component decorate(Entity entity, Component original) {
        if (!isEnabled() || original == null || !(entity instanceof LivingEntity living)) return original;
        if (entity == Minecraft.getInstance().player) return original;
        if (playersOnly.get() && !(entity instanceof Player)) return original;
        MutableComponent out = original.copy();
        boolean any = false;
        if (health.get()) {
            float hp = living.getHealth() + living.getAbsorptionAmount();
            float max = Math.max(1f, living.getMaxHealth());
            int color = colorByHealth.get() ? Colors.health(Math.min(1.0, hp / max)) & 0xFFFFFF : 0xFFFFFF;
            String text = switch (style.get()) {
                case NUMBER -> String.format(Locale.ROOT, "%.0f", hp);
                case HEARTS -> String.format(Locale.ROOT, "%.1f❤", hp / 2f);
                case BOTH -> String.format(Locale.ROOT, "%.0f (%.1f❤)", hp, hp / 2f);
            };
            out.append(Component.literal(" " + text).withColor(color));
            any = true;
        }
        if (armor.get()) {
            out.append(Component.literal(" " + living.getArmorValue() + "🛡").withColor(0xA7B4C8));
            any = true;
        }
        if (pops.get() && entity instanceof Player player) {
            int n = ShardClient.modules().get(TotemPopModule.class).popsFor(player.getUUID());
            if (n > 0) {
                out.append(Component.literal(" " + n + " pops").withColor(Theme.warning() & 0xFFFFFF));
                any = true;
            }
        }
        return any ? out : original;
    }
}
