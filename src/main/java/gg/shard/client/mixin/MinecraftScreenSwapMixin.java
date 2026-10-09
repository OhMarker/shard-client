package gg.shard.client.mixin;

import gg.shard.client.gui.menu.MenuScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Swaps vanilla's title screen and server list for Shard's while "Shard title screen" is on
 * (only the exact vanilla classes, so other mods' subclasses are left alone).
 */
@Mixin(Minecraft.class)
abstract class MinecraftScreenSwapMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private Screen shard$swapScreen(Screen screen) {
        return MenuScreens.replace(screen);
    }
}
