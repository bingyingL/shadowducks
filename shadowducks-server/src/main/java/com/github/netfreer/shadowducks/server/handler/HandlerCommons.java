package com.github.netfreer.shadowducks.server.handler;

import com.github.netfreer.shadowducks.common.utils.AppConstans;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * @author: landy
 * @date: 2017-04-16 23:03
 */
public class HandlerCommons {
    public static InetSocketAddress tryParseAddress(ByteBuf buf) {
        buf.markReaderIndex();
        InetSocketAddress address = null;
        if (buf.readableBytes() > 3) {
            short addressType = buf.readUnsignedByte();
            if (addressType == AppConstans.IPv4) {
                if (buf.readableBytes() >= 6) {
                    String ip = buf.readUnsignedByte() + "." + buf.readUnsignedByte()
                            + "." + buf.readUnsignedByte() + "." + buf.readUnsignedByte();
                    int port = buf.readUnsignedShort();
                    address = new InetSocketAddress(ip, port);
                }
            } else if (addressType == AppConstans.IPv6) {
                if (buf.readableBytes() >= 16) {
                    String host = String.format("%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x",
                            buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                            buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                            buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                            buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                    int port = buf.readUnsignedShort();
                    address = new InetSocketAddress(host, port);
                }
            } else if (addressType == AppConstans.Domain) {
                short len = buf.readUnsignedByte();
                if (buf.readableBytes() >= (len + 2)) {
                    byte[] tmp = new byte[len];
                    buf.readBytes(tmp);
                    String domain = new String(tmp, CharsetUtil.UTF_8);
                    int port = buf.readUnsignedShort();
                    address = new InetSocketAddress(domain, port);
                }
            } else {
                throw new IllegalArgumentException("invalid address type " + addressType);
            }
        }
        if (address != null) {
            return address;
        } else {
            buf.resetReaderIndex();
            return null;
        }
    }
}
