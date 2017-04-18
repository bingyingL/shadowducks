package test;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * @author: landy
 * @date: 2017-04-11 20:00
 */
public class InetAddressTest {
    @Test
    public void test1() throws Exception {
        InetSocketAddress address = new InetSocketAddress("2002:97b:e7aa::97b:e7aa", 80);
        System.out.println(address.getAddress().getHostAddress());
    }

    @Test
    public void test2() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[]{1, 1, 1, 1});
        System.out.println(address.getHostAddress());

    }
    @Test
    public void test3() throws Exception {
        InetAddress address = InetAddress.getByName("192.168.22.29");
        System.out.println(Arrays.toString(address.getAddress()));
    }
}
