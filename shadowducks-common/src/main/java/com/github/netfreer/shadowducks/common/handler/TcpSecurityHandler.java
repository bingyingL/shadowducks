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

    public TcpSecurityHandler(PortContext portContext) {
        encrypt = CipherFactory.getCipher(portContext.getMethod()).init(true, portContext.getPassword());
        decrypt = CipherFactory.getCipher(portContext.getMethod()).init(false, portContext.getPassword());
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        if (buf.readableBytes() >= decrypt.prefixSize()) {
            byte[] prefix = new byte[decrypt.prefixSize()];
            buf.readBytes(prefix);
            decrypt.setPrefix(prefix);
        }
        translate(buf, decrypt);
        super.channelRead(ctx, msg);

    }

    private void translate(ByteBuf msg, AbstractCipher cipher) {
        if (msg.isReadable()) {
            ByteBuf buf = msg;
            int i = buf.readerIndex();
            buf.markReaderIndex();
            byte[] tmp = new byte[buf.readableBytes()];
            buf.readBytes(tmp);
            tmp = cipher.process(tmp);
            buf.setBytes(i, tmp);
            buf.resetReaderIndex();
        }
    }

    private boolean writeIv = false;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!writeIv) {
            writeIv = true;
            ctx.write(Unpooled.wrappedBuffer(encrypt.getPrefix()));
        }
        translate((ByteBuf) msg, encrypt);
        super.write(ctx, msg, promise);
    }
}
