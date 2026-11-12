package pwn.noobs.trouserstreak.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.haproxy.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.HAProxySpoofer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    @Final
    public abstract SocketAddress getRemoteAddress();
    @Shadow
    private Channel channel;
    @Inject(method = "channelActive", at = @At("TAIL"))
    private void trouser$onChannelActive(
            ChannelHandlerContext ctx, CallbackInfo ci
    ) {
        if (!Modules.get().get(HAProxySpoofer.class).isActive())return;
        String spoofedAddress = Modules.get().get(HAProxySpoofer.class).spoofedAddress.get();
        HAProxyProtocolVersion version = Modules.get().get(HAProxySpoofer.class).haProxyProtocolVersion.get();

        SocketAddress rawAddress = this.getRemoteAddress();
        if (rawAddress instanceof InetSocketAddress inetAddress && channel.localAddress() instanceof InetSocketAddress localAddr) {
            String serverIp = inetAddress.getAddress().getHostAddress();
            int serverPort = inetAddress.getPort();
            HAProxyMessage proxyHeader = new HAProxyMessage(
                    version,
                    HAProxyCommand.PROXY,
                    HAProxyProxiedProtocol.TCP4,
                    spoofedAddress,
                    serverIp,
                    localAddr.getPort(),
                    serverPort
            );
            if (channel.pipeline().get("haproxy-encoder") == null) {
                channel.pipeline().addFirst(
                        "haproxy-encoder",
                        HAProxyMessageEncoder.INSTANCE
                );
            }
            channel.writeAndFlush(proxyHeader);
        }
    }
}
