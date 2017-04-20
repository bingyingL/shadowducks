package com.github.netfreer.shadowducks.server.handler;

import com.github.netfreer.shadowducks.common.utils.AppConstans;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class UdpForwardHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    public static final Logger LOGGER = LoggerFactory.getLogger(UdpForwardHandler.class);
    private Map<String, Channel> src2channel = new HashMap<String, Channel>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        final ByteBuf buf = msg.content();
        InetSocketAddress destAddress = HandlerCommons.tryParseAddress(buf);
        final InetSocketAddress srcAddress = msg.sender();

        if (destAddress != null && buf.isReadable()) {
            final ByteBuf remain = buf.readBytes(buf.readableBytes());
            LOGGER.debug("Local port {} receive UDP packet from {}:{}, dest address is {}:{}",
                    msg.recipient().getPort(),
                    srcAddress.getAddress().getHostAddress(), srcAddress.getPort(),
                    destAddress.getAddress().getHostAddress(), destAddress.getPort());
            final InetSocketAddress finalDestAddress = new InetSocketAddress(destAddress.getHostName(), destAddress.getPort());
            if (src2channel.containsKey(srcAddress.toString())) {
                Channel channel = src2channel.get(srcAddress.toString());
                channel.writeAndFlush(new DatagramPacket(remain, finalDestAddress));
            } else {
                new Bootstrap().group(ctx.channel().eventLoop())
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            protected void initChannel(NioDatagramChannel ch) throws Exception {
                                ch.pipeline().addLast(new IdleStateHandler(0, 0, 60));
                                ch.pipeline().addLast(new InnerHandler(ctx.channel(), srcAddress));
                            }
                        })
                        .bind(0).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            src2channel.put(srcAddress.toString(), future.channel());
                            future.channel().writeAndFlush(new DatagramPacket(remain, finalDestAddress));
                        } else {
                            remain.release();
                        }
                    }
                });
            }
        }
    }


    private class InnerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private final Channel forwardChannel;
        private final InetSocketAddress originalAddress;

        public InnerHandler(Channel forwardChannel, InetSocketAddress originalAddress) {
            this.forwardChannel = forwardChannel;
            this.originalAddress = originalAddress;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            InetSocketAddress sender = msg.sender();
            InetAddress inetAddress = sender.getAddress();
            int addressType = -1;
            if (inetAddress instanceof Inet6Address) {
                addressType = AppConstans.IPv6;
            } else if (inetAddress instanceof Inet4Address) {
                addressType = AppConstans.IPv4;
            }

            ByteBuf buf = Unpooled.buffer().writeByte(addressType);
            if (addressType == AppConstans.Domain) {
                byte[] bytes = inetAddress.getHostAddress().getBytes(CharsetUtil.US_ASCII);
                buf.writeByte(bytes.length)
                        .writeBytes(bytes);
            } else if (addressType == AppConstans.IPv4) {
                buf.writeBytes(inetAddress.getAddress());
            } else if (addressType == AppConstans.IPv6) {
                buf.writeBytes(inetAddress.getAddress());
            }
            buf.writeShort(sender.getPort())
                    .writeBytes(msg.content());
            InetSocketAddress from = (InetSocketAddress) forwardChannel.localAddress();
            LOGGER.debug("Local port {} send UDP packet to {}:{}, original address is {}:{}",
                    from.getPort(),
                    originalAddress.getAddress().getHostAddress(), originalAddress.getPort(),
                    inetAddress.getHostAddress(), sender.getPort());
            forwardChannel.writeAndFlush(new DatagramPacket(buf, originalAddress));
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.close();
            } else {
                ctx.fireUserEventTriggered(evt);
            }
        }
    }
}
