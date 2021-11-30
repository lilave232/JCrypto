/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Blockchain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import org.apache.commons.codec.digest.DigestUtils;
import com.pfinance.p2pcomm.Transaction.*;
import com.pfinance.p2pcomm.Contracts.*;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import java.math.BigDecimal;

/**
 *
 * @author averypozzobon
 */
public class Block implements Serializable {
    private static final long serialVersionUID = -6703290616271460742L;
    private String stakeContractHash = null;
    private String timestamp = null;
    private String previousBlockHash = null;
    private String hash = null;
    private byte[] signature = null;
    private BigInteger key = null;
    ArrayList<Object> data = new ArrayList<>();
    
    public Block(String previousBlockHash, String stakeContractHash, String timestamp, String address, ArrayList<TransactionOutput> baseOutput, BigInteger pub) {
        this.previousBlockHash = previousBlockHash;
        this.stakeContractHash = stakeContractHash;
        this.timestamp = timestamp;
        this.key = pub;
        this.hash = hashBlock();
        //Add Base Transaction
        Transaction baseTransaction = new Transaction();
        TransactionInput baseInput = new TransactionInput(DigestUtils.sha256Hex("Base"),0,DigestUtils.sha256("Base"),null);
        BigDecimal reward = new BigDecimal((int) (20 * (1/Math.max(2*((int)(((Long.valueOf(this.timestamp)/1000) - 1577836800)/315360000)),1))));
        if (baseOutput.isEmpty()) {baseOutput.add(new TransactionOutput(address,(BigDecimal)reward));}
        for (TransactionOutput output : baseOutput) {baseTransaction.addOutput(output);}
        baseTransaction.addInput(baseInput);
        addData(baseTransaction,BigDecimal.ZERO);
    }
    
    public void addData(Object entry, BigDecimal fee) {
        this.data.add(entry);
        updateReward(fee);
        this.hash = hashBlock();
    }
    
    public void updateReward(BigDecimal fee) {
        Transaction original = (Transaction) this.data.get(0);
        BigDecimal value = original.sum();
        for (TransactionOutput output : original.getOutputs()) {
            output.value = output.value.add(fee.multiply((output.value.divide(value))));
        }
    }
    
    public String hashBlock() {
        StringBuffer dataHash = new StringBuffer();
        dataHash.append(this.previousBlockHash)
                .append(this.stakeContractHash)
                .append(this.timestamp)
                .append(hashData());
        return DigestUtils.sha256Hex(dataHash.toString());
    }
    
    public String hashData() {
        StringBuffer dataHash = new StringBuffer();
        this.data.forEach(t -> {
            if (t instanceof Transaction) {
                dataHash.append(((Transaction) t).getHash());
            } else if (t instanceof BorrowContract) {
                dataHash.append(((BorrowContract) t).getHash());
            } else if (t instanceof LendContract) {
                dataHash.append(((LendContract) t).getHash());
            } else if (t instanceof StakeContract) {
                dataHash.append(((StakeContract) t).getHash());
            }
        });
        return DigestUtils.sha256Hex(dataHash.toString());
    }
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Start Block").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n","Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n","Previous Hash: " + this.previousBlockHash));
        returnString.append(String.format("|%-131s|\n","Stake Contract Hash: " + this.stakeContractHash));
        returnString.append(String.format("|%-131s|\n","Timestamp: " + this.timestamp));
        returnString.append(String.format("|%-131s|\n", "Data").replace(' ', '-'));
        for (int i = 0; i < this.data.size(); i++) {
            returnString.append(String.format("|%-131s|\n", "Data Entry " + i).replace(' ', '-'));
            returnString.append(data.get(i).toString());
        }
        returnString.append(String.format("|%-131s|\n", "End Data").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End Block").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
    
    public String getHash() { return this.hash; }
    public String getStakeContractHash() { return this.stakeContractHash; }
    public String getPreviousBlockHash() { return this.previousBlockHash; }
    public String getTimestamp() { return this.timestamp; }
    public BigInteger getKey() {return this.key;}
    public ArrayList<Object> getData() { return this.data; }
}
