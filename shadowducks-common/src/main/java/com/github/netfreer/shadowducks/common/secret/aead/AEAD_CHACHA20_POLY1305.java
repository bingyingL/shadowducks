package com.github.netfreer.shadowducks.common.secret.aead;

import com.github.netfreer.shadowducks.common.secret.AbstractAEADCipher;
import com.github.netfreer.shadowducks.common.utils.Tuple;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * @author: landy
 * @date: 2017-04-23 20:40
 */
public class AEAD_CHACHA20_POLY1305 extends AbstractAEADCipher {
    @Override
    protected int saltSize() {
        return 32;
    }

    @Override
    protected byte[] decrypt(int nonce, byte[] lenData, byte[] lenTag) {
        if (isEncrypt()) {
            throw new IllegalStateException("Not an decrypt cipher");
        }
        return new byte[0];
    }

    @Override
    protected Tuple<byte[], byte[]> encrypt(int nonce, byte[] data) {
        if (!isEncrypt()) {
            throw new IllegalStateException("Not an encrypt cipher");
        }
        return null;
    }

    @Override
    public int tagSize() {
        return 16;
    }

    @Override
    public int nonceSize() {
        return 12;
    }

    @Override
    protected void initCipher() {
        ChaCha7539Engine engine = new ChaCha7539Engine();
        byte[] key = null;
        engine.init(isEncrypt(), new ParametersWithIV(new KeyParameter(key), getPrefix()));
//        engine.processBytes()
    }

    @Override
    public int keySize() {
        return 32;
    }
}
