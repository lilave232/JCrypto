/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Wallet;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.math.ec.ECPoint;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class KeyDerivation {
    
    private boolean hard = false;
    
    public KeyDerivation() {
        
    }
    
    byte[] getPoint(byte[] key) {
        return pointSerP_gMultiply(new BigInteger(1, key));
    }
    
    static byte[] pointSerP(final ECPoint point) {
        return point.getEncoded(true);
    }
    
    
    static byte[] pointSerP_gMultiply(final BigInteger p) {
        return pointSerP(gMultiply(p));
    }
    
    private static ECPoint gMultiply(BigInteger p) {
        X9ECParameters CURVE = CustomNamedCurves.getByName("secp256k1");
        return CURVE.getG()
                .multiply(p);
    }
    
    public ECKeyPair deriveKeyPath(CharSequence derivationPath, byte[] parent) throws NoSuchAlgorithmException, InvalidKeyException {
        ECKeyPair keyPair = null;
        final int length = derivationPath.length();
        if (length == 0){throw new IllegalArgumentException("Path cannot be empty");}
        if (derivationPath.charAt(0) != 'm'){throw new IllegalArgumentException("Path must start with m");}
        if (length == 1){return ECKeyPair.create(parent);}
        if (derivationPath.charAt(1) != '/'){throw new IllegalArgumentException("Path must start with m/");}
        int buffer = 0;
        for (int i = 2; i < length; i++) {
            final char c = derivationPath.charAt(i);
            switch (c) {
                case '\'':
                    hard = true;
                    break;
                case '/':
                    hard = false;
                    buffer = 0;
                    break;
                default:
                    buffer *= 10;
                    if (c < '0' || c > '9'){throw new IllegalArgumentException("Illegal character in path: " + c);}
                    buffer += c - '0';
                    if (hard){throw new IllegalArgumentException("Index number too large");}
            }
        }
        keyPair = deriveKey(parent, buffer);
        return keyPair;
    }
 
    
    public ECKeyPair deriveKey(byte[] parent, int index) throws NoSuchAlgorithmException, InvalidKeyException {
        ECKeyPair keyPair = null;
        //DERIVE CHILD KEY
        int childIndex = index;
        //GET MASTER KEY AND CHAIN CODE
        byte[] masterKey = Arrays.copyOf(parent, 32);
        byte[] chainCode = new byte[parent.length - 32];
        System.arraycopy(parent, 32, chainCode, 0, chainCode.length);
        //APPEND 0 to BEGINNING OF MASTER KEY
        byte[] privKey = new byte[37];
        if (hard) {
            byte[] zero = new byte[1];
            zero[0] = 0;
            System.arraycopy(masterKey, 0, privKey, 1, masterKey.length);
            System.arraycopy(zero, 0, privKey, 0, 1);
        } else {System.arraycopy(getPoint(masterKey), 0, privKey, 0, masterKey.length);}
        //APPEND INDEX TO END OF MASTER KEY
        privKey[33] = (byte) (childIndex >> 24);
        privKey[34] = (byte) (childIndex >> 16);
        privKey[35] = (byte) (childIndex >> 8);
        privKey[36] = (byte) (childIndex);
        //HMAC CHAIN CODE WITH PRIVATE KEY
        SecretKeySpec keySpec = new SecretKeySpec(chainCode, "HmacSHA512");
        Mac hmacSha512 = Mac.getInstance("HmacSHA512");
        hmacSha512.init(keySpec);
        byte[] I = hmacSha512.doFinal(privKey);
        //GET CHILD KEY and CHAIN CODE FROM FIRST 32 and last 32 bytes
        Arrays.fill(privKey, (byte) 0);
        final byte[] ChildKey = Arrays.copyOf(I, 32);
        byte[] ChildChainCode = new byte[ChildKey.length - 32];
        System.arraycopy(I, 32, ChildChainCode, 0, ChildChainCode.length);
        //Take ChildKey Add Parent Key Modulo N From Curve
        byte[] key = masterKey;
        BigInteger parse256_ChildKey = new BigInteger(1, ChildKey);
        X9ECParameters CURVE = CustomNamedCurves.getByName("secp256k1");
        final BigInteger ki = parse256_ChildKey.add(new BigInteger(1, key)).mod(CURVE.getN());
        //If ChildKey greater than Curve N need to do derive further
        if (parse256_ChildKey.compareTo(CURVE.getN()) >= 0 || ki.equals(BigInteger.ZERO)) {return deriveKey(parent,index+1);}
        //Unable to Serialize Buffer
        if (ki.bitLength() > ChildKey.length * 8){throw new RuntimeException("ser256 failed, cannot fit integer in buffer");}
        final byte[] modArr = ki.toByteArray();
        Arrays.fill(ChildKey, (byte) 0);
        if (modArr.length < ChildKey.length) {System.arraycopy(modArr, 0, ChildKey, ChildKey.length - modArr.length, modArr.length);} 
        else { System.arraycopy(modArr, modArr.length - ChildKey.length, ChildKey, 0, ChildKey.length);}
        Arrays.fill(modArr, (byte) 0);
        keyPair = ECKeyPair.create(ChildKey);
        return keyPair;
    }
}
