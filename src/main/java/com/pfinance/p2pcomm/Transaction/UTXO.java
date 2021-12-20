/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Transaction;

import com.pfinance.p2pcomm.Contracts.BorrowContract;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Wallet.Key;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class UTXO implements Serializable {
    private TransactionOutput output = null;
    private String timestamp = null;
    private String timestamp_out = null;
    private String hash;
    private String hash_out = null;
    private Integer index = null;
    private Boolean lent = false;
    private Boolean lentReturn = false;
    private Boolean borrowed = false;
    private BorrowContract borrowContract = null;
    
    public UTXO(TransactionOutput output, String timestamp, String hash, Integer index, Key key) {
        this.timestamp = timestamp;
        this.output = output;
        this.hash = hash;
        this.index = index;
    }
    
    public TransactionInput getInput(Key key) {
        try {
            TransactionInput input = new TransactionInput(this.hash,this.index);
            return input;
        } catch (Exception e) {
            return null;
        }
    }
    
    public void setOut(String timestampOut, String hashOut) {
        this.timestamp_out = timestampOut;
        this.hash_out = hashOut;
    }
    
    public String getTimestampIn() {return this.timestamp;}
    public String getTimestampOut() {return this.timestamp_out;}
    public void setLent(Boolean state) {this.lent = state;}
    public void setLentReturn(Boolean state) {this.lentReturn = state;}
    public void setBorrowed(Boolean state, BorrowContract contract) {this.borrowed = state; this.borrowContract=contract;}
    public Boolean getLent() {return this.lent;}
    public Boolean getLentReturn() {return this.lentReturn;}
    public Boolean getBorrowed() {return this.borrowed;}
    public BorrowContract getBorrowedContract() {return this.borrowContract;}
    public String getPreviousHash() {return this.hash;}
    public String getHashOut() {return this.hash_out;}
    public Integer getIndex() {return this.index;}
    public String getAddress() {return this.output.address;}
    public BigDecimal toFloat() {return this.output.value;}
    
    @Override
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "UTXO").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Transaction Timestamp In: " + this.timestamp));
        returnString.append(String.format("|%-131s|\n", "Transaction In: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Transaction Timestamp Out: " + this.timestamp_out));
        returnString.append(String.format("|%-131s|\n", "Transaction Out: " + this.hash_out));
        returnString.append(String.format("|%-131s|\n", "Index: " + this.index));
        returnString.append(String.format("|%-131s|\n", "Address: " + this.output.address));
        returnString.append(String.format("|%-131s|\n", "Value: " + this.output.value));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
    
}
