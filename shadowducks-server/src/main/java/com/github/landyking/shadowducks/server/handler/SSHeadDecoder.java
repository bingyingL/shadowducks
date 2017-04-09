package com.github.landyking.shadowducks.server.handler;

import com.github.landyking.shadowducks.server.dns.FakeDNSContainer;
import com.github.landyking.shadowducks.server.event.BackendConnectedEvent;
import com.github.landyking.shadowducks.server.pojo.SSHead;
import com.google.common.base.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.github.landyking.shadowducks.server.handler.SSHeadDecoder.State.head;


public class SSHeadDecoder extends ByteToMessageDecoder {
    enum State {
        head, connecting, connected,
    }
    private final FakeDNSContainer fakeDNSContainer;

    public SSHeadDecoder(FakeDNSContainer fakeDNSContainer) {
        this.fakeDNSContainer = fakeDNSContainer;
    }

    private State state = head;

    public static final int IPv4 = 1;
    public static final int IPv6 = 4;
    public static final int Domain = 3;
    public static final Logger LOGGER = LoggerFactory.getLogger(SSHeadDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        switch (state) {
            case head:
                parseHead(channelHandlerContext, byteBuf, list);
                break;
            case connecting:
                break;
            case connected:
//                ByteBuf tmp = byteBuf.readSlice(super.actualReadableBytes()).retain(2);
                ByteBuf tmp = byteBuf.readBytes(super.actualReadableBytes()).retain();
//                LOGGER.info("buf refCount {}", tmp.refCnt());
                list.add(tmp);
                break;
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof BackendConnectedEvent) {
            LOGGER.debug("receive backend connected event");
            state = State.connected;
            ctx.pipeline().fireChannelRead(Unpooled.EMPTY_BUFFER);
        }
    }

    private void parseHead(ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list) {
        buf.markReaderIndex();
        SSHead head = null;
        if (buf.readableBytes() > 3) {
            short addressType = buf.readUnsignedByte();
            if (addressType == IPv4) {
                if (buf.readableBytes() >= 6) {
                    String ip = buf.readUnsignedByte() + "." + buf.readUnsignedByte()
                            + "." + buf.readUnsignedByte() + "." + buf.readUnsignedByte();
                    int port = buf.readUnsignedShort();
                    String domainByFakeIp = fakeDNSContainer.getDomainByFakeIp(ip);
                    if (!Strings.isNullOrEmpty(domainByFakeIp)) {
                        head = new SSHead(Domain, domainByFakeIp, port);
                    }else{
                        head = new SSHead(IPv4, ip, port);
                    }
                }
            } else if (addressType == IPv6) {
                throw new IllegalArgumentException("not support IPv6 .....");
            } else if (addressType == Domain) {
                short len = buf.readUnsignedByte();
                if (buf.readableBytes() >= (len + 2)) {
                    byte[] tmp = new byte[len];
                    buf.readBytes(tmp);
                    String domain = new String(tmp, CharsetUtil.UTF_8);
                    int port = buf.readUnsignedShort();
                    head = new SSHead(Domain, domain, port);
                }
            }else{
                throw new IllegalArgumentException("unknown address type "+addressType);
            }
        }
        if (head != null) {
            list.add(head);
            state = State.connecting;
        } else {
            buf.resetReaderIndex();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("SSHead decoder error : {}",cause.getMessage(),cause);
        ctx.close();
    }
}
