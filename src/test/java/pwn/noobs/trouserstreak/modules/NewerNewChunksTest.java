package pwn.noobs.trouserstreak.modules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pwn.noobs.trouserstreak.modules.follow.RouteMath;

class NewerNewChunksTest {

    @Test
    void forwardCheck_allowsForwardAndLateral_disallowsBackwards() {
        // Forward along +X
        assertTrue(RouteMath.isForwardOrLateralInt(0, 0, 5, 0, 1, 0));
        // Lateral (dot == 0)
        assertTrue(RouteMath.isForwardOrLateralInt(0, 0, 0, 5, 1, 0));
        assertTrue(RouteMath.isForwardOrLateralInt(0, 0, 0, -5, 1, 0));
        // Backwards along -X
        assertFalse(RouteMath.isForwardOrLateralInt(0, 0, -1, 0, 1, 0));
    }

    @Test
    void forwardCheck_worksForDiagonalForward() {
        // Forward vector (+1,+1); point in NE quadrant accepted
        assertTrue(RouteMath.isForwardOrLateralInt(0, 0, 3, 4, 1, 1));
        // Opposite quadrant (SW) rejected
        assertFalse(RouteMath.isForwardOrLateralInt(0, 0, -3, -4, 1, 1));
        // Pure lateral relative to (1,1) has dot == 0
        assertTrue(RouteMath.isForwardOrLateralInt(0, 0, 5, -5, 1, 1));
    }
}
