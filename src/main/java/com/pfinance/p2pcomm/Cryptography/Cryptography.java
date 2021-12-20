/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Cryptography;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.spongycastle.util.encoders.Hex;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

/**
 *
 * @author averypozzobon
 */
public class Cryptography {
    
    private static final byte[] salt = {
        (byte) 0x43, (byte) 0x76, (byte) 0x95, (byte) 0xc7,
        (byte) 0x5b, (byte) 0xd7, (byte) 0x45, (byte) 0x17 
    };
    
    public static byte[] sign(byte[] msg, ECKeyPair pair) {
        byte[] msgHash = Hash.sha256(msg);
        Sign.SignatureData signed = Sign.signMessage(msgHash, pair, false);
        byte[] signature = new byte[signed.getR().length + signed.getS().length + signed.getV().length];
        System.arraycopy(signed.getR(), 0, signature, 0, signed.getR().length);
        System.arraycopy(signed.getS(), 0, signature, signed.getR().length, signed.getS().length);
        System.arraycopy(signed.getV(), 0, signature, signed.getR().length + signed.getS().length, signed.getV().length);
        return signature;
    }
    
    public static boolean verify(byte[] signature, byte[] msg, BigInteger pubKey) {
        byte[] v = new byte[1];
        System.arraycopy(signature, signature.length-1, v, 0, 1);
        byte[] r = new byte[32];
        System.arraycopy(signature, 0, r, 0, 32);
        byte[] s = new byte[32];
        System.arraycopy(signature, 32, s, 0, 32);
        Sign.SignatureData signed = new Sign.SignatureData(v, r, s);
        BigInteger pubKeyRecovered;
        try {
            byte[] msgHash = Hash.sha256(msg);
            pubKeyRecovered = Sign.signedMessageHashToKey(msgHash, signed);
            //System.out.println("Recovered: " + Hex.toHexString(pubKeyRecovered.toByteArray()));
            boolean validSig = pubKey.equals(pubKeyRecovered);
            return validSig;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static Cipher makeCipher(String pass, Boolean decryptMode) throws GeneralSecurityException{
        //Use a KeyFactory to derive the corresponding key from the passphrase:
        PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(keySpec);
        //Create parameters from the salt and an arbitrary number of iterations:
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 42);
        //Set up the cipher:
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        //Set the cipher mode to decryption or encryption:
        if(decryptMode){cipher.init(Cipher.ENCRYPT_MODE, key, pbeParamSpec);} 
        else {cipher.init(Cipher.DECRYPT_MODE, key, pbeParamSpec);}
        return cipher;
    }
    
    public static byte[] encrpyt(String pwd, byte[] data) throws InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, GeneralSecurityException {
        byte[] decData;
        byte[] encData;
        Cipher cipher = makeCipher(pwd, true);
        int blockSize = 8;
        //Figure out how many bytes are padded
        int paddedCount = blockSize - ((int)data.length  % blockSize );
        //Figure out full size including padding
        int padded = (int)data.length + paddedCount;
        decData = new byte[padded];
        //Write out padding bytes as per PKCS5 algorithm
        for( int i = 0; i < padded; ++i )
            if (i >= (int)data.length){decData[i] = (byte)paddedCount;} else {decData[i] = data[i];}
        //Encrypt the file data:
        encData = cipher.doFinal(decData);
        return encData;
    }
    
    public static byte[] decrypt(String pwd, byte[] data) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, GeneralSecurityException {
        byte[] decData;
        //Generate the cipher using pass:
        Cipher cipher = makeCipher(pwd, false);
        //Decrypt the file data:
        decData = cipher.doFinal(data);
        //Figure out how much padding to remove
        int padCount = (int)decData[decData.length - 1];
        //Naive check, will fail if plaintext file actually contained
        //this at the end
        //For robust check, check that padCount bytes at the end have same value
        if( padCount >= 1 && padCount <= 8 )
            decData = Arrays.copyOfRange( decData , 0, decData.length - padCount);
        return decData;
    }
    
    public static byte[] deriveSignature(JsonObject sigObject, byte[] msg) {
        int recId = 0;
        for (int x = 0; x < 4; x++) {
            int headerByte = x + 27;
            byte v = (byte) headerByte;
            byte[] r = Numeric.toBytesPadded(new BigInteger(sigObject.get("r").getAsString()), 32);
            byte[] s = Numeric.toBytesPadded(new BigInteger(sigObject.get("s").getAsString()), 32);
            byte[] signature = new byte[65];
            System.arraycopy(r, 0, signature, 0, r.length);
            System.arraycopy(s, 0, signature, r.length, s.length);
            signature[64] = v;
            if (Cryptography.verify(signature, msg,new BigInteger(sigObject.get("public").getAsString()))) {
                recId = x;
                System.out.println("ID Found");
                return signature;
            }
            if (x == 3) {
                return null;
            }
        }
        return null;
    }
    
    public static byte[] generateSignatureRSV(JsonObject sigObject) {
        int recId = 0;
        recId = sigObject.get("v").getAsInt();
        int headerByte = recId + 27;
        byte v = (byte) headerByte;
        byte[] r = Numeric.toBytesPadded(new BigInteger(sigObject.get("r").getAsString()), 32);
        byte[] s = Numeric.toBytesPadded(new BigInteger(sigObject.get("s").getAsString()), 32);
        byte[] contractSignature = new byte[65];
        System.arraycopy(r, 0, contractSignature, 0, r.length);
        System.arraycopy(s, 0, contractSignature, r.length, s.length);
        contractSignature[64] = v;
        return contractSignature;
    }
}
