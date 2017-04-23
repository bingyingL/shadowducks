package com.github.netfreer.shadowducks.common.handler;

import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.secret.AbstractCipher;
import com.github.netfreer.shadowducks.common.secret.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * @author: landy
 * @date: 2017-04-23 09:04
 */
public class AEADTcpHandler extends ByteToMessageCodec<ByteBuf> {
    private boolean writePrefix;
    private boolean readPrefix;
    private final AbstractCipher encrypt;
    private final AbstractCipher decrypt;

    public AEADTcpHandler(PortContext portContext) {
        encrypt = CipherFactory.getCipher(portContext.getMethod()).init(true, portContext.getPassword());
        decrypt = CipherFactory.getCipher(portContext.getMethod()).init(false, portContext.getPassword());
        writePrefix = false;
        readPrefix = false;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (!writePrefix) {
            out.writeBytes(encrypt.getPrefix());
            writePrefix = true;
        }
        encrypt.translate(msg);
        out.writeBytes(msg);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!readPrefix) {
            if (in.readableBytes() < decrypt.prefixSize()) {
                return;
            }
            byte[] bs = new byte[decrypt.prefixSize()];
            in.readBytes(bs);
            decrypt.setPrefix(bs);
            readPrefix = true;
        }
        int len = in.readableBytes();
        if (len > 0) {
            ByteBuf buf = in.readBytes(len);//copy new one
            decrypt.translate(buf);
            out.add(buf);
        }
    }
}
