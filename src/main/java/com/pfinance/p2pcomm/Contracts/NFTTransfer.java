/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Contracts;

import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Transaction.Bid;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class NFTTransfer implements Serializable {
    private static final long serialVersionUID = 6459378639014298908L;
    private String transferDate = null;
    private Transaction saleTransaction = null;
    private Bid bidTransaction = null;
    private String nftHash = null;
    private String previousHash = null;
    private String hash = null;
    private String transferToAddress = null;
    private byte[] signature = null;
    private BigInteger key = null;
    
    public NFTTransfer(Object saleTransaction, String previousHash, String nftHash, String transferToAddress, ECKeyPair key) {
        String time = Long.toString(System.currentTimeMillis());
        this.transferDate = time;
        String saleTransactionHash = "";
        if (saleTransaction instanceof Transaction) {
            this.saleTransaction = (Transaction) saleTransaction;
            saleTransactionHash = this.saleTransaction.getHash();
        }
        if (saleTransaction instanceof Bid) {
            this.bidTransaction = (Bid) saleTransaction;
            saleTransactionHash = this.bidTransaction.getHash();
        }
        this.nftHash = nftHash;
        this.previousHash = previousHash;
        this.transferToAddress = transferToAddress;
        this.hash = DigestUtils.sha256Hex(this.transferDate + this.nftHash + this.previousHash + this.transferToAddress + saleTransactionHash);
        this.key = key.getPublicKey();
        try {
            this.signature = Cryptography.sign(this.hash.getBytes(),key);
        } catch (Exception e) {e.printStackTrace();}
    }
    
    public NFTTransfer(String time, Object saleTransaction, String previousHash, String nftHash, String transferToAddress, byte[] signature, BigInteger key) {
        this.transferDate = time;
        String saleTransactionHash = "";
        if (saleTransaction instanceof Transaction) {
            this.saleTransaction = (Transaction) saleTransaction;
            saleTransactionHash = this.saleTransaction.getHash();
        }
        if (saleTransaction instanceof Bid) {
            this.bidTransaction = (Bid) saleTransaction;
            saleTransactionHash = this.bidTransaction.getHash();
        }
        this.nftHash = nftHash;
        this.previousHash = previousHash;
        this.transferToAddress = transferToAddress;
        this.hash = DigestUtils.sha256Hex(this.transferDate + this.nftHash + this.previousHash + this.transferToAddress + saleTransactionHash);
        this.key = key;
        this.signature = signature;
    }
    
    public String getHash() {return this.hash; }
    public BigInteger getKey() {return this.key;}
    public byte[] getSignature() {return this.signature;}
    public String getDate() {return this.transferDate;}
    public String getNFTHash() {return this.nftHash;}
    public String getPreviousHash() {return this.previousHash;}
    public String getTransferAddress() {return this.transferToAddress;}
    public Transaction getSaleTransaction() { return this.saleTransaction; }
    public Bid getBidTransaction() {return this.bidTransaction;}
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "NFT Transfer").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Transfer Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "NFT Hash: " + this.nftHash));
        returnString.append(String.format("|%-131s|\n", "Previous Hash: " + this.previousHash));
        returnString.append(String.format("|%-131s|\n", "Transfer Date: " + this.transferDate));
        returnString.append(String.format("|%-131s|\n", "Transfer To: " + this.transferToAddress));
        returnString.append(String.format("|%-131s|\n", "Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "Sale Transaction").replace(' ', '-'));
        if (this.saleTransaction != null) returnString.append(this.saleTransaction.toString());
        if (this.bidTransaction != null) returnString.append(this.bidTransaction.toString());
        returnString.append(String.format("|%-131s|\n", "End Sale Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End NFT Transfer").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
    
    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      NFTTransfer o1 = (NFTTransfer) o;
      return Objects.equals(this.nftHash, o1.nftHash);
    }
}
