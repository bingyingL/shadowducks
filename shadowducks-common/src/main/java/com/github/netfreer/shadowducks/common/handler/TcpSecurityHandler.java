package com.github.netfreer.shadowducks.common.handler;

import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.secret.AbstractCipher;
import com.github.netfreer.shadowducks.common.secret.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class TcpSecurityHandler extends ChannelDuplexHandler {
    private final AbstractCipher encrypt;
    private final AbstractCipher decrypt;
    private final byte[] prefix;
    private int mark;

    public TcpSecurityHandler(PortContext portContext) {
        encrypt = CipherFactory.getCipher(portContext.getMethod()).init(true, portContext.getPassword());
        decrypt = CipherFactory.getCipher(portContext.getMethod()).init(false, portContext.getPassword());
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
            writePrefix = true;
            ctx.write(Unpooled.wrappedBuffer(encrypt.getPrefix()));
        }
        encrypt.translate((ByteBuf) msg);
        super.write(ctx, msg, promise);
    }
}
