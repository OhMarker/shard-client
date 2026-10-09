package gg.shard.client.compat;

// Before 1.21.11 the RenderType factories were static methods on RenderType itself. This stand-in
// keeps call sites written as RenderTypes.x(...) (the stonecutter replacement points imports of
// net.minecraft.client.renderer.rendertype.RenderTypes here on older versions).
//? if <1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class RenderTypes {
    private RenderTypes() {}

    public static RenderType entityTranslucent(Identifier texture) { return RenderType.entityTranslucent(texture); }
    public static RenderType entitySolid(Identifier texture) { return RenderType.entitySolid(texture); }
    public static RenderType entityCutoutNoCull(Identifier texture) { return RenderType.entityCutoutNoCull(texture); }
    public static RenderType armorEntityGlint() { return RenderType.armorEntityGlint(); }
    public static RenderType lines() { return RenderType.lines(); }
}
*///?}
