/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Contracts;

import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Transaction.*;
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
public class BorrowContract implements Serializable {
    private String inceptionDate = null;
    private String borrowerAddress = null;
    private Transaction validatorCommission = null;
    private String hash = null;
    private byte[] signature = null;
    private BigInteger key = null;
    
    public BorrowContract(String borrowerAddress, Transaction validatorCommission, ECKeyPair key) {
        String time = Long.toString(System.currentTimeMillis());
        this.inceptionDate = time;
        this.borrowerAddress = borrowerAddress;
        this.validatorCommission = validatorCommission;
        this.hash = DigestUtils.sha256Hex(this.inceptionDate + this.borrowerAddress + this.validatorCommission.getHash());
        this.key = key.getPublicKey();
        try {
            this.signature = Cryptography.sign(this.hash.getBytes(),key);
        } catch (Exception e) {e.printStackTrace();}
    }
    
    public String getInceptionDate() {return this.inceptionDate;}
    public String getBorrowerAddress() {return this.borrowerAddress;}
    public Transaction getValidatorCommission() {return this.validatorCommission;}
    public String getHash() {return this.hash;}
    public BigInteger getKey() {return this.key;}
    public byte[] getSignature() {return this.signature;}
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "Borrow Contract").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Contract Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Inception Date: " + this.inceptionDate));
        returnString.append(String.format("|%-131s|\n", "Borrower Address: " + this.borrowerAddress));
        returnString.append(String.format("|%-131s|\n", "Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "Commission Transaction").replace(' ', '-'));
        returnString.append(this.validatorCommission.toString());
        returnString.append(String.format("|%-131s|\n", "End Commission Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End Borrow Contract").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
