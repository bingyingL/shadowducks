package com.github.landyking.shadowducks.common.handler;

import com.github.landyking.shadowducks.common.ISecret;
import com.github.landyking.shadowducks.common.config.PortContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class TcpSecurityHandler extends ChannelDuplexHandler {
    private ISecret secret;
    private byte[] iv;
    private int flag;

    public TcpSecurityHandler(PortContext portContext) {
        this.secret = secret;
        iv = new byte[secret.ivLength()];
        flag = secret.ivLength();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        while (flag > 0 && buf.isReadable()) {
            iv[secret.ivLength() - flag] = buf.readByte();
            flag--;
            if (flag == 0) {
                this.secret.setIV(iv);
            }
        }
        translate(buf, false);
        super.channelRead(ctx, msg);

    }

    private void translate(ByteBuf msg, boolean encrypt) {
        if (msg.isReadable()) {
            ByteBuf buf = msg;
            int i = buf.readerIndex();
            buf.markReaderIndex();
            byte[] tmp = new byte[buf.readableBytes()];
            buf.readBytes(tmp);
            if (encrypt) {
                tmp = secret.encrypt(tmp);
            } else {
                tmp = secret.decrypt(tmp);
            }
            buf.setBytes(i, tmp);
            buf.resetReaderIndex();
        }
    }

    private boolean writeIv = false;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!writeIv) {
            writeIv = true;
            ctx.write(Unpooled.wrappedBuffer(this.iv));
        }
        translate((ByteBuf) msg, true);
        super.write(ctx, msg, promise);
    }
}
