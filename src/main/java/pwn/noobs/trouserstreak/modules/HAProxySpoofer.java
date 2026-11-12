package pwn.noobs.trouserstreak.modules;

import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class HAProxySpoofer extends Module {
    private final SettingGroup specialGroup = settings.createGroup("Credits to soccer1992.");
    public Setting<String> spoofedAddress = specialGroup.add(new StringSetting.Builder()
            .name("spoofed-address")
            .description("The spoofed IP address that will be sent to the server.")
            .defaultValue("127.0.0.1")
            .filter((text, c) -> (text + c).matches("^[0-9a-f\\\\.:]{0,45}$"))
            .build()
    );
    public Setting<HAProxyProtocolVersion> haProxyProtocolVersion = specialGroup.add(new EnumSetting.Builder<HAProxyProtocolVersion>()
            .name("haproxy-mode")
            .description("HAProxy protocol mode that is used")
            .defaultValue(HAProxyProtocolVersion.V2)
            .build());
    public HAProxySpoofer() {
        super(Trouser.Main, "HAProxySpoofer", "Allows you to join servers with an exposed HAProxy forwarding backend. ONLY ENABLE THIS IF YOU ACTUALLY WANT TO JOIN A HASPOOFABLE SERVER! IPV4 only.");
    }
}