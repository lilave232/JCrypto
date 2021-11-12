/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Wallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class KeyGenerator {
    
    private static String[] wordlist = new String[2048];
    private static final String[] dict =
            {"0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000",
                    "1001", "1010", "1011", "1100", "1101", "1110", "1111"};
    
    
    
    public String createEntropy() {
        UUID uuid = UUID.randomUUID();
        String[] digits = uuid.toString().split("\\-");
        StringBuilder randomDigits = new StringBuilder();
        for (String digit : digits) {
            randomDigits.append(digit);
        }
        return randomDigits.toString();
    }
    
    public String generateMnemonic(String entropy) throws DecoderException, UnsupportedEncodingException, IOException {
        //generate sha-256 from entropy
        String encodeStr = "";
        byte[] hash = DigestUtils.sha256(Hex.decodeHex(entropy));
        encodeStr = String.valueOf(Hex.encodeHex(hash));
        char firstSHA = encodeStr.charAt(0);
        String new_entropy = entropy + firstSHA;
        String bin_entropy = "";
        for (int i = 0; i < new_entropy.length(); i++) {
            bin_entropy += dict[Integer.parseInt(new_entropy.substring(i, i + 1), 16)];
        }
        String[] segments = new String[12];
        //hardcode
        for (int i = 0; i <= 11; i++) {
            segments[i] = bin_entropy.substring(i * 11, (i + 1) * 11);
        }
        String path = System.getProperty("user.dir") + "/bip-39/english.txt";
        readTextFile(path);
        String mnemonic = "";
        //generate mnemonic
        mnemonic += wordlist[Integer.valueOf(segments[0], 2)];
        for (int j = 1; j < segments.length; j++) {
            mnemonic += " " + (wordlist[Integer.valueOf(segments[j], 2)]);
        }
        return mnemonic;
    }
    
    public void readTextFile(String filePath) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        String encoding = "utf-8";
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt = null;
            int index = 0;
            while ((lineTxt = bufferedReader.readLine()) != null) {wordlist[index++] = lineTxt;}
            read.close();
        } else {System.out.println("Could not find file.");}
    }
    
    public byte[] getSeed(String mnemonic, String salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException, UnsupportedEncodingException {
        
        char[] chars = Normalizer.normalize(mnemonic, Normalizer.Form.NFKD).toCharArray();
        byte[] salt_ = salt.getBytes("UTF-8");
        KeySpec spec = new PBEKeySpec(chars, salt_, 2048, 512);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        return f.generateSecret(spec).getEncoded();
    }
    
    public byte[] getMaster(String mnemonic) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        //GENERATE SEED
        String salt = "mnemonic";
        byte[] seed = getSeed(mnemonic, salt);
        //HMAC SEED
        byte[] master = getSeed(Hex.encodeHexString(seed), "Bitcoin seed");
        return master;
    }
    
    public Key generate(byte[] seed) throws Exception {
        ECKeyPair pair = new KeyDerivation().deriveKeyPath("m/\'",seed);
        //System.out.println(pair.getPrivateKey());
        //System.out.println(pair.getPublicKey());
        //System.out.println(Hex.encodeHexString(pair.getPublicKey().toByteArray()));
        String address = DigestUtils.sha256Hex(pair.getPublicKey().toByteArray());
        //System.out.println(address);
        Key key = new Key(pair,address);
        return key;
    }
    
}
