package gg.shard.client.compat;

/**
 * Shard saves keybinds as GLFW key codes, so one config (the launcher shares it between
 * instances) means the same keys on every Minecraft version. Up to 26.2 the game's key codes are
 * GLFW's and this is the identity; 26.3 runs on SDL3 and uses SDL scancodes, converted here.
 * No Minecraft classes, so it is unit-testable.
 */
public final class KeyCodes {
    private KeyCodes() {}

    /** No counterpart on the other side. */
    public static final int UNMAPPED = Integer.MIN_VALUE;

    /** The GLFW code of a game key code (-1 for the game's "unknown key"), or {@link #UNMAPPED}. */
    public static int toGlfw(int key) {
        //? if >=26.3 {
        /*if (key == 0) return -1; // SDL_SCANCODE_UNKNOWN is GLFW_KEY_UNKNOWN (unbound)
        return key >= 0 && key < TO_GLFW.length && TO_GLFW[key] != 0 ? TO_GLFW[key] : UNMAPPED;
        *///?} else {
        return key;
        //?}
    }

    /** The game key code of a GLFW code, or {@link #UNMAPPED}. */
    public static int fromGlfw(int glfw) {
        //? if >=26.3 {
        /*return glfw >= 0 && glfw < FROM_GLFW.length && FROM_GLFW[glfw] != 0 ? FROM_GLFW[glfw] : UNMAPPED;
        *///?} else {
        return glfw;
        //?}
    }

    /** A game key code as saved in the config: GLFW (unbound and unknown keys as they are). */
    public static int save(int key) {
        if (key < 0) return key;
        int glfw = toGlfw(key);
        return glfw == UNMAPPED ? key : glfw;
    }

    /** A saved (GLFW) key code as the game's. */
    public static int load(int saved) {
        if (saved < 0) return saved;
        int key = fromGlfw(saved);
        return key == UNMAPPED ? saved : key;
    }

    /** The GLFW key codes Keys names (LWJGL's values; org.lwjgl.glfw is not on 26.3's classpath). */
    public static final class GLFW {
        private GLFW() {}

        public static final int GLFW_KEY_SPACE = 32, GLFW_KEY_APOSTROPHE = 39, GLFW_KEY_COMMA = 44, GLFW_KEY_MINUS = 45,
                GLFW_KEY_PERIOD = 46, GLFW_KEY_SLASH = 47, GLFW_KEY_0 = 48, GLFW_KEY_9 = 57, GLFW_KEY_SEMICOLON = 59,
                GLFW_KEY_EQUAL = 61, GLFW_KEY_A = 65, GLFW_KEY_Z = 90, GLFW_KEY_LEFT_BRACKET = 91, GLFW_KEY_BACKSLASH = 92,
                GLFW_KEY_RIGHT_BRACKET = 93, GLFW_KEY_GRAVE_ACCENT = 96, GLFW_KEY_ESCAPE = 256, GLFW_KEY_ENTER = 257,
                GLFW_KEY_TAB = 258, GLFW_KEY_BACKSPACE = 259, GLFW_KEY_INSERT = 260, GLFW_KEY_DELETE = 261,
                GLFW_KEY_RIGHT = 262, GLFW_KEY_LEFT = 263, GLFW_KEY_DOWN = 264, GLFW_KEY_UP = 265, GLFW_KEY_PAGE_UP = 266,
                GLFW_KEY_PAGE_DOWN = 267, GLFW_KEY_HOME = 268, GLFW_KEY_END = 269, GLFW_KEY_CAPS_LOCK = 280,
                GLFW_KEY_F1 = 290, GLFW_KEY_F25 = 314, GLFW_KEY_KP_0 = 320, GLFW_KEY_KP_9 = 329,
                GLFW_KEY_LEFT_SHIFT = 340, GLFW_KEY_LEFT_CONTROL = 341, GLFW_KEY_LEFT_ALT = 342,
                GLFW_KEY_RIGHT_SHIFT = 344, GLFW_KEY_RIGHT_CONTROL = 345, GLFW_KEY_RIGHT_ALT = 346, GLFW_KEY_LAST = 348;
    }

    //? if >=26.3 {
    /*/^* {GLFW key code, SDL scancode} for every key both have. ^/
    private static final int[][] PAIRS = {
            {32, 44}, {39, 52}, {44, 54}, {45, 45}, {46, 55}, {47, 56}, {48, 39}, {59, 51}, {61, 46},
            {91, 47}, {92, 49}, {93, 48}, {96, 53}, {161, 100},
            {256, 41}, {257, 40}, {258, 43}, {259, 42}, {260, 73}, {261, 76}, {262, 79}, {263, 80}, {264, 81}, {265, 82},
            {266, 75}, {267, 78}, {268, 74}, {269, 77}, {280, 57}, {281, 71}, {282, 83}, {283, 70}, {284, 72},
            {320, 98}, {330, 99}, {331, 84}, {332, 85}, {333, 86}, {334, 87}, {335, 88}, {336, 103},
            {340, 225}, {341, 224}, {342, 226}, {343, 227}, {344, 229}, {345, 228}, {346, 230}, {347, 231}, {348, 101},
    };
    private static final int[] TO_GLFW = new int[512];
    private static final int[] FROM_GLFW = new int[349];

    static {
        for (int[] p : PAIRS) put(p[0], p[1]);
        for (int i = 0; i < 26; i++) put(65 + i, 4 + i);   // A-Z
        for (int i = 1; i <= 9; i++) put(48 + i, 29 + i);  // 1-9 (0 is above)
        for (int i = 0; i < 12; i++) put(290 + i, 58 + i); // F1-F12
        for (int i = 0; i < 12; i++) put(302 + i, 104 + i); // F13-F24
        for (int i = 1; i <= 9; i++) put(320 + i, 88 + i); // keypad 1-9 (0 is above)
    }

    private static void put(int glfw, int sdl) {
        TO_GLFW[sdl] = glfw;
        FROM_GLFW[glfw] = sdl;
    }
    *///?}
}
