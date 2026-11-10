/*
 * Copyright (c) 2026 EiluDick/ZDarkZ and contributors.
 * Released under the MIT License.
 */
package nc.geyserext.netease.initializer;

import io.netty.channel.Channel;
import nc.geyserext.netease.handler.NetEaseUpstreamHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.NoopCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.GeyserServerInitializer;
import org.geysermc.geyser.network.bedrock.InvalidPacketHandler;
import org.geysermc.geyser.session.GeyserSession;

/** Only used by the dedicated NetEase UDP port; v11/NetherNet keep their own Geyser pipeline. */
public final class NeteaseServerInitializer extends GeyserServerInitializer {
    public NeteaseServerInitializer(GeyserImpl geyser) {
        super(geyser, "Geyser NetEase player thread");
    }

    @Override
    protected void preInitChannel(Channel channel) throws Exception {
        super.preInitChannel(channel);
        if (channel.pipeline().get(CompressionCodec.class) == null) {
            throw new IllegalStateException("NetEase compression pipeline unavailable");
        }
        channel.pipeline().replace(CompressionCodec.NAME, CompressionCodec.NAME,
            new CompressionCodec(new SimpleCompressionStrategy(new NoopCompression()), false));
    }

    @Override
    protected void initPacketCodec(Channel channel) throws Exception {
        channel.pipeline().addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3());
    }

    @Override
    public void initSession(@NonNull BedrockServerSession bedrockSession) {
        try {
            bedrockSession.setLogging(geyser.config().debugMode());
            GeyserSession session = new GeyserSession(geyser, bedrockSession, getEventLoopGroup().next());
            if (!bedrockSession.isSubClient()) {
                InvalidPacketHandler errorHandler = bedrockSession.getPeer().getChannel()
                    .pipeline().get(InvalidPacketHandler.class);
                if (errorHandler != null) errorHandler.setSession(session);
            }
            bedrockSession.setPacketHandler(new NetEaseUpstreamHandler(geyser, session));
        } catch (Throwable error) {
            geyser.getLogger().error("[GeyserNetease] Error initializing NetEase session", error);
            bedrockSession.disconnect("Unable to initialize NetEase session");
        }
    }
}
