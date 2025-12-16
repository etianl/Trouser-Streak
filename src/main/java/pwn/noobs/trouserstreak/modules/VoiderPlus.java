package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

public class VoiderPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle Module if not OP")
            .description("Turn this off to prevent the bug of module always being turned off when you join server.")
            .defaultValue(false)
            .build()
    );
    private final Setting<String> block = sgGeneral.add(new StringSetting.Builder()
            .name("Block to be used for /fill")
            .description("What is created.")
            .defaultValue("air")
            .build());
    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
            .name("radius")
            .description("radius")
            .defaultValue(45)
            .sliderRange(1, 90)
            .build());
    public final Setting<Boolean> getplayerY = sgGeneral.add(new BoolSetting.Builder()
            .name("UsePlayerY")
            .description("Use the player's Y level for calculating where voider will start.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> playerheight = sgGeneral.add(new IntSetting.Builder()
            .name("maxheight(fromplayerY)")
            .description("maxheight")
            .defaultValue(0)
            .sliderRange(-64, 64)
            .visible(getplayerY::get)
            .build());
    private final Setting<Integer> maxheight = sgGeneral.add(new IntSetting.Builder()
            .name("maxheight")
            .description("maxheight")
            .defaultValue(128)
            .sliderRange(64, 319)
            .visible(() -> !getplayerY.get())
            .build());
    private final Setting<Integer> minheight = sgGeneral.add(new IntSetting.Builder()
            .name("minheight")
            .description("minheight")
            .defaultValue(-64)
            .sliderRange(-64, 128)
            .build());
    public final Setting<Boolean> threebythree = sgGeneral.add(new BoolSetting.Builder()
            .name("VoiderBot3x3")
            .description("Runs voider nine times in a 3x3 grid pattern to replace a whole lot more")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> tpfwd = sgGeneral.add(new BoolSetting.Builder()
            .name("TP forward")
            .description("Teleports you double your radius forward after voiding to aid in voiding a perfect strip.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> tgl = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle off after TP forward")
            .description("Turn module off after TP, or not.")
            .defaultValue(false)
            .visible(tpfwd::get)
            .build()
    );

    public VoiderPlus() {
        super(Trouser.operator, "voider+", "Runs /fill on the world from the top down (Must have OP status)");
    }
    int i;
    private int passes=0;
    private int TPs=0;
    private int pX;
    private int pZ;
    private int sX;
    private int sY;
    private int sZ;
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
        if (event.screen instanceof DeathScreen) {
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get())toggle();
    }
    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;
        if (notOP.get() && PermissionUtils.getPermissionLevel(mc.player) < 2 && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            toggle();
            error("Must have permission level 2 or higher");
        }
        if (!getplayerY.get()){
            i = maxheight.get();
        }else if (getplayerY.get()){
            i=mc.player.getBlockY()+playerheight.get();
        }
        sX=mc.player.getBlockPos().getX();
        sY=mc.player.getBlockPos().getY();
        sZ=mc.player.getBlockPos().getZ();
    }
    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (!threebythree.get() && !tpfwd.get()){
            ChatUtils.sendPlayerMsg("/fill " + (sX - radius.get()) + " " + i +" "+ (sZ - radius.get()) +" "+ (sX + radius.get()) + " " + i +" "+ (sZ + radius.get()) + " "+block);
            i--;
            if (i<=minheight.get()){
                if (!getplayerY.get()){
                    i = maxheight.get();
                }else if (getplayerY.get()){
                    i=mc.player.getBlockY()+playerheight.get();
                }
                toggle();
            }
        }  else if (!threebythree.get() &&tpfwd.get()){
            if (i>= maxheight.get() || (i>= mc.player.getBlockY()+playerheight.get() && getplayerY.get())){
                sX=mc.player.getBlockPos().getX();
                sY=mc.player.getBlockPos().getY();
                sZ=mc.player.getBlockPos().getZ();
            }
            ChatUtils.sendPlayerMsg("/fill " + (sX - radius.get()) + " " + i +" "+ (sZ - radius.get()) +" "+ (sX + radius.get()) + " " + i +" "+ (sZ + radius.get()) + " "+block);
            i--;
            if (i<=minheight.get()){
                switch (mc.player.getMovementDirection()){
                    case EAST -> ChatUtils.sendPlayerMsg("/tp "+(sX+(radius.get()*2))+" "+sY+" "+sZ);
                    case WEST -> ChatUtils.sendPlayerMsg("/tp "+(sX+(-(radius.get()*2)))+" "+sY+" "+sZ);
                    case NORTH -> ChatUtils.sendPlayerMsg("/tp "+sX+" "+sY+" "+(sZ+(-(radius.get()*2))));
                    case SOUTH -> ChatUtils.sendPlayerMsg("/tp "+sX+" "+sY+" "+(sZ+(radius.get()*2)));
                }
                if (!getplayerY.get()){
                    i = maxheight.get();
                }else if (getplayerY.get()){
                    i=mc.player.getBlockY()+playerheight.get();
                }
                if (tgl.get()) toggle();
            }
        } else if (threebythree.get() && !tpfwd.get()){
            if (i<=maxheight.get() && passes==0 && TPs==0 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes==0 && TPs==0 )){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=1;
                }
            } else if (i==maxheight.get()+1 && passes == 1 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 1)){
                ChatUtils.sendPlayerMsg("/tp "+(sX+(radius.get()*2))+" "+sY+" "+sZ);
                TPs=1;
                i--;
            } else if (i<=maxheight.get() && passes == 1 && TPs==1 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 1 && TPs==1)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=2;
                }
            } else if (i==maxheight.get()+1 && passes == 2 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 2 )){
                ChatUtils.sendPlayerMsg("/tp "+(sX+(radius.get()*2))+" "+sY+" "+(sZ+(-(radius.get()*2))));
                TPs=2;
                i--;
            } else if (i<=maxheight.get() && passes == 2 && TPs==2 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 2 && TPs==2)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=3;
                }
            } else if (i==maxheight.get()+1 && passes == 3 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 3)){
                ChatUtils.sendPlayerMsg("/tp "+sX+" "+sY+" "+(sZ+(-(radius.get()*2))));
                TPs=3;
                i--;
            } else if (i<=maxheight.get() && passes == 3 && TPs==3 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 3 && TPs==3)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=4;
                }
            } else if (i==maxheight.get()+1 && passes == 4 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 4 )){
                ChatUtils.sendPlayerMsg("/tp "+(sX+(-(radius.get()*2)))+" "+sY+" "+(sZ+(-(radius.get()*2))));
                TPs=4;
                i--;
            } else if (i<=maxheight.get() && passes == 4 && TPs==4 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 4 && TPs==4)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=5;
                }
            } else if (i==maxheight.get()+1 && passes == 5 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 5)){
                ChatUtils.sendPlayerMsg("/tp "+(sX+(-(radius.get()*2)))+" "+sY+" "+sZ);
                TPs=5;
                i--;
            } else if (i<=maxheight.get() && passes == 5 && TPs==5 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 5 && TPs==5)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=6;
                }
            } else if (i==maxheight.get()+1 && passes == 6 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 6)){
                ChatUtils.sendPlayerMsg("/tp "+(sX+(-(radius.get()*2)))+" "+sY+" "+(sZ+(radius.get()*2)));
                TPs=6;
                i--;
            } else if (i<=maxheight.get() && passes == 6 && TPs==6 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 6 && TPs==6)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=7;
                }
            } else if (i==maxheight.get()+1 && passes == 7 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 7)){
                ChatUtils.sendPlayerMsg("/tp "+sX+" "+sY+" "+(sZ+(radius.get()*2)));
                TPs=7;
                i--;
            } else if (i<=maxheight.get() && passes == 7 && TPs==7 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 7 && TPs==7)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=8;
                }
            } else if (i==maxheight.get()+1 && passes == 8 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes == 8)){
                ChatUtils.sendPlayerMsg("/tp "+(sX+(radius.get()*2))+" "+sY+" "+(sZ+(radius.get()*2)));
                TPs=8;
                i--;
            } else if (i<=maxheight.get() && passes == 8 && TPs==8 || (getplayerY.get() && i<=mc.player.getBlockY()+playerheight.get() && passes == 8 && TPs==8)){
                i--;
                pX=mc.player.getBlockPos().getX();
                pZ=mc.player.getBlockPos().getZ();
                ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + i +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + i +" "+ (pZ + radius.get()) + " "+block);
                if (i<=minheight.get()){
                    if (!getplayerY.get()){
                        i = maxheight.get()+1;
                    }else if (getplayerY.get()){
                        i=mc.player.getBlockY()+playerheight.get()+1;
                    }
                    passes=9;
                }
            } else if (i==maxheight.get()+1 && passes >= 9 || (getplayerY.get() && i==mc.player.getBlockY()+playerheight.get()+1 && passes >= 9)){
                ChatUtils.sendPlayerMsg("/tp "+sX+" "+sY+" "+sZ);
                if (!getplayerY.get()){
                    i = maxheight.get();
                }else if (getplayerY.get()){
                    i=mc.player.getBlockY()+playerheight.get();
                }
                passes=0;
                TPs=0;
                toggle();
            }
        }
        else if (threebythree.get() && tpfwd.get()){
            error("Do Not use TPforward with VoiderBot.");
            toggle();
        }
    }
}