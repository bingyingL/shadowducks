package com.github.netfreer.shadowducks.common.handler;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;

/**
 * @author: landy
 * @date: 2017-04-11 20:26
 */
public class TransferHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(TransferHandler.class);
    private final Channel output;
    private final String tag;

    public TransferHandler(Channel output, String tag) {
        this.output = output;
        this.tag = tag;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ChannelOutboundBuffer outboundBuffer = ctx.channel().unsafe().outboundBuffer();
        if (outboundBuffer.totalPendingWriteBytes() > 0) {
            LOG.debug("[{}] any data need to flush", tag);
            ctx.flush();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        output.write(msg).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        output.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (output.isOpen()) {
            output.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        InetSocketAddress from = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = from.isUnresolved() ? from.getAddress().getHostAddress() : from.getHostName();
        if (cause instanceof ClosedChannelException) {
            //ignore
        } else {
            String exName = cause.getClass().getSimpleName();
            if (cause instanceof IOException) {
                LOG.warn("[{}] Read from {}:{} {}: {}", tag, host, from.getPort(), exName, cause.getMessage());
            } else {
                LOG.warn("[{}] Read from {}:{} {}", tag, host, from.getPort(), exName, cause);
            }
        }
        ctx.close();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[{}] channel writable: true, buffer empty: {}", tag, ctx.channel().unsafe().outboundBuffer().totalPendingWriteBytes());
            }
            output.config().setAutoRead(true);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[{}] channel writable: false, buffer full: {}", tag, ctx.channel().unsafe().outboundBuffer().totalPendingWriteBytes());
            }
            output.config().setAutoRead(false);
        }
    }
}
