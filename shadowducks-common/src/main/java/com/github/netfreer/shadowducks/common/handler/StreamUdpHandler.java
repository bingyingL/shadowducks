package com.github.netfreer.shadowducks.common.handler;

import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.secret.AbstractStreamCipher;
import com.github.netfreer.shadowducks.common.utils.DucksFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author: landy
 * @date: 2017-04-23 09:04
 */
public class StreamUdpHandler extends ChannelDuplexHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StreamUdpHandler.class);
    private final PortContext portContext;

    public StreamUdpHandler(PortContext portContext) {
        this.portContext = portContext;


    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        try {
            AbstractStreamCipher decrypt = DucksFactory.getStreamCipher(portContext.getMethod());
            decrypt.init(false, portContext.getPassword());
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
        AbstractStreamCipher encrypt = DucksFactory.getStreamCipher(portContext.getMethod());
        encrypt.init(true, portContext.getPassword());
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf buf = packet.content();
        encrypt.translate(buf);
        ByteBuf preBuf = ctx.alloc().ioBuffer(encrypt.prefixSize());
        preBuf.writeBytes(encrypt.getPrefix());
        packet = packet.replace(Unpooled.wrappedBuffer(preBuf, buf));
        super.write(ctx, packet, promise);
    }
}