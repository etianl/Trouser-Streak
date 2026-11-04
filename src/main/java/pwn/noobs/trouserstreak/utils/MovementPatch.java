package pwn.noobs.trouserstreak.utils;

public class MovementPatch {
    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}