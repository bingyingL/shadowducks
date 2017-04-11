package test;

import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;

/**
 * @author: landy
 * @date: 2017-04-11 21:29
 */
public class CipherTest {
    @Test
    public void test1() throws Exception {
        SecretKeySpec aes = new SecretKeySpec("password".getBytes(), "AES");
        System.out.println(new String(aes.getEncoded()));

    }
}
