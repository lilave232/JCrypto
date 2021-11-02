/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Contracts;

import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Wallet.Key;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class EndLendContract implements Serializable {
    private String timestamp;
    private String lendContractHash;
    private String borrowContractHash;
    private Transaction validatorCommission = null;
    private BigInteger publicKey;
    private String hash = null;
    private byte[] signature = null;
    
    public EndLendContract(String lendContractHash, String borrowContractHash, Transaction validatorCommission, Key key) {
        this.timestamp = Long.toString(System.currentTimeMillis());
        this.lendContractHash = lendContractHash;
        this.borrowContractHash = borrowContractHash;
        this.publicKey = key.getKey().getPublicKey();
        this.validatorCommission = validatorCommission;
        this.hash = DigestUtils.sha256Hex(this.timestamp + this.lendContractHash + this.borrowContractHash + this.validatorCommission.getHash());
        this.signature = Cryptography.sign(this.hash.getBytes(), key.getKey());
    }
    
    public String getHash() {return this.hash;}
    public String getTimestamp() { return this.timestamp; }
    public BigInteger getKey() { return this.publicKey; }
    public byte[] getSignature() { return this.signature; }
    public Transaction getValidatorCommission() {return this.validatorCommission;}
    public String getLendContractHash() {return this.lendContractHash; }
    public String getBorrowContractHash() {return this.borrowContractHash; }
    
    @Override
    public String toString() {
        StringBuilder returnString = new StringBuilder();
        returnString.append(String.format("|%-131s|\n", "End Lend Contract").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Timestamp: " + this.timestamp));
        returnString.append(String.format("|%-131s|\n", "Lend Contract Hash: " + this.lendContractHash));
        returnString.append(String.format("|%-131s|\n", "Borrow Contract Hash: " + this.borrowContractHash));
        returnString.append(String.format("|%-131s|\n", "Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
