package com.github.netfreer.shadowducks.common.utils;

import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.handler.AEADTcpHandler;
import com.github.netfreer.shadowducks.common.handler.AEADUdpHandler;
import com.github.netfreer.shadowducks.common.handler.StreamTcpHandler;
import com.github.netfreer.shadowducks.common.handler.StreamUdpHandler;
import com.github.netfreer.shadowducks.common.secret.*;
import com.github.netfreer.shadowducks.common.secret.stream.AES_256_CFB;
import com.google.common.base.Throwables;
import io.netty.channel.ChannelHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: landy
 * @date: 2017-04-10 23:26
 */
public class DucksFactory {
    public static final String STREAM_AES_128_CTR = "aes-128-ctr";
    public static final String STREAM_AES_192_CTR = "aes-192-ctr";
    public static final String STREAM_AES_256_CTR = "aes-256-ctr";
    public static final String STREAM_AES_128_CFB = "aes-128-cfb";
    public static final String STREAM_AES_192_CFB = "aes-192-cfb";
    public static final String STREAM_AES_256_CFB = "aes-256-cfb";
    public static final String STREAM_CAMELLIA_128_CFB = "camellia-128-cfb";
    public static final String STREAM_CAMELLIA_192_CFB = "camellia-192-cfb";
    public static final String STREAM_CAMELLIA_256_CFB = "camellia-256-cfb";
    public static final String STREAM_CHACHA20_IETF = "chacha20-ietf";

    public static final String AEAD_CHACHA20_POLY1305 = "chacha20-ietf-poly1305";
    public static final String AEAD_AES_128_GCM = "aes-128-gcm";
    public static final String AEAD_AES_192_GCM = "aes-192-gcm";
    public static final String AEAD_AES_256_GCM = "aes-256-gcm";

    private static List<String> STREAM_CIPHER_LIST = Arrays.asList(
            STREAM_AES_128_CFB, STREAM_AES_192_CFB, STREAM_AES_256_CFB,
            STREAM_AES_128_CTR, STREAM_AES_192_CTR, STREAM_AES_256_CTR,
            STREAM_CAMELLIA_128_CFB, STREAM_CAMELLIA_192_CFB, STREAM_CAMELLIA_256_CFB,
            STREAM_CHACHA20_IETF
    );
    private static List<String> AEAD_CIPHER_LIST = Arrays.asList(
            AEAD_AES_128_GCM, AEAD_AES_192_GCM, AEAD_AES_256_GCM, AEAD_CHACHA20_POLY1305
    );
    private static Map<String, Class<? extends AbstractCipher>> cipherClassMap = new HashMap<String, Class<? extends AbstractCipher>>();

    static {
        cipherClassMap.put(STREAM_AES_256_CFB, AES_256_CFB.class);
        cipherClassMap.put(AEAD_CHACHA20_POLY1305, com.github.netfreer.shadowducks.common.secret.aead.AEAD_CHACHA20_POLY1305.class);
    }

    public static AbstractStreamCipher getStreamCipher(String method) {
        return (AbstractStreamCipher) getCipher(method);
    }

    public static AbstractAEADCipher getAEADCipher(String method) {
        return (AbstractAEADCipher) getCipher(method);
    }

    private static AbstractCipher getCipher(String method) {
        Class<? extends AbstractCipher> clazz = cipherClassMap.get(method.toLowerCase());
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        throw new IllegalArgumentException("Unsupport method: " + method);
    }


    public static ChannelHandler getChannelHandler(PortContext portContext, boolean tcp) {
        if (isStream(portContext.getMethod())) {
            if (tcp) {
                return new StreamTcpHandler(portContext);
            } else {
                return new StreamUdpHandler(portContext);
            }
        } else if (isAEAD(portContext.getMethod())) {
            if (tcp) {
                return new AEADTcpHandler(portContext);
            } else {
                return new AEADUdpHandler(portContext);
            }
        } else {
            throw new IllegalArgumentException("Unsupport method: " + portContext.getMethod());
        }
    }

    private static boolean isAEAD(String method) {
        return AEAD_CIPHER_LIST.contains(method.toLowerCase());
    }

    private static boolean isStream(String method) {
        return STREAM_CIPHER_LIST.contains(method.toLowerCase());
    }
}
