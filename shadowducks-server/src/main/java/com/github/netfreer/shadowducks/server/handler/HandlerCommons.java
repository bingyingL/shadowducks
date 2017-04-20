package com.github.netfreer.shadowducks.server.handler;

import com.github.netfreer.shadowducks.common.utils.AppConstans;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @author: landy
 * @date: 2017-04-16 23:03
 */
public class HandlerCommons {
    public static InetSocketAddress tryParseAddress(ByteBuf buf) throws UnknownHostException {
        buf.markReaderIndex();
        InetSocketAddress address = null;
        if (buf.readableBytes() > 3) {
            short addressType = buf.readUnsignedByte();
            if (addressType == AppConstans.IPv4) {
                if (buf.readableBytes() >= 6) {
                    byte[] data = new byte[4];
                    buf.readBytes(data);
                    InetAddress inetAddress = InetAddress.getByAddress(data);
                    int port = buf.readUnsignedShort();
                    address = new InetSocketAddress(inetAddress, port);
                }
            } else if (addressType == AppConstans.IPv6) {
                if (buf.readableBytes() >= 18) {
                    byte[] data = new byte[16];
                    buf.readBytes(data);
                    InetAddress inetAddress = InetAddress.getByAddress(data);
                    int port = buf.readUnsignedShort();
                    address = new InetSocketAddress(inetAddress, port);
                }
            } else if (addressType == AppConstans.Domain) {
                short len = buf.readUnsignedByte();
                if (buf.readableBytes() >= (len + 2)) {
                    String domain = buf.toString(buf.readerIndex(), len, CharsetUtil.UTF_8);
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
