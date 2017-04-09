package com.github.netfreer.shadowducks.common.handler;

import com.github.netfreer.shadowducks.common.ISecret;
import com.github.netfreer.shadowducks.common.config.PortContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;

public class UdpSecurityHandler extends ChannelDuplexHandler {
    private ISecret secret;

    public UdpSecurityHandler(PortContext portContext) {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        translate((DatagramPacket) msg, false);

        super.channelRead(ctx, msg);
    }

    private void translate(DatagramPacket msg, boolean encrypt) {
        ByteBuf buf = msg.content();
        int i = buf.readerIndex();
        buf.markReaderIndex();
        byte[] tmp = new byte[buf.readableBytes()];
        buf.readBytes(tmp);
        if (encrypt) {
            tmp = secret.encryptOnce(tmp);
        } else {
            tmp = secret.decryptOnce(tmp);
        }
        buf.setBytes(i, tmp);
        buf.resetReaderIndex();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        translate((DatagramPacket) msg, true);

        super.write(ctx, msg, promise);
    }
}
