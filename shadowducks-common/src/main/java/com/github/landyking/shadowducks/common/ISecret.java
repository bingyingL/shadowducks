package com.github.landyking.shadowducks.common;

/**
 * @author: landy
 * @date: 2016/05/18 19:50
 * note:
 */
public interface ISecret {

    int ivLength();

    int keySize();

    byte[] decrypt(byte[] tmp);

    byte[] decryptOnce(byte[] tmp);

    byte[] encrypt(byte[] tmp);

    byte[] encryptOnce(byte[] tmp);

    void setIV(byte[] iv) throws Exception;
}
