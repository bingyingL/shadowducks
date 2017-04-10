package com.github.netfreer.shadowducks.common.secret;

/**
 * @author: landy
 * @date: 2017-04-10 23:16
 */
public abstract class AbstractCipher {
    private byte[] prefix;
    private boolean encrypt;

    public AbstractCipher init(boolean encrypt, String password) {
        this.encrypt = encrypt;
        if (encrypt) {
            this.prefix = newPrefix();
        }
        return this;
    }

    private byte[] newPrefix() {
        return new byte[0];
    }

    public abstract int prefixSize();

    public abstract int keySize();

    public abstract byte[] process(byte[] data);

    public void setPrefix(byte[] prefix) {
        this.prefix = prefix;
    }


    public byte[] getPrefix() {
        return prefix;
    }
}
