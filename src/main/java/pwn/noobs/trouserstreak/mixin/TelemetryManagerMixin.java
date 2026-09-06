package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.session.telemetry.PropertyMap;
import net.minecraft.client.session.telemetry.TelemetryEventType;
import net.minecraft.client.session.telemetry.TelemetrySender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.NoTelemetry;

import java.util.function.Consumer;

@Mixin(net.minecraft.client.session.telemetry.TelemetryManager.class)
public abstract class TelemetryManagerMixin {

    private static final TelemetrySender NOOP_SENDER = new TelemetrySender() {
        @Override
        public void send(TelemetryEventType eventType, Consumer<PropertyMap.Builder> propertyAdder) {
            System.out.println("[NoTelemetry] Blocked telemetry send: " + eventType);
        }

        @Override
        public TelemetrySender decorate(Consumer<PropertyMap.Builder> decorationAdder) {
            return this;
        }
    };

    @Inject(
            method = "computeSender()Lnet/minecraft/client/session/telemetry/TelemetrySender;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onComputeSenderHead(CallbackInfoReturnable<TelemetrySender> cir) {
        Modules modules = Modules.get();
        if (modules != null && modules.isActive(NoTelemetry.class)) {
            cir.setReturnValue(NOOP_SENDER);
        }
    }

    @Inject(
            method = "getSender()Lnet/minecraft/client/session/telemetry/TelemetrySender;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetSender(CallbackInfoReturnable<TelemetrySender> cir) {
        Modules modules = Modules.get();
        if (modules != null && modules.isActive(NoTelemetry.class)) {
            cir.setReturnValue(NOOP_SENDER);
        }
    }
}