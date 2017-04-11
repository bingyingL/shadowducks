package com.github.netfreer.shadowducks.common.secret;

import com.google.common.base.Throwables;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: landy
 * @date: 2017-04-10 23:26
 */
public class CipherFactory {
    private static Map<String, Class<? extends AbstractCipher>> classMap = new HashMap<String, Class<? extends AbstractCipher>>();

    static {
        classMap.put("aes-256-cfb".toUpperCase(), AES256CFBCipher.class);
    }

    public static AbstractCipher getCipher(String method) {
        Class<? extends AbstractCipher> clazz = classMap.get(method.toUpperCase());
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        return null;
    }
}
