plugins {
    id("dev.kikugie.stonecutter")
}

// The version the shared src/ is switched to (the code you see in the editor).
stonecutter active "1.21.11"

// Global replacements for pure renames. src/ is written with the 1.21.11 names; each block maps
// them to the older names on versions before the rename (direction true = 1.21.11 form).
// Package moves are matched with both '.' (imports) and '/' (mixin descriptors) separators.
stonecutter parameters {
    /** Class [cls] lives in [newPkg] from [since] on and in [oldPkg] before. */
    fun moved(since: String, cls: String, oldPkg: String, newPkg: String) {
        // e.g. net([./])minecraft\1util\1Util\b  ->  net$1minecraft$1Util
        fun pattern(pkg: String) = (pkg.split('.') + cls).let { p -> p[0] + "([./])" + p.drop(1).joinToString("\\1") + "\\b" }
        fun value(pkg: String) = (pkg.split('.') + cls).joinToString("\$1")
        replacements.regex(current.parsed >= since) {
            replace(pattern(oldPkg), value(newPkg), pattern(newPkg), value(oldPkg))
        }
    }

    // 1.21.11 renamed ResourceLocation to Identifier (same package, same API).
    replacements.regex(current.parsed >= "1.21.11") {
        replace("""\bResourceLocation\b""", "Identifier", """\bIdentifier\b""", "ResourceLocation")
    }
    // ...and SoundInstance.getLocation() to getIdentifier().
    replacements.regex(current.parsed >= "1.21.11") {
        replace("""\.getLocation\(\)""", ".getIdentifier()", """\.getIdentifier\(\)""", ".getLocation()")
    }
    // ...and GuiGraphics.submitOutline() back to renderOutline().
    replacements.regex(current.parsed >= "1.21.11") {
        replace("""\.submitOutline\(""", ".renderOutline(", """\.renderOutline\(""", ".submitOutline(")
    }
    // Screen.init(Minecraft, int, int) lost its Minecraft parameter in 1.21.11 (mixin descriptors).
    replacements.string(current.parsed >= "1.21.11") {
        replace("\"init(Lnet/minecraft/client/Minecraft;II)V\"", "\"init(II)V\"")
    }
    // 1.21.11 package reorganisation (Mojang mappings).
    moved("1.21.11", "Util", "net.minecraft", "net.minecraft.util")
    moved("1.21.11", "RenderType", "net.minecraft.client.renderer", "net.minecraft.client.renderer.rendertype")
    moved("1.21.11", "EndCrystalModel", "net.minecraft.client.model", "net.minecraft.client.model.object.crystal")
    moved("1.21.11", "ShieldModel", "net.minecraft.client.model", "net.minecraft.client.model.object.equipment")
    moved("1.21.11", "PlayerModel", "net.minecraft.client.model", "net.minecraft.client.model.player")
    moved("1.21.11", "EnderDragonPart", "net.minecraft.world.entity.boss", "net.minecraft.world.entity.boss.enderdragon")
    moved("1.21.11", "AbstractArrow", "net.minecraft.world.entity.projectile", "net.minecraft.world.entity.projectile.arrow")
    moved("1.21.11", "Zombie", "net.minecraft.world.entity.monster", "net.minecraft.world.entity.monster.zombie")
    // 1.21.11 split RenderType's static factories into RenderTypes; older versions use a
    // same-named stand-in that forwards to RenderType (gg.shard.client.compat.RenderTypes).
    moved("1.21.11", "RenderTypes", "gg.shard.client.compat", "net.minecraft.client.renderer.rendertype")
    // JSpecify is only on the classpath from 1.21.11; JetBrains annotations (TYPE_USE too) before.
    moved("1.21.11", "Nullable", "org.jetbrains.annotations", "org.jspecify.annotations")
}
