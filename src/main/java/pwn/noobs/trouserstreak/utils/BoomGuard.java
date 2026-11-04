package pwn.noobs.trouserstreak.utils;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

/**
 * BoomGuard —— Client-side protection against "translatable component expansion" (CustomName / "book-page bombs").
 *
 * Vulnerability: {@code {translate:"%1$s%1$s…", with:[{…}]}} — Each nesting level repeats the inner content $k$ times, resulting in an expansion of $k^{depth}$ upon rendering.
 * Field test 26.1.2: d5 exhibits noticeable stuttering; d7 freezes completely.
 *
 * Two lines of defense:
 * <ol>
 *   <li>Rendering-phase interception: {@link #allow(int)} is invoked by {@code TranslatableContentsMixin} (subject to a limit on the number of nested placeholders + a 50ms expansion time budget);</li>
     *   <li><b>Cleaning of Items</b>：{@link #isHostile(Text, int)} Recursively check the component tree; if a match is found, replace the entire component with a literal.
 *       Fundamentally ensure that the "bomb" is no longer present on your person (without relying on any injection points).</li>
 * </ol>
 */
public final class BoomGuard {
    /** The maximum number of conversion specifiers allowed in a single-level format string. */
    public static final int MAX_SPECIFIERS = 4;
    /** Maximum nesting depth of the component tree. */
    public static final int MAX_DEPTH = 4;
    /** The number of expansions allowed within a single 50ms window. */
    private static final int MAX_BUDGET = 4000;
    private static final long WINDOW_NS = 50_000_000L;

    private static volatile boolean enabled = false;
    private static volatile int blockCount = 0;
    private static final ThreadLocal<long[]> STATE = ThreadLocal.withInitial(() -> new long[]{0L, 0L});

    private BoomGuard() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Logs an interception event and returns whether it is one of the initial occurrences (used for rate-limiting log output). */
    public static int noteBlock() {
        return ++blockCount;
    }

    public static int countSpecifiers(String template) {
        if (template == null || template.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < template.length(); i++) {
            if (template.charAt(i) != '%') {
                continue;
            }
            if (i + 1 < template.length() && template.charAt(i + 1) == '%') {
                i++;
                continue;
            }
            count++;
        }
        return count;
    }

    /** Render-time budget check: true = expansion allowed. */
    public static boolean allow(int specifiers) {
        if (specifiers > MAX_SPECIFIERS) {
            return false;
        }
        long now = System.nanoTime();
        long[] state = STATE.get();
        if (now - state[0] > WINDOW_NS) {
            state[0] = now;
            state[1] = 0L;
        }
        state[1] += Math.max(1, specifiers);
        return state[1] <= MAX_BUDGET;
    }

    /**
     * Recursively check the component tree for the presence of "expandable" translatable components.
     * Trigger condition: Single-layer conversion specifiers > MAX_SPECIFIERS, or nesting depth > MAX_DEPTH.
     */
    public static boolean isHostile(Text component, int depth) {
        if (component == null) {
            return false;
        }
        if (depth > MAX_DEPTH) {
            return true;
        }
        if (component.getContent() instanceof TranslatableTextContent translatable) {
            if (countSpecifiers(translatable.getKey()) > MAX_SPECIFIERS) {
                return true;
            }
            Object[] args = translatable.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof Text child && isHostile(child, depth + 1)) {
                        return true;
                    }
                }
                // The number of parameters itself is also an amplification factor.
                if (args.length > MAX_SPECIFIERS && countSpecifiers(translatable.getKey()) > 1) {
                    return true;
                }
            }
        }
        for (Text sibling : component.getSiblings()) {
            if (isHostile(sibling, depth + 1)) {
                return true;
            }
        }
        return false;
    }
}