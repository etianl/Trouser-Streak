package pwn.noobs.trouserstreak.modules.follow;

public final class RouteMath {
    private RouteMath() {}

    public static boolean isForwardOrLateralInt(int sx, int sz, int cx, int cz, int fx, int fz) {
        int proj = (cx - sx) * fx + (cz - sz) * fz;
        return proj >= 0;
    }
}

