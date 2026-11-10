/*
 * Copyright (c) 2026 EiluDick/ZDarkZ and contributors.
 * Released under the MIT License.
 */
package nc.geyserext.netease.util;

import io.netty.bootstrap.ServerBootstrap;
import nc.geyserext.netease.initializer.NeteaseServerInitializer;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.channel.raknet.config.RakServerCookieMode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.RaknetServer;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

/**
 * Starts a second, NetEase-only UDP listener. Never shuts down or modifies
 * Geyser's primary listener; the legacy restart logic has been removed.
 *
 * This is an internal-ABI integration and must be revalidated for every Geyser build.
 */
public final class ServerRestartUtil {
    private static RaknetServer neteaseServer;
    private static NeteaseServerInitializer neteaseInitializer;

    private ServerRestartUtil() {}

    public static synchronized void start(int port) {
        if (port == 0) return; // Explicitly disabled unless configured.
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid NetEase UDP port: " + port);
        GeyserImpl geyser = GeyserImpl.getInstance();
        if (port == geyser.config().bedrock().port()) {
            throw new IllegalArgumentException("NetEase UDP port must differ from the primary Bedrock port");
        }
        if (neteaseServer != null) throw new IllegalStateException("NetEase listener already running");

        RaknetServer candidate = new RaknetServer(geyser,
            Math.max(1, Integer.getInteger("Geyser.BedrockNetworkThreads", 2)));
        NeteaseServerInitializer initializer = new NeteaseServerInitializer(geyser);
        try {
            // This is an independent RaknetServer instance, NOT geyser.getGeyserServer().
            Field bootstrapField = RaknetServer.class.getDeclaredField("bootstrap");
            bootstrapField.setAccessible(true);
            ServerBootstrap bootstrap = (ServerBootstrap) bootstrapField.get(candidate);
            if (bootstrap == null) throw new IllegalStateException("Geyser RaknetServer bootstrap missing");

            bootstrap.option(RakChannelOption.RAK_SUPPORTED_PROTOCOLS, new int[] {8});
            bootstrap.option(RakChannelOption.RAK_SERVER_COOKIE_MODE, RakServerCookieMode.INVALID);
            bootstrap.childHandler(initializer);
            candidate.bind(new InetSocketAddress(geyser.config().bedrock().address(), port)).join();

            neteaseInitializer = initializer;
            neteaseServer = candidate;
            geyser.getLogger().info("[GeyserNetease] NetEase-only RakNet v8 listener bound on UDP " + port);
        } catch (Throwable error) {
            close(candidate, initializer);
            throw new IllegalStateException("NetEase listener failed; primary Geyser listener was not touched", error);
        }
    }

    public static synchronized void stop() {
        RaknetServer server = neteaseServer;
        NeteaseServerInitializer initializer = neteaseInitializer;
        neteaseServer = null;
        neteaseInitializer = null;
        close(server, initializer);
    }

    private static void close(RaknetServer server, NeteaseServerInitializer initializer) {
        if (server != null) {
            try {
                server.shutdown();
            } catch (Throwable error) {
                GeyserImpl.getInstance().getLogger().warning(
                    "[GeyserNetease] Isolated listener cleanup failed: " + error);
            }
        }
        if (initializer != null) {
            initializer.getEventLoopGroup().shutdownGracefully();
        }
    }
}
