package com.github.netfreer.shadowducks.common.secret;

import io.netty.buffer.ByteBuf;

/**
 * @author: landy
 * @date: 2017-04-21 23:03
 */
public abstract class AbstractStreamCipher extends AbstractCipher {
    @Override
    public void translate(ByteBuf msg) {
        if (msg.isReadable()) {
            ByteBuf buf = msg;
            int i = buf.readerIndex();
            buf.markReaderIndex();
            byte[] tmp = new byte[buf.readableBytes()];
            buf.readBytes(tmp);
            tmp = process(tmp);
            buf.setBytes(i, tmp);
            buf.resetReaderIndex();
        }
    }

    abstract byte[] process(byte[] data);

}
