package pwn.noobs.trouserstreak;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import pwn.noobs.trouserstreak.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Trouser extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger(Trouser.class);
	public static final Category Main = new Category("TrouserStreak", stack());

	@Override
	public void onInitialize() {
		LOG.info("Removing dingleberries");

        Modules.get().add(new ShulkerDupe());
        Modules.get().add(new BetterScaffold());
        Modules.get().add(new AutoBuild());
        Modules.get().add(new AutoBuildDown());
        Modules.get().add(new AutoStaircase());
		Modules.get().add(new Phase());
        Modules.get().add(new FireballClicker());
        Modules.get().add(new FireballRain());
        Modules.get().add(new Boom());
        Modules.get().add(new Voider());
        Modules.get().add(new HandOfGod());
        Modules.get().add(new ExplosionAura());
        Modules.get().add(new DupeModule());
        Modules.get().add(new RedstoneNuker());
        Modules.get().add(new AutoDrop());
        Modules.get().add(new AutoStaircaseDown());
        Modules.get().add(new AutoStaircaseFly());
        Modules.get().add(new InstantKill());
	}

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(Main);
	}

    public String getPackage() {
        return "pwn.noobs.trouserstreak";
    }

    private static ItemStack stack() {
        ItemStack a = new ItemStack(Items.GLOW_LICHEN);
        a.addEnchantment(Enchantment.byRawId(1), 1);
        return a;
    }
}
