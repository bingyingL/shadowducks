package test;

import org.junit.Test;

import java.net.InetSocketAddress;

/**
 * @author: landy
 * @date: 2017-04-11 20:00
 */
public class IPv6Test {
    @Test
    public void test1() throws Exception {
        InetSocketAddress address = new InetSocketAddress("2002:97b:e7aa::97b:e7aa", 80);
        System.out.println(address.getAddress().getHostAddress());
    }
}
