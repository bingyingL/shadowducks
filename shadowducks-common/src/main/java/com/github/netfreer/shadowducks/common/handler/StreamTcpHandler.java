package com.github.netfreer.shadowducks.common.handler;

import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.secret.AbstractStreamCipher;
import com.github.netfreer.shadowducks.common.utils.DucksFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class StreamTcpHandler extends ChannelDuplexHandler {
    private final AbstractStreamCipher encrypt;
    private final AbstractStreamCipher decrypt;
    private final byte[] prefix;
    private int mark;

    public StreamTcpHandler(PortContext portContext) {
        encrypt = DucksFactory.getStreamCipher(portContext.getMethod());
        encrypt.init(true, portContext.getPassword());
        decrypt = DucksFactory.getStreamCipher(portContext.getMethod());
        decrypt.init(false, portContext.getPassword());
        prefix = new byte[decrypt.prefixSize()];
        mark = 0;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        while (mark < decrypt.prefixSize() && buf.isReadable()) {
            prefix[mark] = buf.readByte();
            mark++;
            if (mark == decrypt.prefixSize()) {
                decrypt.setPrefix(prefix);
            }
        }
        if (buf.isReadable()) {
            decrypt.translate(buf);
            if (buf.isReadable()) {
                super.channelRead(ctx, msg);
            }
        }

    }

    private boolean writePrefix = false;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!writePrefix) {
            ByteBuf buf = ctx.alloc().ioBuffer(encrypt.prefixSize());
            buf.writeBytes(encrypt.getPrefix());
            ctx.write(buf);
            writePrefix = true;
        }
        encrypt.translate((ByteBuf) msg);
        super.write(ctx, msg, promise);
    }
}
