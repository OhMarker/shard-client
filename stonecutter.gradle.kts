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
    // ---- 26.1 (unobfuscated; Mojang's own names) ------------------------------------------------
    // GUI drawing became render-state extraction: GuiGraphics is GuiGraphicsExtractor and the
    // drawing calls Shard uses lost their render/draw prefixes.
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bGuiGraphics\b""", "GuiGraphicsExtractor", """\bGuiGraphicsExtractor\b""", "GuiGraphics")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bg\.drawString\(""", "g.text(", """\bg\.text\(""", "g.drawString(")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bg\.renderItem\(""", "g.item(", """\bg\.item\(""", "g.renderItem(")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bg\.renderOutline\(""", "g.outline(", """\bg\.outline\(""", "g.renderOutline(")
    }
    // Screen overrides: render -> extractRenderState, renderBackground -> extractBackground, and
    // the background helpers Shard's screens call.
    replacements.regex(current.parsed >= "26.1") {
        replace("""public void render(?=\(\w+ \w+, int \w+, int \w+, float)""", "public void extractRenderState",
            """public void extractRenderState(?=\(\w+ \w+, int \w+, int \w+, float)""", "public void render")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\brenderBackground\(""", "extractBackground(", """\bextractBackground\(""", "renderBackground(")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\brenderMenuBackground\(""", "extractMenuBackground(", """\bextractMenuBackground\(""", "renderMenuBackground(")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\brenderBlurredBackground\(""", "extractBlurredBackground(", """\bextractBlurredBackground\(""", "renderBlurredBackground(")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\brenderPanorama\(""", "extractPanorama(", """\bextractPanorama\(""", "renderPanorama(")
    }
    // 26.1 package moves.
    moved("26.1", "GuiMessage", "net.minecraft.client", "net.minecraft.client.multiplayer.chat")
    moved("26.1", "GuiMessageTag", "net.minecraft.client", "net.minecraft.client.multiplayer.chat")
    moved("26.1", "GuiEntityRenderState", "net.minecraft.client.gui.render.state.pip", "net.minecraft.client.renderer.state.gui.pip")
    moved("26.1", "BlockStateModel", "net.minecraft.client.renderer.block.model", "net.minecraft.client.renderer.block.dispatch")
    moved("26.1", "BlockOutlineRenderState", "net.minecraft.client.renderer.state", "net.minecraft.client.renderer.state.level")
    moved("26.1", "CameraRenderState", "net.minecraft.client.renderer.state", "net.minecraft.client.renderer.state.level")
    moved("26.1", "LevelRenderState", "net.minecraft.client.renderer.state", "net.minecraft.client.renderer.state.level")
    moved("26.1", "SkyRenderState", "net.minecraft.client.renderer.state", "net.minecraft.client.renderer.state.level")
    moved("26.1", "BlockAndTintGetter", "net.minecraft.world.level", "net.minecraft.client.renderer.block")
    // Fabric API 26.1: key bindings are key mappings.
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bclient\.keybinding\.v1\b""", "client.keymapping.v1", """\bclient\.keymapping\.v1\b""", "client.keybinding.v1")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bKeyBindingHelper\b""", "KeyMappingHelper", """\bKeyMappingHelper\b""", "KeyBindingHelper")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\.registerKeyBinding\(""", ".registerKeyMapping(", """\.registerKeyMapping\(""", ".registerKeyBinding(")
    }
    // 26.1: the atlas sprite reference record Material is SpriteId in resources.model.sprite
    // (a new Material type there is a block-model material). The lookahead keeps the package part
    // and the class name separate matches.
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bresources([./])model\1(?=Material\b)""", "resources\$1model\$1sprite\$1", """\bresources([./])model\1sprite\1(?=SpriteId\b)""", "resources\$1model\$1")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bMaterial\b""", "SpriteId", """\bSpriteId\b""", "Material")
    }
    moved("26.1", "AtlasManager", "net.minecraft.client.resources.model", "net.minecraft.client.resources.model.sprite")
    // Fabric API 26.1: renderer API under client.renderer, feature renderers are render layers.
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bfabric\.api\.renderer\.v1\b""", "fabric.api.client.renderer.v1", """\bfabric\.api\.client\.renderer\.v1\b""", "fabric.api.renderer.v1")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bLivingEntityFeatureRendererRegistrationCallback\b""", "LivingEntityRenderLayerRegistrationCallback",
            """\bLivingEntityRenderLayerRegistrationCallback\b""", "LivingEntityFeatureRendererRegistrationCallback")
    }
    // 26.1: ChatComponent.addMessage(Component) is addClientSystemMessage, Minecraft.resizeDisplay()
    // is resizeGui() (the render targets follow the window on their own), and the cull-less
    // entity cutout type is entityCutout (the culling one is entityCutoutCull).
    replacements.regex(current.parsed >= "26.1") {
        replace("""\.getChat\(\)\.addMessage\(""", ".getChat().addClientSystemMessage(", """\.getChat\(\)\.addClientSystemMessage\(""", ".getChat().addMessage(")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bresizeDisplay\(\)""", "resizeGui()", """\bresizeGui\(\)""", "resizeDisplay()")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\bRenderTypes\.entityCutoutNoCull\(""", "RenderTypes.entityCutout(", """\bRenderTypes\.entityCutout\(""", "RenderTypes.entityCutoutNoCull(")
    }
    // 26.1 mixin targets that follow the render -> extract renaming (Gui, Screen, GuiGraphics).
    replacements.regex(current.parsed >= "26.1") {
        replace("""\brender(CameraOverlays|Vignette|PortalOverlay|TextureOverlay|DeferredSubtitles|Crosshair)\b""", "extract\$1",
            """\bextract(CameraOverlays|Vignette|PortalOverlay|TextureOverlay|DeferredSubtitles|Crosshair)\b""", "render\$1")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""\brenderWithTooltipAndSubtitles\b""", "extractRenderStateWithTooltipAndSubtitles", """\bextractRenderStateWithTooltipAndSubtitles\b""", "renderWithTooltipAndSubtitles")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""method = "renderBackground"""", "method = \"extractBackground\"", """method = "extractBackground"""", "method = \"renderBackground\"")
    }
    replacements.regex(current.parsed >= "26.1") {
        replace("""method = "submitEntityRenderState"""", "method = \"entity\"", """method = "entity"""", "method = \"submitEntityRenderState\"")
    }
    // ---- 26.2 --------------------------------------------------------------------------------
    // The in-game HUD class Gui is Hud (same package), and a new Gui (Minecraft.gui) owns the
    // screen, overlay and toasts; the HUD is Minecraft.gui.hud. In src/ "Gui" always means the HUD;
    // a quoted "net.minecraft.client.gui.Gui" (mixin targets = ...) is the new class and is kept.
    replacements.regex(current.parsed >= "26.2") {
        replace("""\bGui\b(?!")""", "Hud", """\bHud\b(?!")""", "Gui")
    }
    replacements.regex(current.parsed >= "26.2") {
        // Lookaheads: the 26.1 getChat().addMessage rule matches the same text.
        replace("""\.gui(?=\.getChat\()""", ".gui.hud", """\.gui\.hud(?=\.getChat\()""", ".gui")
    }
    replacements.regex(current.parsed >= "26.2") {
        replace("""\b(mc|client|minecraft|Minecraft\.getInstance\(\))\.setScreen\(""", "\$1.gui.setScreen(",
            """\b(mc|client|minecraft|Minecraft\.getInstance\(\))\.gui\.setScreen\(""", "\$1.setScreen(")
    }
    replacements.regex(current.parsed >= "26.2") {
        replace("""\b(mc|client|minecraft|Minecraft\.getInstance\(\))\.screen\b""", "\$1.gui.screen()",
            """\b(mc|client|minecraft|Minecraft\.getInstance\(\))\.gui\.screen\(\)""", "\$1.screen")
    }
    replacements.regex(current.parsed >= "26.2") {
        replace("""\.getOverlay\(\)""", ".gui.overlay()", """\.gui\.overlay\(\)""", ".getOverlay()")
    }
    replacements.regex(current.parsed >= "26.2") {
        replace("""\.getToastManager\(\)""", ".gui.toastManager()", """\.gui\.toastManager\(\)""", ".getToastManager()")
    }
    replacements.regex(current.parsed >= "26.2") {
        replace("""\.getMainRenderTarget\(\)""", ".gameRenderer.mainRenderTarget()", """\.gameRenderer\.mainRenderTarget\(\)""", ".getMainRenderTarget()")
    }
    // Section rebuilds moved from LevelRenderer to the new LevelExtractor.
    replacements.regex(current.parsed >= "26.2") {
        replace("""\.levelRenderer\.allChanged\(\)""", ".levelExtractor.allChanged()", """\.levelExtractor\.allChanged\(\)""", ".levelRenderer.allChanged()")
    }
    // F1 (Options.hideGui) is Hud's own flag (isHidden/toggle). Writes first; the read rule skips them.
    replacements.regex(current.parsed >= "26.2") {
        replace("""mc\.options\.hideGui = (true|false);""", "if (mc.gui.hud.isHidden() != \$1) mc.gui.hud.toggle();",
            """if \(mc\.gui\.hud\.isHidden\(\) != (true|false)\) mc\.gui\.hud\.toggle\(\);""", "mc.options.hideGui = \$1;")
    }
    replacements.regex(current.parsed >= "26.2") {
        replace("""mc\.options\.hideGui(?! =)""", "mc.gui.hud.isHidden()", """mc\.gui\.hud\.isHidden\(\)(?! !=)""", "mc.options.hideGui")
    }
    // Mixin target renames (ItemInHandRenderer itself is gone in 26.3).
    replacements.regex(current.parsed >= "26.2") {
        replace("""method = "renderArmWithItem"""", "method = \"submitArmWithItem\"", """method = "submitArmWithItem"""", "method = \"renderArmWithItem\"")
    }
    // Entity type constants moved to EntityTypes.
    replacements.regex(current.parsed >= "26.2") {
        replace("""\bEntityType\.(?=[A-Z_]+\b)""", "net.minecraft.world.entity.EntityTypes.", """\bnet\.minecraft\.world\.entity\.EntityTypes\.""", "EntityType.")
    }
    // TextureFormat is GpuFormat (top-level blaze3d package on 26.2, renderpearl.api on 26.3; RGBA8
    // is RGBA8_UNORM). One rule per name: replacements never chain.
    val gpuFormatPkg = if (current.parsed >= "26.3") "renderpearl\$1api" else "blaze3d"
    replacements.regex(current.parsed >= "26.2") {
        replace("""\bmojang([./])blaze3d\1textures\1TextureFormat\b""", "mojang\$1$gpuFormatPkg\$1GpuFormat",
            """\bmojang([./])(blaze3d|renderpearl\1api)\1GpuFormat\b""", "mojang\$1blaze3d\$1textures\$1TextureFormat")
    }
    replacements.regex(current.parsed >= "26.2") {
        replace("""\bTextureFormat\.RGBA8\b""", "GpuFormat.RGBA8_UNORM", """\bGpuFormat\.RGBA8_UNORM\b""", "TextureFormat.RGBA8")
    }
    // ---- 26.3 --------------------------------------------------------------------------------
    // The GPU abstraction moved from com.mojang.blaze3d to com.mojang.renderpearl.api.
    moved("26.3", "RenderPipeline", "com.mojang.blaze3d.pipeline", "com.mojang.renderpearl.api.pipeline")
    moved("26.3", "CommandEncoder", "com.mojang.blaze3d.systems", "com.mojang.renderpearl.api.commands")
    moved("26.3", "GpuDevice", "com.mojang.blaze3d.systems", "com.mojang.renderpearl.api.device")
    moved("26.3", "AddressMode", "com.mojang.blaze3d.textures", "com.mojang.renderpearl.api.textures")
    moved("26.3", "FilterMode", "com.mojang.blaze3d.textures", "com.mojang.renderpearl.api.textures")
    moved("26.3", "GpuSampler", "com.mojang.blaze3d.textures", "com.mojang.renderpearl.api.textures")
    moved("26.3", "GpuTexture", "com.mojang.blaze3d.textures", "com.mojang.renderpearl.api.textures")
    moved("26.3", "VertexFormat", "com.mojang.blaze3d.vertex", "com.mojang.renderpearl.api.vertex")
    // authlib 10: profile results moved to authlib.services (YggdrasilAuthenticationService is gone;
    // AccountManager uses MinecraftServicesDiscoveryService in place).
    moved("26.3", "ProfileResult", "com.mojang.authlib.yggdrasil", "com.mojang.authlib.services")
}
