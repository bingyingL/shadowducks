package com.github.netfreer.shadowducks.common.handler;

import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.secret.AbstractAEADCipher;
import com.github.netfreer.shadowducks.common.utils.DucksFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * @author: landy
 * @date: 2017-04-23 09:04
 */
public class AEADTcpHandler extends ByteToMessageCodec<ByteBuf> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AEADTcpHandler.class);
    private boolean writePrefix;
    private final AbstractAEADCipher encrypt;
    private final AbstractAEADCipher decrypt;
    private final int MAX_DATA_LEN = 0x3FFF;

    public AEADTcpHandler(PortContext portContext) {
        encrypt = DucksFactory.getAEADCipher(portContext.getMethod());
        encrypt.init(true, portContext.getPassword());
        decrypt = DucksFactory.getAEADCipher(portContext.getMethod());
        decrypt.init(false, portContext.getPassword());
        writePrefix = false;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (!writePrefix) {
            out.writeBytes(encrypt.getPrefix());
            writePrefix = true;
        }
        int total = msg.readableBytes();
        while (total > 0) {
            int actual;
            if (total > MAX_DATA_LEN) {
                actual = MAX_DATA_LEN;
            } else {
                actual = total;
            }
            encrypt.encryptLength(actual & MAX_DATA_LEN, out);
            encrypt.encryptPayload(msg, out, actual);

            total -= actual;
        }
    }

    enum State {
        init, len, payload
    }

    private State decryptState = State.init;
    private int payloadLength = 0;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (true) {
            switch (decryptState) {
                case init:
                    if (in.readableBytes() < decrypt.prefixSize()) {
                        return;
                    }
                    byte[] bs = new byte[decrypt.prefixSize()];
                    in.readBytes(bs);
                    decrypt.setPrefix(bs);
                    decryptState = State.len;
                    break;
                case len:
                    if (in.isReadable(2 + decrypt.tagSize())) {
                        payloadLength = decrypt.decryptLength(in);
                        if (payloadLength == 0) {
                            logger.warn("Read payload length is zero");
                        } else {
                            if (payloadLength > MAX_DATA_LEN) {
                                throw new IllegalStateException("Too big payload length: " + payloadLength);
                            }
                            decryptState = State.payload;
                        }
                    } else {
                        return;
                    }
                    break;
                case payload:
                    if (in.isReadable(payloadLength + decrypt.tagSize())) {
                        ByteBuf data = decrypt.decryptPayload(ctx, in, payloadLength);
                        int readableLen = data.readableBytes();
                        if (readableLen != payloadLength) {
                            throw new IllegalStateException("Decrypted data size:" + readableLen
                                    + " not equals to " + payloadLength);
                        }
                        out.add(data);
                        decryptState = State.len;
                        payloadLength = 0;
                    } else {
                        return;
                    }
                    break;
                default:

            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("AEAD cipher translate failure", cause);
        ctx.close();
    }
}
