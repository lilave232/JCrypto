/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Contracts;

import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Transaction.Transaction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class StakeContract implements Serializable {
    private String inceptionDate = null;
    private String borrowContractHash = null;
    private Transaction validatorCommission = null;
    private String hash = null;
    private byte[] signature = null;
    private BigInteger key = null;
    private String address = null;
    
    public StakeContract(String borrowContractHash, Transaction validatorCommission, ECKeyPair key) {
        try {
            String time = Long.toString(System.currentTimeMillis());
            this.inceptionDate = time;
            this.borrowContractHash = borrowContractHash;
            this.validatorCommission = validatorCommission;
            this.hash = DigestUtils.sha256Hex(this.inceptionDate + this.borrowContractHash + this.validatorCommission.toString());
            this.signature = Cryptography.sign(this.hash.getBytes(),key);
            this.key = key.getPublicKey();
            this.address = DigestUtils.sha256Hex(key.getPublicKey().toByteArray());
        } catch (Exception e) {e.printStackTrace();}
    }
    
    public String getInceptionDate() { return this.inceptionDate; }
    public String getBorrowContractHash() { return this.borrowContractHash;}
    public Transaction getValidatorCommission() {return this.validatorCommission;}
    public String getHash() { return this.hash; }
    public BigInteger getKey() { return this.key; }
    public String getAddress() { return this.address; }
    public byte[] getSignature() {return this.signature;}
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "Stake Contract").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Inception Date: " + this.inceptionDate));
        returnString.append(String.format("|%-131s|\n", "Borrow Contract Hash: " + this.borrowContractHash));
        returnString.append(String.format("|%-131s|\n", "Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "Commission Transaction").replace(' ', '-'));
        returnString.append(this.validatorCommission);
        returnString.append(String.format("|%-131s|\n", "End Commission Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End Stake Contract").replace(' ', '-'));
        return returnString.toString();
    }
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
