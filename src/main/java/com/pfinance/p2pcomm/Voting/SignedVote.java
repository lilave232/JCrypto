/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Voting;

import com.pfinance.p2pcomm.Cryptography.Cryptography;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class SignedVote implements Serializable {
    int vote = 0;
    String address = null;
    String stakeHash = null;
    String date = null;
    private String hash = null;
    private byte[] signature = null;
    private BigInteger key = null;
    
    public SignedVote(int vote, String address, String stakeHash, ECKeyPair key) {
        this.date =  Long.toString(System.currentTimeMillis());
        this.vote = vote;
        this.address = address;
        this.stakeHash = stakeHash;
        this.key = key.getPublicKey();
        this.hash = DigestUtils.sha256Hex(this.date + String.valueOf(this.vote) + this.address + this.stakeHash);
        try {
            this.signature = Cryptography.sign(this.hash.getBytes(),key);
        } catch (Exception e) {e.printStackTrace();}
    }
    
    public String getStakeHash() {return this.stakeHash;}
    public String getHash() {return this.hash;}
    public byte[] getSignature() {return this.signature;}
    public BigInteger getKey() {return this.key;}
    public int getVote() {return this.vote;}
    public String getAddress() {return this.address;}
    public String getDate() {return this.date;}
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
