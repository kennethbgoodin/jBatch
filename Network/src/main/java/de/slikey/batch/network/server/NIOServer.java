package de.slikey.batch.network.server;

import de.slikey.batch.network.protocol.PacketChannelInitializer;
import de.slikey.batch.network.server.listener.StartServerListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;

/**
 * @author Kevin
 * @since 23.03.2015
 */
public abstract class NIOServer {

    private final int port;
    private EventLoopGroup bossLoop, workerLoop;

    public NIOServer(int port) {
        this.port = port;
    }

    public void run() {
        bossLoop = new NioEventLoopGroup();
        workerLoop = new NioEventLoopGroup();

        try {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossLoop, workerLoop)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(buildPacketChannelInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("Attempting to bind on port " + port + "...");
            bootstrap.bind(port)
                    .sync()
                    .addListener(new StartServerListener(port))
                    .channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossLoop.shutdownGracefully();
            workerLoop.shutdownGracefully();
        }
    }

    protected void close() {
        System.out.println("Shutdown requested...");
        if (bossLoop != null) {
            bossLoop.shutdownGracefully().awaitUninterruptibly();
            System.out.println("Successfully shut bossLoop down!");
        } else {
            System.out.println("There is no bossLoop!");
        }

        if (workerLoop != null) {
            workerLoop.shutdownGracefully().awaitUninterruptibly();
            System.out.println("Successfully shut workerLoop down!");
        } else {
            System.out.println("There is no workerLoop!");
        }
    }

    protected abstract PacketChannelInitializer buildPacketChannelInitializer();

}