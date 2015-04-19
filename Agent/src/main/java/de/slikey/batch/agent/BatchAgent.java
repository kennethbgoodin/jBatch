package de.slikey.batch.agent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.slikey.batch.network.client.NIOClient;
import de.slikey.batch.network.protocol.PacketChannelInitializer;
import de.slikey.batch.network.protocol.PacketHandler;
import de.slikey.batch.network.protocol.packet.Packet2HealthStatus;
import de.slikey.batch.network.protocol.packet.Packet4Ping;
import de.slikey.batch.network.protocol.packet.Packet5Pong;
import de.slikey.batch.network.protocol.packet.Packet6KeepAlive;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kevin Carstens
 * @since 13.04.2015
 */
public class BatchAgent extends NIOClient {

    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("ChatClient")
            .build());

    public static void main(String[] args) throws InterruptedException {
        new BatchAgent("localhost", 8080).run();
    }

    public BatchAgent(String host, int port) {
        super(host, port);
    }

    @Override
    protected PacketChannelInitializer buildPacketChannelInitializer() {
        return new PacketChannelInitializer() {

            @Override
            public PacketHandler newPacketChannelHandler(final SocketChannel socketChannel) {
                return new PacketHandler() {

                    @Override
                    public void handle(Packet4Ping packet) {
                        socketChannel.writeAndFlush(new Packet5Pong(packet, System.nanoTime()));
                    }

                    @Override
                    public void handle(Packet5Pong packet) {
                        System.out.println(packet);
                    }

                };
            }

            @Override
            protected ReadTimeoutHandler newReadTimeoutHandler(SocketChannel socketChannel) {
                return null;
            }
        };
    }

    @Override
    protected void startClient() throws InterruptedException {
        final Channel channel = getChannel();
        final Random random = new Random(System.nanoTime());
        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (channel.isActive()) {
                        channel.writeAndFlush(new Packet6KeepAlive());
                        Thread.sleep(random.nextInt(4000));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (channel.isActive()) {
                        channel.writeAndFlush(Packet2HealthStatus.create());
                        Thread.sleep(random.nextInt(1000));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    while (channel.isActive()) {
                        String line = reader.readLine();
                        if (line == null)
                            continue;
                        if (line.equals("/quit")) {
                            synchronized (BatchAgent.this) {
                                BatchAgent.this.notify();
                            }
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        synchronized (this) {
            wait();
        }
    }

}