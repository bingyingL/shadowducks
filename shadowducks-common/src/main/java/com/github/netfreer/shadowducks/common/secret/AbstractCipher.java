package com.github.netfreer.shadowducks.common.secret;

import com.google.common.base.Throwables;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * @author: landy
 * @date: 2017-04-10 23:16
 */
public abstract class AbstractCipher {
    private byte[] prefix;
    private boolean encrypt;
    private String password;
    private final static SecureRandom randomGenerator = new SecureRandom();

    public final AbstractCipher init(boolean encrypt, String password) {
        this.encrypt = encrypt;
        this.password = password;
        if (encrypt) {
            this.setPrefix(newPrefix());
        }
        return this;
    }

    protected abstract void initCipher();

    public boolean isEncrypt() {
        return encrypt;
    }

    public String getPassword() {
        return password;
    }

    private byte[] newPrefix() {
        byte[] bytes = new byte[prefixSize()];
        randomGenerator.nextBytes(bytes);
        return bytes;
    }

    public static byte[] getKeyFromPass(int keyLength, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int length = (keyLength + 15) / 16 * 16;
            byte[] passwordBytes = password.getBytes("UTF-8");
            byte[] temp = digest.digest(passwordBytes);
            byte[] key = Arrays.copyOf(temp, length);
            for (int i = 1; i < length / 16; i++) {
                temp = Arrays.copyOf(temp, 16 + passwordBytes.length);
                System.arraycopy(passwordBytes, 0, temp, 16, passwordBytes.length);
                System.arraycopy(digest.digest(temp), 0, key, i * 16, 16);
            }
            return Arrays.copyOf(key, keyLength);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return new byte[keyLength];
    }

    public abstract int prefixSize();

    public abstract int keySize();


    public final void setPrefix(byte[] prefix) {
        if (this.prefix != null) {
            throw new IllegalStateException("prefix already set!");
        }
        this.prefix = prefix;
        initCipher();
    }

    public byte[] getPrefix() {
        return prefix;
    }
}
