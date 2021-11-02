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
import java.math.BigInteger;

/**
 *
 * @author averypozzobon
 */
public class TransactionInput implements Serializable {
    private static final long serialVersionUID = -1087045000118285808L;
    public String previousTxnHash = null;
    public Integer outputIndex = null;
    public byte[] outputSignature = null;
    private BigInteger key = null;
    
    public TransactionInput(String previousTxn, Integer index, byte[] signature, BigInteger key) {
        this.previousTxnHash = previousTxn;
        this.outputIndex = index;
        this.outputSignature = signature;
        this.key = key;
    }
    
    public void removeSignature() {
        outputSignature = null;
    }
    
    public BigInteger getKey() {return this.key;}
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-32s|", this.previousTxnHash.substring(0, 32)));
        returnString.append(String.format("%-32s|", this.outputIndex));
        returnString.append(String.format("%-32s|", this.outputSignature));
        returnString.append(String.format("%-32s|", ""));
        return returnString.toString();
    }
    
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
