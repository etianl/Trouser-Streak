package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import pwn.noobs.trouserstreak.Trouser;

public class NoTelemetry extends Module {
    public NoTelemetry() {
        super(Trouser.Main, "NoTelemetry", "Prevents the required telemetry from being sent to Mojang. You can find blocked telemetry events in latest.log.");
    }
}