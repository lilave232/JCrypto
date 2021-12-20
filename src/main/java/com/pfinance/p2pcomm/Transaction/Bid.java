/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Transaction;

import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Wallet.Key;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class Bid implements Serializable {
    private static final long serialVersionUID = 3084956333598879728L;
    private Transaction transaction = null;
    private String timestamp = null;
    private String contractHash = null;
    private String hash = null;
    private BigInteger key = null;
    private byte[] signature = null;
    
    public Bid(String contractHash, Transaction transaction, Key key) {
        String time = Long.toString(System.currentTimeMillis());
        this.timestamp = time;
        this.contractHash = contractHash;
        this.key = key.getKey().getPublicKey();
        this.transaction = transaction;
        this.hash = DigestUtils.sha256Hex(this.timestamp+this.contractHash+this.transaction.getHash());
        this.signature = Cryptography.sign(this.hash.getBytes(), key.getKey());
    }
    
    public Bid(String time, String contractHash, Transaction transaction, byte[] signature, BigInteger key) {
        this.timestamp = time;
        this.contractHash = contractHash;
        this.key = key;
        this.transaction = transaction;
        this.transaction.removeSignature();
        this.hash = DigestUtils.sha256Hex(this.timestamp+this.contractHash+this.transaction.getHash());
        this.signature = signature;
    }

    
    public String getHash() {return this.hash;}
    public String getContractHash() {return this.contractHash;}
    public String getTimestamp() {return this.timestamp;}
    public byte[] getSignature() {return this.signature;}
    public Transaction getTransaction() {return this.transaction;}
    public BigInteger getKey() {return this.key;}
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        //TRANSACTION DETAILS
        returnString.append(String.format("|%-131s|\n", "Bid").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n","Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n","Timestamp: " + this.timestamp));
        returnString.append(String.format("|%-131s|\n","Contract: " + this.contractHash));
        returnString.append(String.format("|%-131s|\n","Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Bid Transaction").replace(' ', '-'));
        returnString.append(this.transaction.toString());
        returnString.append(String.format("|%-131s|\n", "End Bid Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End Bid").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    } 
}
