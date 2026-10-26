package org.mtr.mapping.tool;

import org.lwjgl.input.Keyboard;
import java.util.HashMap;
import java.util.Map;

public final class KeyCodes {
    private static final Map<Integer, Integer> LEGACY = new HashMap<>(), GLFW = new HashMap<>();
    static {
        int[] letters = {Keyboard.KEY_A, Keyboard.KEY_B, Keyboard.KEY_C, Keyboard.KEY_D, Keyboard.KEY_E, Keyboard.KEY_F,
            Keyboard.KEY_G, Keyboard.KEY_H, Keyboard.KEY_I, Keyboard.KEY_J, Keyboard.KEY_K, Keyboard.KEY_L, Keyboard.KEY_M,
            Keyboard.KEY_N, Keyboard.KEY_O, Keyboard.KEY_P, Keyboard.KEY_Q, Keyboard.KEY_R, Keyboard.KEY_S, Keyboard.KEY_T,
            Keyboard.KEY_U, Keyboard.KEY_V, Keyboard.KEY_W, Keyboard.KEY_X, Keyboard.KEY_Y, Keyboard.KEY_Z};
        for (int i = 0; i < letters.length; i++) bind(65 + i, letters[i]);
        int[] digits = {Keyboard.KEY_0, Keyboard.KEY_1, Keyboard.KEY_2, Keyboard.KEY_3, Keyboard.KEY_4,
            Keyboard.KEY_5, Keyboard.KEY_6, Keyboard.KEY_7, Keyboard.KEY_8, Keyboard.KEY_9};
        int[] keypad = {Keyboard.KEY_NUMPAD0, Keyboard.KEY_NUMPAD1, Keyboard.KEY_NUMPAD2, Keyboard.KEY_NUMPAD3,
            Keyboard.KEY_NUMPAD4, Keyboard.KEY_NUMPAD5, Keyboard.KEY_NUMPAD6, Keyboard.KEY_NUMPAD7, Keyboard.KEY_NUMPAD8, Keyboard.KEY_NUMPAD9};
        for (int i = 0; i < 10; i++) { bind(48 + i, digits[i]); bind(320 + i, keypad[i]); }
        int[][] named = {{-1, Keyboard.KEY_NONE}, {32, Keyboard.KEY_SPACE}, {39, Keyboard.KEY_APOSTROPHE},
            {44, Keyboard.KEY_COMMA}, {45, Keyboard.KEY_MINUS}, {46, Keyboard.KEY_PERIOD}, {47, Keyboard.KEY_SLASH},
            {59, Keyboard.KEY_SEMICOLON}, {61, Keyboard.KEY_EQUALS}, {91, Keyboard.KEY_LBRACKET}, {92, Keyboard.KEY_BACKSLASH},
            {93, Keyboard.KEY_RBRACKET}, {96, Keyboard.KEY_GRAVE}, {256, Keyboard.KEY_ESCAPE}, {257, Keyboard.KEY_RETURN},
            {258, Keyboard.KEY_TAB}, {259, Keyboard.KEY_BACK}, {260, Keyboard.KEY_INSERT}, {261, Keyboard.KEY_DELETE},
            {262, Keyboard.KEY_RIGHT}, {263, Keyboard.KEY_LEFT}, {264, Keyboard.KEY_DOWN}, {265, Keyboard.KEY_UP},
            {266, Keyboard.KEY_PRIOR}, {267, Keyboard.KEY_NEXT}, {268, Keyboard.KEY_HOME}, {269, Keyboard.KEY_END},
            {280, Keyboard.KEY_CAPITAL}, {281, Keyboard.KEY_SCROLL}, {282, Keyboard.KEY_NUMLOCK}, {283, Keyboard.KEY_SYSRQ},
            {284, Keyboard.KEY_PAUSE}, {330, Keyboard.KEY_DECIMAL}, {331, Keyboard.KEY_DIVIDE}, {332, Keyboard.KEY_MULTIPLY},
            {333, Keyboard.KEY_SUBTRACT}, {334, Keyboard.KEY_ADD}, {335, Keyboard.KEY_NUMPADENTER},
            {336, Keyboard.KEY_NUMPADEQUALS}, {340, Keyboard.KEY_LSHIFT}, {341, Keyboard.KEY_LCONTROL},
            {342, Keyboard.KEY_LMENU}, {343, Keyboard.KEY_LMETA}, {344, Keyboard.KEY_RSHIFT},
            {345, Keyboard.KEY_RCONTROL}, {346, Keyboard.KEY_RMENU}, {347, Keyboard.KEY_RMETA}, {348, Keyboard.KEY_APPS}};
        for (int[] pair : named) bind(pair[0], pair[1]);
        int[] functions = {Keyboard.KEY_F1, Keyboard.KEY_F2, Keyboard.KEY_F3, Keyboard.KEY_F4, Keyboard.KEY_F5,
            Keyboard.KEY_F6, Keyboard.KEY_F7, Keyboard.KEY_F8, Keyboard.KEY_F9, Keyboard.KEY_F10, Keyboard.KEY_F11,
            Keyboard.KEY_F12, Keyboard.KEY_F13, Keyboard.KEY_F14, Keyboard.KEY_F15};
        for (int i = 0; i < functions.length; i++) bind(290 + i, functions[i]);
    }
    private KeyCodes() {}
    private static void bind(int glfw, int legacy) { LEGACY.put(glfw, legacy); GLFW.put(legacy, glfw); }
    public static int toLegacy(int key) {
        Integer legacy = LEGACY.get(key);
        if (legacy == null) throw new IllegalArgumentException("Unsupported GLFW key " + key);
        return legacy;
    }
    public static int toGlfw(int key) { return GLFW.getOrDefault(key, -1); }
}
