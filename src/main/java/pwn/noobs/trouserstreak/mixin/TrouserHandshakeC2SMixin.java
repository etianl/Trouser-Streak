package pwn.noobs.trouserstreak.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.BungeeSpoofer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
//credits to DAM for the sauce
@Mixin(ClientIntentionPacket.class)
public abstract class TrouserHandshakeC2SMixin {
    @Unique
    private static final Gson gson = new Gson();
    @Mutable
    @Shadow
    @Final
    private String hostName;

    @Shadow
    public abstract ClientIntent intention();

    @Inject(method = "<init>(ILjava/lang/String;ILnet/minecraft/network/protocol/handshake/ClientIntent;)V", at = @At("RETURN"))
    private void onHandshakeC2SPacket(int i, String string, int j, ClientIntent connectionIntent, CallbackInfo ci) {
        if (!Modules.get().get(BungeeSpoofer.class).isActive()) return;
        if (this.intention() != ClientIntent.LOGIN) return;
        String spoofedUUID = mc.getUser().getProfileId().toString();

        String URL = "https://api.mojang.com/users/profiles/minecraft/" + mc.getUser().getName();

        Http.Request request = Http.get(URL);
        String response = request.sendString();
        if (response != null) {
            JsonObject jsonObject = gson.fromJson(response, JsonObject.class);

            if (jsonObject != null && jsonObject.has("id")) {
                spoofedUUID = jsonObject.get("id").getAsString();
            }
        }

        this.hostName += "\u0000" + Modules.get().get(BungeeSpoofer.class).spoofedAddress.get() + "\u0000" + spoofedUUID;
    }
}