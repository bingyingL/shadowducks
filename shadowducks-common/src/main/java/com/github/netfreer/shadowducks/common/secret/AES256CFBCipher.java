package com.github.netfreer.shadowducks.common.secret;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: landy
 * @date: 2017-04-10 23:30
 */
public class AES256CFBCipher extends AbstractCipher {
    private CFBBlockCipher ciphter;

    @Override
    public int prefixSize() {
        return 0;
    }

    @Override
    public int keySize() {
        return 0;
    }

    @Override
    public AbstractCipher init(boolean encrypt, String password) {
        super.init(encrypt, password);
        byte[] key = getKeyFromPass(password);
        AESEngine engine = new AESEngine();

        ciphter = new CFBBlockCipher(engine, prefixSize() * 8);
        //初始化，设置为解密模式
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(key), this.getPrefix());
        ciphter.init(encrypt, params);
        return this;
    }

    private byte[] getKeyFromPass(String password) {

        List<Byte[]> m = new ArrayList<Byte[]>();
        int i = 0;
        while (allSize(m) < (keySize() + prefixSize())) {
            Hasher md5 = Hashing.md5().newHasher();
            LinkedList<Byte> data = initData(password.getBytes());
            if (i > 0) {
                data = initData(m.get(i - 1));
                data.addAll(initData(password.getBytes()));
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

    private byte[] toByteArr(List<Byte> data) {
        byte[] tmp = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            tmp[i] = data.get(i);
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

    @Override
    public byte[] process(byte[] tmp) {
        byte[] out = new byte[tmp.length];
        ciphter.processBytes(tmp, 0, tmp.length, out, 0);
        return out;
    }
}
