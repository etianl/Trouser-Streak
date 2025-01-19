package pwn.noobs.trouserstreak.modules.addon;

import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class TrouserModule extends Module {

    /**
     * @Author: I-No-oNe <a href="https://github.com/I-No-oNe/">...</a>
     * You can put here variables and access it via any Module that extends this class
     * Also we can use this module to get the name and description of modules that extends this class
     * Example:
     * if (module instanceof TrouserModule m)
     * m.getName();
     */

    public TrouserModule(String name, String description) {
        super(Trouser.Main,name,description);
    }

    public TrouserModule(String name, String description, String originalAuthor) {
        super(Trouser.Main,name,description,originalAuthor);
    }
}