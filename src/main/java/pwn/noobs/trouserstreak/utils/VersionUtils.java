package pwn.noobs.trouserstreak.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtils {
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private VersionUtils() {}

    public static boolean isVersionLessThan(String version, int major, int minor, int patch) {
        int[] parsed = parseVersion(version);
        if (parsed == null) return false;

        return compare(parsed, new int[] { major, minor, patch }) < 0;
    }

    public static boolean isVersionLessThanOrEqual(String version, int major, int minor, int patch) {
        int[] parsed = parseVersion(version);
        if (parsed == null) return false;

        return compare(parsed, new int[] { major, minor, patch }) <= 0;
    }

    public static boolean isVersionGreaterThan(String version, int major, int minor, int patch) {
        int[] parsed = parseVersion(version);
        if (parsed == null) return false;

        return compare(parsed, new int[] { major, minor, patch }) > 0;
    }

    public static boolean isVersionGreaterThanOrEqual(String version, int major, int minor, int patch) {
        int[] parsed = parseVersion(version);
        if (parsed == null) return false;

        return compare(parsed, new int[] { major, minor, patch }) >= 0;
    }

    private static int[] parseVersion(String version) {
        if (version == null || version.isBlank()) return null;

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) return null;

        try {
            return new int[] {
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
                    matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int compare(int[] left, int[] right) {
        for (int i = 0; i < 3; i++) {
            int result = Integer.compare(left[i], right[i]);
            if (result != 0) return result;
        }

        return 0;
    }
}