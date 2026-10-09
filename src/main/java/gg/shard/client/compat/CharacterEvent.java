package gg.shard.client.compat;

// Stand-in for net.minecraft.client.input.CharacterEvent before 1.21.9 (see MouseButtonEvent).
//? if <1.21.9 {
/*public record CharacterEvent(int codepoint, int modifiers) {
    public String codepointAsString() {
        return Character.toString(codepoint);
    }
}
*///?}
