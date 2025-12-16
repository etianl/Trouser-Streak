package pwn.noobs.trouserstreak;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pwn.noobs.trouserstreak.commands.*;
import pwn.noobs.trouserstreak.hud.*;
import pwn.noobs.trouserstreak.modules.*;


public class Trouser extends MeteorAddon {
        public static final Logger LOG = LoggerFactory.getLogger(Trouser.class);
        public static final Category Main = new Category("TrouserStreak", Items.LEATHER_LEGGINGS.getDefaultStack());
        public static final Category baseHunting = new Category("TrouserHunting", Items.SPYGLASS.getDefaultStack());
        public static final Category operator = new Category("TrouserOP/Creative", Items.NETHER_STAR.getDefaultStack());

        //false commented out modules to categorize modules when displayed on the anticope page
        @Override
        public void onInitialize() {
                LOG.info("Initializing PantsMod!");
                //Modules.get().add(new **Official Griefing Utilities of Mountains of Lava Inc.**());
                //Modules.get().add(new !!!!!Featuring powerful Grief Tools and Home of the Original PaletteExploit for NewChunks!!!!!());
                //Modules.get().add(new -----> Automated Lavacasting Features! Make Lava Mountains fast and easily! <-----());
                Modules.get().add(new AutoMountain());
                Modules.get().add(new AutoLavaCaster());
                Modules.get().add(new AutoStaircase());
                Commands.add(new LavaTimeCalculator());
                Commands.add(new CasterTimer());

                //Modules.get().add(new -----> Find and Grief noobs! <-----());
                Modules.get().add(new WaypointCoordExploit());
                Modules.get().add(new NewerNewChunks());
                Modules.get().add(new BaseFinder());
                Modules.get().add(new InfiniteElytra());
                Modules.get().add(new ActivatedSpawnerDetector());
                Modules.get().add(new NoSpawnerDetector());
                Modules.get().add(new PortalPatternFinder());
                Modules.get().add(new CaveDisturbanceDetector());
                Modules.get().add(new AdvancedItemESP());
                Modules.get().add(new MobGearESP());
                Modules.get().add(new PotESP());
                Modules.get().add(new OnlinePlayerActivityDetector());
                Modules.get().add(new HoleAndTunnelAndStairsESP());
                Modules.get().add(new StorageLooter());
                Modules.get().add(new LavaAura());
                Modules.get().add(new MaceKill());
                Modules.get().add(new SuperInstaMine());
                Modules.get().add(new InstaMineNuker());
                Modules.get().add(new BetterScaffold());
                Modules.get().add(new RedstoneNuker());

                //Modules.get().add(new -----> Create Illegal things with Creative mode! <-----());
                Modules.get().add(new ForceOPSign());
                Modules.get().add(new ForceOPBook());
                Modules.get().add(new NbtEditor());
                Modules.get().add(new AirstrikePlus());
                Modules.get().add(new BoomPlus());
                Modules.get().add(new ExplosionAura());

                //Modules.get().add(new -----> Overpowered OP mode modules! <-----());
                Modules.get().add(new BungeeSpoofer());
                Modules.get().add(new HandOfGod());
                Modules.get().add(new VoiderPlus());
                Modules.get().add(new OPServerKillModule());
                Commands.add(new CrashCommand());
                Modules.get().add(new AutoCommand());
                Modules.get().add(new AutoScoreboard());
                Modules.get().add(new AutoTitles());
                Modules.get().add(new AutoDisplays());
                Modules.get().add(new AutoTexts());
                Commands.add(new TextCommand());
                Modules.get().add(new AutoNames());
                Modules.get().add(new OPplayerTPmodule());
                Modules.get().add(new MultiverseAnnihilator());

                //Modules.get().add(new -----> Exploits for old versions! <-----());
                Modules.get().add(new BookAndQuillDupe());
                Modules.get().add(new ShulkerDupe());
                Modules.get().add(new InvDupeModule());
                Modules.get().add(new BoatKill());
                Modules.get().add(new InstantKill());
                Modules.get().add(new LecternCrash());

                //Modules.get().add(new -----> And much more <-----());
                Modules.get().add(new RemoteEnderChest());
                Modules.get().add(new AttributeSwap());
                Modules.get().add(new InfiniteTools());
                Modules.get().add(new Teleport());
                Modules.get().add(new PortalGodMode());
                Modules.get().add(new ItemTractorBeam());
                Modules.get().add(new CollectibleESP());
                Modules.get().add(new TPFly());
                Modules.get().add(new FlightAntikick());
                Modules.get().add(new BetterAutoSign());
                Modules.get().add(new InstaSafetyBox());
                Modules.get().add(new TrouserBuild());
                Modules.get().add(new TrailMaker());
                Modules.get().add(new MultiUse());
                Modules.get().add(new AutoDrop());
                Modules.get().add(new AnHero());
                Modules.get().add(new PlayerAlarms());
                Commands.add(new WorldInfoCommand());
                Commands.add(new ViewNbtCommand());
                Commands.add(new AutoVclipCommand());
                Commands.add(new AutoVaultClipCommand());
                Modules.get().add(new BlockListMineCommand());
                Commands.add(new GarbageCleanerCommand());

                //Modules.get().add(new -----> Additions to the HUD module! <-----());
                Hud.get().register(WaypointTriangulationHud.INFO);
                Hud.get().register(ElytraCount.INFO);
        }

        @Override
        public void onRegisterCategories() {
                Modules.registerCategory(Main);
                Modules.registerCategory(baseHunting);
                Modules.registerCategory(operator);
        }

        public String getPackage() {
                return "pwn.noobs.trouserstreak";
        }

}