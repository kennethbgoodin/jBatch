package de.slikey.batch.network.client;

import de.slikey.batch.network.NIOComponent;
import de.slikey.batch.network.protocol.Packet;
import de.slikey.batch.network.protocol.PacketChannelInitializer;
import de.slikey.batch.network.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ResourceLeakDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author Kevin
 * @since 23.03.2015
 */
public abstract class NIOClient extends NIOComponent {

    private static final Logger logger = LogManager.getLogger(NIOClient.class.getSimpleName());

    private final String host;
    private final int port;
    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    private boolean reconnect;

    public NIOClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.reconnect = false;
    }

    public void run() {
        eventLoopGroup = new NioEventLoopGroup();

        try {
            Protocol.initialize();
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

            logger.info("Attempting to connect to " + host + ":" + port + "...");

            connect();
            startClient();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public void connect() throws InterruptedException {
        channel = null;
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
                .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
                .handler(buildPacketChannelInitializer());
        channel = bootstrap.connect(host, port)
                .sync()
                .channel();
        System.out.println(channel);
    }

    protected void close() throws InterruptedException {
        if (channel != null)
            channel.close().awaitUninterruptibly();
        System.out.println("Shutdown requested...");
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
            System.out.println("Successfully shutdown!");
        } else {
            System.out.println("There is no EventLoopGroup!");
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public void sendPacket(Packet packet) {
        channel.writeAndFlush(packet);
    }

    public boolean isRunning() {
        return channel != null && channel.isActive();
    }

    protected Channel getChannel() {
        return channel;
    }

    protected abstract PacketChannelInitializer buildPacketChannelInitializer();

    protected abstract void startClient() throws InterruptedException;

}
