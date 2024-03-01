package pwn.noobs.trouserstreak;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pwn.noobs.trouserstreak.commands.*;
import pwn.noobs.trouserstreak.modules.*;

import java.util.Optional;


public class Trouser extends MeteorAddon {
        public static final Logger LOG = LoggerFactory.getLogger(Trouser.class);
        public static final Category Main = new Category("TrouserStreak");

        @Override
        public void onInitialize() {
                LOG.info("Initializing PantsMod!");

                Modules.get().add(new AutoLavaCaster());
                Modules.get().add(new AutoMountain());
                Modules.get().add(new AutoStaircase());
                Modules.get().add(new TrouserBuild());
                Modules.get().add(new TrailMaker());
                Modules.get().add(new NewerNewChunks());
                Modules.get().add(new SuperInstaMine());
                Modules.get().add(new BaseFinder());
                Modules.get().add(new Teleport());
                Modules.get().add(new TPFly());
                Modules.get().add(new HandOfGod());
                Modules.get().add(new OPServerKillModule());
                Modules.get().add(new OPplayerTPmodule());
                Modules.get().add(new ExplosionAura());
                Modules.get().add(new ShulkerDupe());
                Modules.get().add(new InvDupeModule());
                Modules.get().add(new InstantKill());
                Modules.get().add(new LavaAura());
                Modules.get().add(new LecternCrash());
                Modules.get().add(new AutoDrop());
                Modules.get().add(new NbtEditor());
                Modules.get().add(new AnHero());
                Modules.get().add(new RedstoneNuker());
                Modules.get().add(new AirstrikePlus());
                Modules.get().add(new BoomPlus());
                Modules.get().add(new VoiderPlus());
                Modules.get().add(new BetterScaffold());
                Modules.get().add(new BetterAutoSign());
                Modules.get().add(new FlightAntikick());
                Modules.get().add(new BlockListMineCommand());
                Modules.get().add(new AutoCommand());
                Modules.get().add(new AutoScoreboard());
                Modules.get().add(new AutoTitles());
                Modules.get().add(new AutoDisplays());
                Commands.add(new LavaTimeCalculator());
                Commands.add(new CasterTimer());
                Commands.add(new NewChunkCounter());
                Commands.add(new BaseFinderCommands());
                Commands.add(new WorldInfoCommand());
                Commands.add(new ViewNbtCommand());
                Commands.add(new AutoVclipCommand());
                Commands.add(new AutoVaultClipCommand());
                Commands.add(new CrashCommand());
                Commands.add(new GarbageCleanerCommand());
        }


        @Override
        public void onRegisterCategories() {
                Modules.registerCategory(Main);
        }

        @Override
        public String getWebsite() {
                return "https://github.com/etianl/trouser-streak";
        }

        @Override
        public GithubRepo getRepo() {
                return new GithubRepo("etianl", "trouser-streak", "main");
        }

        @Override
        public String getCommit() {
                Optional<String> commit = FabricLoader
                        .getInstance()
                        .getModContainer("streak-addon")
                        .map(ModContainer::getMetadata)
                        .map(metadata -> metadata.getCustomValue("github:sha"))
                        .map(CustomValue::getAsString);

                if (commit.isPresent() && !commit.get().isEmpty()) {
                        LOG.info(String.format("Pants version: %s", commit.get().trim()));
                        return commit.get().trim();
                } else {
                        LOG.warn("Pants version not available or empty.");
                        return null;
                }
        }

        public String getPackage() {
                return "pwn.noobs.trouserstreak";
        }
}
