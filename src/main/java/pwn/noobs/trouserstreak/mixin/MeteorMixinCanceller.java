package pwn.noobs.trouserstreak.mixin;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import java.util.List;

public class MeteorMixinCanceller implements MixinCanceller {
    private static final String AbstractSignEditScreenMixin = "meteordevelopment.meteorclient.mixin.AbstractSignEditScreenMixin";
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        if (AbstractSignEditScreenMixin.equals(mixinClassName)) {
            System.out.println("Cancelling AbstractSignEditScreenMixin.");
            return true;
        }
        return false;
    }
}