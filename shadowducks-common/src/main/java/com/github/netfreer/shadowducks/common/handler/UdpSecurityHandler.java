package com.github.netfreer.shadowducks.common.handler;

import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.secret.AbstractCipher;
import com.github.netfreer.shadowducks.common.secret.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class UdpSecurityHandler extends ChannelDuplexHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UdpSecurityHandler.class);
    private final PortContext portContext;

    public UdpSecurityHandler(PortContext portContext) {
        this.portContext = portContext;


    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        try {
            AbstractCipher decrypt = CipherFactory.getCipher(portContext.getMethod()).init(false, portContext.getPassword());
            ByteBuf buf = packet.content();
            byte[] prefix = new byte[decrypt.prefixSize()];
            buf.readBytes(prefix);
            buf.discardReadBytes();
            decrypt.setPrefix(prefix);
            decrypt.translate(buf);
            super.channelRead(ctx, msg);
        } catch (Exception e) {
            logger.warn("receive one invalid DatagramPacket from: {}", packet.sender().toString(), e);
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        AbstractCipher encrypt = CipherFactory.getCipher(portContext.getMethod()).init(true, portContext.getPassword());
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf buf = packet.content();
        encrypt.translate(buf);
        packet = packet.replace(Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(encrypt.getPrefix()), buf));
        super.write(ctx, packet, promise);
    }
}
