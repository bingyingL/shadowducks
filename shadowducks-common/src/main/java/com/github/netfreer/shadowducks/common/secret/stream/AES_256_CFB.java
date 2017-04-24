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


    @Override
    public byte[] process(byte[] tmp) {
        byte[] out = new byte[tmp.length];
        cipher.processBytes(tmp, 0, tmp.length, out, 0);
        return out;
    }
}
