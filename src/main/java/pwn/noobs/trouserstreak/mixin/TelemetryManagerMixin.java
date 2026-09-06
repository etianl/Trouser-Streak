package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryPropertyMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.NoTelemetry;

import java.util.function.Consumer;

@Mixin(net.minecraft.client.telemetry.ClientTelemetryManager.class)
public abstract class TelemetryManagerMixin {

    private static final TelemetryEventSender NOOP_SENDER = new TelemetryEventSender() {
        @Override
        public void send(TelemetryEventType eventType, Consumer<TelemetryPropertyMap.Builder> propertyAdder) {
            System.out.println("[NoTelemetry] Blocked telemetry send: " + eventType);
        }

        @Override
        public TelemetryEventSender decorate(Consumer<TelemetryPropertyMap.Builder> decorationAdder) {
            return this;
        }
    };

    @Inject(
            method = "createEventSender()Lnet/minecraft/client/telemetry/TelemetryEventSender;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onComputeSenderHead(CallbackInfoReturnable<TelemetryEventSender> cir) {
        Modules modules = Modules.get();
        if (modules != null && modules.isActive(NoTelemetry.class)) {
            cir.setReturnValue(NOOP_SENDER);
        }
    }

    @Inject(
            method = "getOutsideSessionSender()Lnet/minecraft/client/telemetry/TelemetryEventSender;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetSender(CallbackInfoReturnable<TelemetryEventSender> cir) {
        Modules modules = Modules.get();
        if (modules != null && modules.isActive(NoTelemetry.class)) {
            cir.setReturnValue(NOOP_SENDER);
        }
    }
}