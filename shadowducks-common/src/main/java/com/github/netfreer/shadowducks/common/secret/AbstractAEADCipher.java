package com.github.netfreer.shadowducks.common.secret;

import com.github.netfreer.shadowducks.common.utils.Tuple;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author: landy
 * @date: 2017-04-21 23:03
 */
public abstract class AbstractAEADCipher extends AbstractCipher {
    public static final String info = "ss-subkey";
    private int decryptNonce = 0;
    private int encryptNonce = 0;

    public int decryptLength(ByteBuf in) {
        byte[] data = new byte[2];
        byte[] tag = new byte[tagSize()];
        in.readBytes(data);
        in.readBytes(tag);
        byte[] newData = decrypt(decryptNonce++, data, tag);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(newData);
        int len = byteBuf.readUnsignedShort();
        byteBuf.release();
        return len;
    }

    @Override
    public int prefixSize() {
        return saltSize();
    }

    protected abstract int saltSize();

    protected abstract byte[] decrypt(int nonce, byte[] lenData, byte[] lenTag);

    protected abstract Tuple<byte[], byte[]> encrypt(int nonce, byte[] data);

    public ByteBuf decryptPayload(ChannelHandlerContext ctx, ByteBuf in, int payloadLength) {
        byte[] data = new byte[payloadLength];
        byte[] tag = new byte[tagSize()];
        in.readBytes(data);
        in.readBytes(tag);
        byte[] newData = decrypt(decryptNonce++, data, tag);
        ByteBuf ioBuffer = ctx.alloc().ioBuffer(newData.length);
        ioBuffer.writeBytes(newData);
        return ioBuffer;
    }

    public abstract int tagSize();

    public abstract int nonceSize();

    public void encryptLength(int actual, ByteBuf out) {
        Unpooled.buffer(2).writeShort(actual);
        byte[] data = new byte[2];
        data[0] = (byte) (actual >>> 8);
        data[1] = (byte) actual;
        Tuple<byte[], byte[]> encrypted = encrypt(encryptNonce++, data);
        out.writeBytes(encrypted.getFirst());
        out.writeBytes(encrypted.getSecond());
    }

    public void encryptPayload(ByteBuf msg, ByteBuf out, int actual) {
        byte[] data = new byte[actual];
        msg.readBytes(data);
        Tuple<byte[], byte[]> encrypted = encrypt(encryptNonce++, data);
        out.writeBytes(encrypted.getFirst());
        out.writeBytes(encrypted.getSecond());
    }
}
