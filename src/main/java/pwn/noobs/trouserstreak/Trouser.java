package pwn.noobs.trouserstreak;

import meteordevelopment.meteorclient.systems.commands.Commands;
import pwn.noobs.trouserstreak.commands.CasterTimer;
import pwn.noobs.trouserstreak.commands.LavaTimeCalculator;
import pwn.noobs.trouserstreak.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Trouser extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger(Trouser.class);
	public static final Category Main = new Category("TrouserStreak");

	@Override
	public void onInitialize() {
		LOG.info("Initializing PantsMod!");

        Modules.get().add(new AutoLavaCaster());
        Modules.get().add(new AutoMountain());
        Modules.get().add(new AutoStaircase());
        Modules.get().add(new ShulkerDupe());
        Modules.get().add(new InvDupeModule());
        Modules.get().add(new NewerNewChunks());
        Modules.get().add(new InstantKill());
        Modules.get().add(new Teleport());
        Modules.get().add(new BetterScaffold());
        Modules.get().add(new AutoBuild());
        Modules.get().add(new AirstrikePlus());
        Modules.get().add(new BoomPlus());
        Modules.get().add(new VoiderPlus());
        Modules.get().add(new HandOfGod());
        Modules.get().add(new ExplosionAura());
        Modules.get().add(new RedstoneNuker());
        Modules.get().add(new TPFly());
        Modules.get().add(new AutoDrop());
        Modules.get().add(new Phase());
        Modules.get().add(new TrouserFlight());
        Modules.get().add(new LecternCrash());
        Modules.get().add(new AnHero());
        Commands.get().add(new LavaTimeCalculator());
        Commands.get().add(new CasterTimer());

	}

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(Main);
	}

    public String getPackage() {
        return "pwn.noobs.trouserstreak";
    }

}
