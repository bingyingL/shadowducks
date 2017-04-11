package com.github.netfreer.shadowducks.server.handler;

import com.github.netfreer.shadowducks.common.utils.AppConstans;
import com.github.netfreer.shadowducks.server.dns.DNSServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class UdpForwardHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    public static final Logger LOGGER = LoggerFactory.getLogger(UdpForwardHandler.class);
    private static final String googleDnsServer = "8.8.8.8";

    private static final int googleDnsPort = 53;
    private Map<String, Channel> src2channel = new HashMap<String, Channel>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        final ByteBuf buf = msg.content();
        final InetSocketAddress srcAddress = msg.sender();
        InetSocketAddress destAddress = null;
        if (buf.readableBytes() > 3) {
            short addressType = buf.readUnsignedByte();
            if (addressType == AppConstans.IPv4) {
                if (buf.readableBytes() >= 6) {
                    String ip = buf.readUnsignedByte() + "." + buf.readUnsignedByte()
                            + "." + buf.readUnsignedByte() + "." + buf.readUnsignedByte();
                    int port = buf.readUnsignedShort();
                    destAddress = new InetSocketAddress(ip, port);
                }
            } else if (addressType == AppConstans.IPv6) {
                LOGGER.info("不支持IPv6 .....");
                return;
            } else if (addressType == AppConstans.Domain) {
                short len = buf.readUnsignedByte();
                if (buf.readableBytes() >= (len + 2)) {
                    byte[] tmp = new byte[len];
                    buf.readBytes(tmp);
                    String domain = new String(tmp, CharsetUtil.UTF_8);
                    int port = buf.readUnsignedShort();
                    destAddress = new InetSocketAddress(domain, port);
                }
            }
        }
        if (destAddress != null && buf.isReadable()) {
            final ByteBuf remain = buf.readBytes(buf.readableBytes());
            LOGGER.debug("{}:{} 接收到来自 {}:{} 的UDP包, 该包的目的地址是 {}:{}",
                    msg.recipient().getHostName(), msg.recipient().getPort(),
                    srcAddress.getHostName(), srcAddress.getPort(),
                    destAddress.getHostName(), destAddress.getPort());
            final InetSocketAddress finalDestAddress = new InetSocketAddress(destAddress.getHostName(), destAddress.getPort());
            InetSocketAddress tmpAddress;
            if (destAddress.getHostName().equals(googleDnsServer) && destAddress.getPort() == googleDnsPort) {
                LOGGER.debug("实际目的地址由{}:{}替换为{}:{}", googleDnsServer, googleDnsPort, DNSServer.dnsServer, DNSServer.dnsPort);
                tmpAddress = new InetSocketAddress(DNSServer.dnsServer, DNSServer.dnsPort);
            } else {
                tmpAddress = finalDestAddress;
            }
            final InetSocketAddress actualDestAddress = tmpAddress;
            if (src2channel.containsKey(srcAddress.toString())) {
                Channel channel = src2channel.get(srcAddress.toString());
                channel.writeAndFlush(new DatagramPacket(remain, actualDestAddress));
            } else {
                final InetSocketAddress finalDestAddress1 = destAddress;
                new Bootstrap().group(ctx.channel().eventLoop())
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            protected void initChannel(NioDatagramChannel ch) throws Exception {
                                ch.pipeline().addLast(new IdleStateHandler(0, 0, 60));
                                ch.pipeline().addLast(new InnerHandler(srcAddress, finalDestAddress1, ctx.channel()));
                            }
                        })
                        .bind(0).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            src2channel.put(srcAddress.toString(), future.channel());
                            future.channel().writeAndFlush(new DatagramPacket(remain, actualDestAddress));
//                            LOGGER.debug("server-> udp from {} to {}", future.channel().localAddress(), finalDestAddress);
                        }
                    }
                });
            }
        }
    }


    private class InnerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private final Channel forwardChannel;
        private final InetSocketAddress srcAddress;
        private final InetSocketAddress destAddress;

        public InnerHandler(InetSocketAddress srcAddress, InetSocketAddress destAddress, Channel forwardChannel) {
            this.srcAddress = srcAddress;
            this.destAddress = destAddress;
            this.forwardChannel = forwardChannel;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            InetSocketAddress sender = msg.sender();
            LOGGER.debug("收到来自{}:{}的UDP回复包,理论地址为{}:{}", sender.getHostName(), sender.getPort(), destAddress.getHostName(), destAddress.getPort());

/*
            ByteBuf buf = Unpooled.buffer().writeByte(destAddress.getType());
            if (destAddress.getType() == AppConstans.Domain) {
                byte[] bytes = destAddress.getHostName().getBytes(CharsetUtil.US_ASCII);
                buf.writeByte(bytes.length)
                        .writeBytes(bytes);
            } else if (destAddress.getType() == AppConstans.IPv4) {
                String[] arr = destAddress.getHost().split("\\.");
                for (String one : arr) {
                    int i = Integer.parseInt(one);
                    buf.writeByte(i);
                }
            } else if (destAddress.getType() == AppConstans.IPv6) {
                LOGGER.info("not support IPv6 .....");
                ctx.close();
                return;
            }
            buf.writeShort(destAddress.getPort())
                    .writeBytes(msg.content());
            InetSocketAddress from = (InetSocketAddress) forwardChannel.localAddress();
            LOGGER.debug("从{}:{}向{}:{}发送报文,标记该报文来自 {}:{}", from.getHostName(), from.getPort(), srcAddress.getHostName(), srcAddress.getPort(), destAddress.getHost(), destAddress.getPort());
            forwardChannel.writeAndFlush(new DatagramPacket(buf, srcAddress));*/
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
