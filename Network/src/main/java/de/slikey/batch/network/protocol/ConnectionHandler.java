package de.slikey.batch.network.protocol;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author Kevin
 * @since 17.04.2015
 */
public abstract class ConnectionHandler extends ChannelHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(ConnectionHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(">> Connected new Agent! (" + ctx.channel().remoteAddress() + ")");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("<< Disconnected Agent! (" + ctx.channel().remoteAddress() + ")");

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ctx.channel().isActive()) {
            String address = ctx.channel().remoteAddress().toString();
            if (cause instanceof ReadTimeoutException) {
                logger.info("xx " + address + " timed out.");
            } else if (cause instanceof IOException) {
                logger.info("xx " + address + " IOException: " + cause.getMessage());
            } else if (cause instanceof DecoderException) {
                logger.info("xx " + address + " sent a bad packet: " + cause.getMessage());
                if (cause.getCause() instanceof IndexOutOfBoundsException)
                    cause.printStackTrace();
            } else {
                logger.info("xx " + address + " encountered exception: ");
                cause.printStackTrace();
            }

            ctx.close();
        }
    }

    public abstract PacketHandler newPacketHandler();
}
