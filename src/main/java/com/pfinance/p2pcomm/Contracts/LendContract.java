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
public class LendContract implements Serializable {
    private static final long serialVersionUID = -449495291419446786L;
    private String inceptionDate = null;
    private String lenderAddress = null;
    private String borrowContractHash = null;
    private Transaction lendTransaction = null;
    private String hash = null;
    private byte[] signature = null;
    private BigInteger key = null;
    
    public LendContract(String lenderAddress, String borrowContractHash, Transaction lendTransaction, ECKeyPair key) {
        String time = Long.toString(System.currentTimeMillis());
        this.inceptionDate = time;
        this.lenderAddress = lenderAddress;
        this.borrowContractHash = borrowContractHash;
        this.lendTransaction = lendTransaction;
        this.hash = DigestUtils.sha256Hex(this.inceptionDate + this.lenderAddress + this.borrowContractHash + this.lendTransaction.getHash());
        try {
            this.key = key.getPublicKey();
            this.signature = Cryptography.sign(this.hash.getBytes(),key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void setTimestamp(String timestamp) {
        this.inceptionDate = Long.toString(System.currentTimeMillis());
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public void setSignature(byte[] sig) {
        this.signature = sig;
    }
    
    public void setKey(BigInteger key) {
        this.key = key;
    }
    
    
    
    public String getInceptionDate() {return this.inceptionDate; }
    public String getLenderAddress() {return this.lenderAddress; }
    public String getBorrowContractHash() {return this.borrowContractHash; }
    public Transaction getLendTransaction() { return this.lendTransaction; }
    public String getHash() {return this.hash;}
    public BigInteger getKey() {return this.key;}
    public byte[] getSignature() {return this.signature;}
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "Lend Contract").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Contract Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Inception Date: " + this.inceptionDate));
        returnString.append(String.format("|%-131s|\n", "Lender Address: " + this.lenderAddress));
        returnString.append(String.format("|%-131s|\n", "Borrower Contract Hash: " + this.borrowContractHash));
        returnString.append(String.format("|%-131s|\n", "Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "Lend Transaction").replace(' ', '-'));
        returnString.append(this.lendTransaction.toString());
        returnString.append(String.format("|%-131s|\n", "End Lend Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End Lend Contract").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
