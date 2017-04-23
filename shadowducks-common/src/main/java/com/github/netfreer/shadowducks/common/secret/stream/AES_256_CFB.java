package com.github.netfreer.shadowducks.common.secret.stream;

import com.github.netfreer.shadowducks.common.secret.AbstractStreamCipher;
import com.google.common.base.Throwables;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * @author: landy
 * @date: 2017-04-10 23:30
 */
public class AES_256_CFB extends AbstractStreamCipher {
    private CFBBlockCipher cipher;

    @Override
    protected int ivLength() {
        return 16;
    }

    @Override
    public int keySize() {
        return 32;
    }

    @Override
    protected void initCipher() {
        AESEngine engine = new AESEngine();
        cipher = new CFBBlockCipher(engine, prefixSize() * 8);
        //初始化，设置为解密模式
        byte[] key = getKeyFromPass(keySize(), getPassword());
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(key), this.getPrefix());
        cipher.init(isEncrypt(), params);
    }

    private byte[] getKeyFromPass(int keyLength, String password) {
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


    @Override
    public byte[] process(byte[] tmp) {
        byte[] out = new byte[tmp.length];
        cipher.processBytes(tmp, 0, tmp.length, out, 0);
        return out;
    }
}
