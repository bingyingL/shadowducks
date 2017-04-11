package com.github.netfreer.shadowducks.common.secret;

import java.security.SecureRandom;

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

    public abstract int prefixSize();

    public abstract int keySize();

    public abstract byte[] process(byte[] data);

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
