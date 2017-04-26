package com.github.netfreer.shadowducks.server.handler;

import com.github.netfreer.shadowducks.common.config.AppConfig;
import com.github.netfreer.shadowducks.common.handler.HandlerCommons;
import com.github.netfreer.shadowducks.common.handler.TransferHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author: landy
 * @date: 2017-04-11 19:47
 */
public class TcpServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(TcpServerHandler.class);
    private final AppConfig config;
    private Channel destChannel;

    public TcpServerHandler(AppConfig config) {
        this.config = config;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.read();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        if (destChannel == null) {
            ctx.read();
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext originalCtx, Object msg) throws Exception {
        if (destChannel == null) {
            ByteBuf buf = (ByteBuf) msg;
            final InetSocketAddress address = HandlerCommons.tryParseAddress(buf);
            if (address != null) {
                originalCtx.channel().config().setAutoRead(false);
                Bootstrap bootstrap = new Bootstrap()
                        .group(originalCtx.channel().eventLoop())
                        .channel(originalCtx.channel().getClass())
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getTimeout())
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(150 * 1024, 300 * 1024))
                        .handler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel ch) throws Exception {
                                ch.pipeline().addLast(new TransferHandler(originalCtx.channel(), "server"));
                                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelActive(ChannelHandlerContext destCtx) throws Exception {
                                        if (destCtx.channel().isWritable()) {
                                            originalCtx.channel().config().setAutoRead(true);
                                        }
                                        originalCtx.pipeline().remove(TcpServerHandler.this);
                                        originalCtx.pipeline().addLast(new TransferHandler(destCtx.channel(), "client"));
                                    }
                                });
                            }
                        });
                ChannelFuture connectFuture = bootstrap.connect(address);

                connectFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (!channelFuture.isSuccess()) {
                            LOG.warn("connect failure {}:{}, {} ", address.getHostName(), address.getPort(), channelFuture.cause().getMessage());
                            originalCtx.channel().close();
                        } else {
                            LOG.info("connect success {}:{}", address.getHostName(), address.getPort());
                        }
                    }
                });
                destChannel = connectFuture.channel();
                if (buf.isReadable()) { //process remain data
                    destChannel.write(buf);
                }
            }
        } else {
            destChannel.write(msg);
        }
    }


}
