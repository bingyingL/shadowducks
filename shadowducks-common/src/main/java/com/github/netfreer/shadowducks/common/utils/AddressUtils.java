package com.github.netfreer.shadowducks.common.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Description：TODO <br/>
 *
 * @author: 黄地
 * @date: 2016/08/09 10:00
 * note:
 */
public class AddressUtils {
    public final static int FAKE_NETWORK_MASK=ipStringToInt("255.255.0.0");
    public final static int FAKE_NETWORK_IP=ipStringToInt("10.231.0.0");

    public static InetAddress ipIntToInet4Address(int ip){
        byte[] ipAddress=new byte[4];
        writeInt(ipAddress, 0, ip);
        try {
            return  Inet4Address.getByAddress(ipAddress);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public static String ipIntToString(int ip) {
        return String.format("%s.%s.%s.%s", (ip >> 24) & 0x00FF,
                (ip >> 16) & 0x00FF, (ip >> 8) & 0x00FF, ip & 0x00FF);
    }

    public static String ipBytesToString(byte[] ip) {
        return String.format("%s.%s.%s.%s", ip[0] & 0x00FF,ip[1] & 0x00FF,ip[2] & 0x00FF,ip[3] & 0x00FF);
    }

    public static int ipStringToInt(String ip) {
        String[] arrStrings = ip.split("\\.");
        int r = (Integer.parseInt(arrStrings[0]) << 24)
                | (Integer.parseInt(arrStrings[1]) << 16)
                | (Integer.parseInt(arrStrings[2]) << 8)
                | Integer.parseInt(arrStrings[3]);
        return r;
    }

    public static int readInt(byte[] data, int offset) {
        int r = ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
        return r;
    }

    public static short readShort(byte[] data, int offset) {
        int r = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        return (short) r;
    }

    public static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) (value);
    }

}
