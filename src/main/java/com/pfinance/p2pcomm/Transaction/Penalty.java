/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class Penalty implements Serializable {
    private String stakeHash = null;
    private String timestamp = null;
    private Transaction penalty = null;
    private String hash = null;
    
    public Penalty(String stakeHash, String timestamp, Transaction penalty) {
        this.stakeHash = stakeHash;
        this.timestamp = timestamp;
        this.penalty = penalty;
        this.hash = DigestUtils.sha256Hex(this.stakeHash + this.timestamp + this.penalty.getHash());
    }
    
    public String getStakeHash() {return this.stakeHash;}
    public String getTimestamp() {return this.timestamp;}
    public Transaction getTransaction () {return this.penalty;}
    
    public String getHash() {return this.hash;}
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "Penalty").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Penalty Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Timestamp: " + this.timestamp));
        returnString.append(String.format("|%-131s|\n", "Stake Hash: " + this.stakeHash));
        returnString.append(String.format("|%-131s|\n", "Penalty Transaction").replace(' ', '-'));
        returnString.append(this.penalty.toString());
        returnString.append(String.format("|%-131s|\n", "End Penalty Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End Penalty").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
    
}
