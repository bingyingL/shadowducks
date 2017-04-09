package com.github.netfreer.shadowducks.common.secret;

import com.github.netfreer.shadowducks.common.ISecret;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AESSecret implements ISecret {
    public static final String KEY_ALGORITHM = "AES";
    public static final String CIPHER_ALGORITHM = "AES/CFB/NOPADDING";
    private SecretKey key;
    private final String password;
    private byte[] iv;
    private StreamBlockCipher decryptCipher;
    private StreamBlockCipher encryptCipher;

    public AESSecret(String password) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        this.password = password;
    }

    private byte[] getKeyFromPass() throws NoSuchAlgorithmException, NoSuchProviderException {

        List<Byte[]> m = new ArrayList<Byte[]>();
        int i = 0;
        while (allSize(m) < (keySize() + ivLength())) {
            Hasher md5 = Hashing.md5().newHasher();
            LinkedList<Byte> data = initData(this.password.getBytes());
            if (i > 0) {
                data = initData(m.get(i - 1));
                data.addAll(initData(this.password.getBytes()));
            }
            md5.putBytes(toByteArr(data));
            byte[] bytes = md5.hash().asBytes();
            printArr("md5", bytes);
            m.add(toBig(bytes));
            i++;
        }
        List<Byte> tmp = new ArrayList<Byte>();
        for (Byte[] bytes : m) {
            for (Byte aByte : bytes) {
                tmp.add(aByte);
            }
        }
        byte[] encoded = toByteArr(tmp.subList(0, keySize()));

        printArr("key", encoded);
        return encoded;
    }

    private int allSize(List<Byte[]> m) {
        int total = 0;
        for (Byte[] bytes : m) {
            total += bytes.length;
        }
        return total;
    }

    private Byte[] toBig(byte[] bytes) {
        Byte[] tmp = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            tmp[i] = bytes[i];
        }
        return tmp;
    }

    private void printArr(String name, byte[] encoded) {
        System.out.print(name + " :");
        for (byte b : encoded) {
            System.out.printf(" %x", b);
        }
        System.out.println();
    }

    private byte[] toByteArr(List<Byte> data) {
        byte[] tmp = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            tmp[i] = data.get(i);
        }
        return tmp;
    }

    private LinkedList<Byte> initData(byte[] bytes) {
        LinkedList<Byte> tmp = new LinkedList<Byte>();
        for (byte aByte : bytes) {
            tmp.add(aByte);
        }
        return tmp;
    }

    private LinkedList<Byte> initData(Byte[] bytes) {
        LinkedList<Byte> tmp = new LinkedList<Byte>();
        for (byte aByte : bytes) {
            tmp.add(aByte);
        }
        return tmp;
    }

    @Override
    public int ivLength() {
        return 16;
    }

    @Override
    public int keySize() {
        return 32;
    }

    @Override
    public byte[] decrypt(byte[] tmp) {
        byte[] out = new byte[tmp.length];
        decryptCipher.processBytes(tmp, 0, tmp.length, out, 0);
        return out;
    }

    @Override
    public byte[] decryptOnce(byte[] tmp) {
        return decrypt(tmp);
    }

    @Override
    public byte[] encrypt(byte[] tmp) {
        byte[] out = new byte[tmp.length];
        encryptCipher.processBytes(tmp, 0, tmp.length, out, 0);
        return out;
    }

    @Override
    public byte[] encryptOnce(byte[] tmp) {
        return encrypt(tmp);
    }

    @Override
    public void setIV(byte[] iv) throws Exception {
        this.iv = iv;
        System.out.print("set iv :");
        for (byte b : iv) {
            System.out.printf(" %x", b);
        }
        System.out.println();
        byte[] encoded = getKeyFromPass();
        initEncrypt(encoded);
        initDecrypt(encoded);
    }

    private void initDecrypt(byte[] encoded) throws Exception {
        AESEngine engine = new AESEngine();

        decryptCipher = new CFBBlockCipher(engine, ivLength() * 8);
        //初始化，设置为解密模式
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(encoded), this.iv);
        decryptCipher.init(false, params);
    }

    private void initEncrypt(byte[] encoded) throws Exception {
        AESEngine engine = new AESEngine();

        encryptCipher = new CFBBlockCipher(engine, ivLength() * 8);
        //初始化，设置为解密模式
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(encoded), this.iv);
        encryptCipher.init(true, params);
    }

}
