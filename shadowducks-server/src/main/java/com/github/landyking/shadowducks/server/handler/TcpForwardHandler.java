package com.github.landyking.shadowducks.server.handler;

import com.github.landyking.shadowducks.server.event.BackendConnectedEvent;
import com.github.landyking.shadowducks.server.pojo.SSHead;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TcpForwardHandler extends SimpleChannelInboundHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(TcpForwardHandler.class);

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (ctx.channel().isActive()) {
            if (msg instanceof SSHead) {
                final SSHead headMsg = (SSHead) msg;
                LOGGER.info("will connect {}:{}", headMsg.getHost(), headMsg.getPort());
                Bootstrap bootstrap = new Bootstrap()
                        .group(ctx.channel().eventLoop())
                        .channel(ctx.channel().getClass())
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .handler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel ch) throws Exception {
//                                ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                                ch.pipeline().addLast(new RelayHandler(ctx.channel()));
                            }
                        });
                bootstrap.connect(headMsg.getHost(), headMsg.getPort()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (!channelFuture.isSuccess()) {
                            LOGGER.info("connect failure : {}:{}", headMsg.getHost(), headMsg.getPort());
                            ctx.channel().close();
                        } else {
//                            InetSocketAddress remoteAddress = (InetSocketAddress) channelFuture.channel().remoteAddress();
                            LOGGER.info("connect success : {}:{}", headMsg.getHost(),headMsg.getPort());
                           /* if (remoteAddress.getPort() != 53) {
                                ctx.pipeline().addLast(new HttpRecordHandler());
                            }*/
                            ctx.pipeline().addLast(new RelayHandler(channelFuture.channel()));
                            ctx.pipeline().fireUserEventTriggered(new BackendConnectedEvent());
                        }
                    }
                });
            } else {
                    /*LOGGER.info("forward msg : {}", msg.getClass().getSimpleName());
                    if (msg instanceof ByteBuf) {
                        LOGGER.info("buf refCount {}", ((ByteBuf) msg).refCnt());
                    }*/
                ctx.fireChannelRead(msg);
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }
}