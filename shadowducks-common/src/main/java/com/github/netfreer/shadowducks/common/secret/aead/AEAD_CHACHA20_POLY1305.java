package com.github.netfreer.shadowducks.common.secret.aead;

import com.github.netfreer.shadowducks.common.secret.AbstractAEADCipher;
import com.github.netfreer.shadowducks.common.utils.Tuple;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.tls.TlsUtils;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

/**
 * @author: landy
 * @date: 2017-04-23 20:40
 */
public class AEAD_CHACHA20_POLY1305 extends AbstractAEADCipher {
    private static final byte[] ZEROES = new byte[15];
    private ChaCha7539Engine cipher;
    private KeyParameter keyParameter;

    @Override
    protected int saltSize() {
        return 32;
    }

    @Override
    protected byte[] decrypt(int nonce, byte[] data, byte[] tag) {
        if (isEncrypt()) {
            throw new IllegalStateException("Not an decrypt cipher");
        }
        KeyParameter macKey = initRecord(cipher, false, nonce, getPrefix());
        byte[] calculatedMAC = calculateRecordMAC(macKey, data, 0, data.length);
        if (!Arrays.constantTimeAreEqual(calculatedMAC, tag)) {
            throw new IllegalStateException("Decrypt failure: tag error!");
        }
        byte[] nData = new byte[data.length];
        cipher.processBytes(data, 0, data.length, nData, 0);
        return nData;
    }

    @Override
    protected Tuple<byte[], byte[]> encrypt(int nonce, byte[] data) {
        if (!isEncrypt()) {
            throw new IllegalStateException("Not an encrypt cipher");
        }
        KeyParameter macKey = initRecord(cipher, true, nonce, getPrefix());
        byte[] nData = new byte[data.length];
        cipher.processBytes(data, 0, data.length, nData, 0);
        byte[] tag = calculateRecordMAC(macKey, nData, 0, data.length);
        return Tuple.newOne(nData, tag);
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
        cipher = new ChaCha7539Engine();
        byte[] pass = getKeyFromPass(keySize(), getPassword());

        byte[] okm = new byte[keySize()];
        SHA1Digest digest = new SHA1Digest();
        HKDFParameters params = new HKDFParameters(pass, getPrefix(), INFO.getBytes());
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(digest);
        hkdf.init(params);
        hkdf.generateBytes(okm, 0, keySize());
        keyParameter = new KeyParameter(okm);
    }

    @Override
    public int keySize() {
        return 32;
    }

    protected KeyParameter initRecord(StreamCipher cipher, boolean forEncryption, long seqNo, byte[] iv) {
        byte[] nonce = calculateNonce(seqNo, iv);
        cipher.init(forEncryption, new ParametersWithIV(keyParameter, nonce));
        return generateRecordMACKey(cipher);
    }

    protected byte[] calculateNonce(long seqNo, byte[] iv) {
        byte[] nonce = new byte[12];
        TlsUtils.writeUint64(seqNo, nonce, 4);

        for (int i = 0; i < 12; ++i) {
            nonce[i] ^= iv[i];
        }

        return nonce;
    }

    protected KeyParameter generateRecordMACKey(StreamCipher cipher) {
        byte[] firstBlock = new byte[64];
        cipher.processBytes(firstBlock, 0, firstBlock.length, firstBlock, 0);

        KeyParameter macKey = new KeyParameter(firstBlock, 0, 32);
        Arrays.fill(firstBlock, (byte) 0);
        return macKey;
    }

    protected byte[] calculateRecordMAC(KeyParameter macKey, byte[] buf, int off, int len) {
        Mac mac = new Poly1305();
        mac.init(macKey);

        updateRecordMACText(mac, buf, off, len);
        updateRecordMACLength(mac, len);

        byte[] output = new byte[mac.getMacSize()];
        mac.doFinal(output, 0);
        return output;
    }

    protected void updateRecordMACLength(Mac mac, int len) {
        byte[] longLen = Pack.longToLittleEndian(len & 0xFFFFFFFFL);
        mac.update(longLen, 0, longLen.length);
    }

    protected void updateRecordMACText(Mac mac, byte[] buf, int off, int len) {
        mac.update(buf, off, len);

        int partial = len % 16;
        if (partial != 0) {
            mac.update(ZEROES, 0, 16 - partial);
        }
    }
}
