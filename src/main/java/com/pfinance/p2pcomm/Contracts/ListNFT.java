/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Contracts;

import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Transaction.Bid;
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
public class ListNFT implements Serializable {
    private String timestamp = null;
    private Transaction validatorCommission = null;
    private String nftHash = null;
    private String hash = null;
    private byte[] signature = null;
    private BigInteger key = null;
    
    public ListNFT(String nftHash, Transaction validatorCommission, Key key) {
        this.timestamp = Long.toString(System.currentTimeMillis());
        this.nftHash = nftHash;
        this.validatorCommission = validatorCommission;
        this.hash = DigestUtils.sha256Hex(this.timestamp + this.nftHash + this.validatorCommission.getHash());
        this.key = key.getKey().getPublicKey();
        this.signature = Cryptography.sign(this.hash.getBytes(), key.getKey());
    }
    
    public ListNFT(String timestamp, String nftHash, Transaction validatorCommission, byte[] signature, BigInteger key) {
        this.timestamp = timestamp;
        this.nftHash = nftHash;
        this.validatorCommission = validatorCommission;
        this.hash = DigestUtils.sha256Hex(this.timestamp + this.nftHash + this.validatorCommission.getHash());
        this.key = key;
        this.signature = signature;
    }
    
    public String getTimestamp() {return this.timestamp;}
    public String getNFTHash() {return this.nftHash;}
    public BigInteger getKey() {return this.key;}
    public Transaction getValidatorCommission() {return this.validatorCommission;}
    public String getHash() {return this.hash;}
    public byte[] getSignature() {return this.signature;}
    
    @Override 
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "List NFT").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "List Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Timestamp: " + this.timestamp));
        returnString.append(String.format("|%-131s|\n", "NFT Hash: " + this.nftHash));
        returnString.append(String.format("|%-131s|\n", "Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "Commission").replace(' ', '-'));
        returnString.append(this.validatorCommission.toString());
        returnString.append(String.format("|%-131s|\n", "End Commission").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End NFT List ").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
