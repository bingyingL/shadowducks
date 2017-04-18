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
    private final AbstractCipher encrypt;
    private final AbstractCipher decrypt;

    public UdpSecurityHandler(PortContext portContext) {
        encrypt = CipherFactory.getCipher(portContext.getMethod()).init(true, portContext.getPassword());
        decrypt = CipherFactory.getCipher(portContext.getMethod()).init(false, portContext.getPassword());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        try {
            ByteBuf buf = packet.content();
            byte[] prefix = new byte[decrypt.prefixSize()];
            buf.readBytes(prefix);
            buf.discardReadBytes();
            decrypt.setPrefix(prefix);
            translate(buf, decrypt);
            super.channelRead(ctx, msg);
        } catch (Exception e) {
            logger.warn("receive one invalid DatagramPacket from: {}", packet.sender().toString());
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    private void translate(ByteBuf buf, AbstractCipher cipher) {
        int i = buf.readerIndex();
        buf.markReaderIndex();
        byte[] tmp = new byte[buf.readableBytes()];
        buf.readBytes(tmp);
        tmp = cipher.process(tmp);
        buf.setBytes(i, tmp);
        buf.resetReaderIndex();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf buf = packet.content();
        translate(buf, encrypt);
        packet.replace(Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(encrypt.getPrefix()), buf));
        super.write(ctx, packet, promise);
    }
}
