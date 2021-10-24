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
import java.sql.Timestamp;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class NFT implements Serializable {
    private static final long serialVersionUID = -6355386719916112978L;
    private String inceptionDate = null;
    private Transaction mintFee = null;
    private String initiatorAddress = null;
    private String title = null;
    private String description = null;
    private byte[] data = null;
    private String hash = null;
    private String fileType = "image/png";
    private byte[] signature = null;
    private BigInteger key = null;
    
    public NFT(String initiatorAddress, Transaction mintFee, String fileType, ECKeyPair key, String title, String description, byte[] data) {
        String time = Long.toString(System.currentTimeMillis());
        this.inceptionDate = time;
        this.initiatorAddress = initiatorAddress;
        this.mintFee = mintFee;
        this.data = data;
        this.title = title;
        this.description = description;
        this.hash = DigestUtils.sha256Hex(this.inceptionDate + this.initiatorAddress + DigestUtils.sha256Hex(data) + this.mintFee.getHash());
        this.key = key.getPublicKey();
        this.fileType = fileType;
        try {
            this.signature = Cryptography.sign(this.hash.getBytes(),key);
        } catch (Exception e) {e.printStackTrace();}
    }
    
    public byte[] getData() {return this.data;}
    public String getInceptionDate() {return this.inceptionDate;}
    public String getInceptionDateFormatted() {return new Timestamp(Long.valueOf(this.inceptionDate)).toString();}
    public String getInitiatorAddress() {return this.initiatorAddress;}
    public Transaction getMintFee() {return this.mintFee;}
    public String getHash() {return this.hash;}
    public BigInteger getKey() {return this.key;}
    public String getTitle() {
        if (this.title == null) {
            return "No Title";
        }
        return this.title;
    }
    public String getDescription() {
        if (this.description == null) {
            return "No Description";
        }
        return this.description;
    }
    public String getFileType() {return this.fileType;}
    public byte[] getSignature() {return this.signature;}
    
    public String getBase64() {
        if (this.fileType == null) {
            return "data:" + "image/png" + ";base64," + DatatypeConverter.printBase64Binary(this.data);
        } else {
            return "data:" + this.fileType + ";base64," + DatatypeConverter.printBase64Binary(this.data);
        }
    }
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "NFT").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Contract Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n", "Inception Date: " + this.inceptionDate));
        returnString.append(String.format("|%-131s|\n", "Borrower Address: " + this.initiatorAddress));
        returnString.append(String.format("|%-131s|\n", "Title: " + this.title));
        returnString.append(String.format("|%-131s|\n", "Description: " + this.description));
        returnString.append(String.format("|%-131s|\n", "File Type: " + this.fileType));
        returnString.append(String.format("|%-131s|\n", "Data Size: " + this.data.length));
        returnString.append(String.format("|%-131s|\n", "Signature: " + this.signature));
        returnString.append(String.format("|%-131s|\n", "Mine Fee").replace(' ', '-'));
        returnString.append(this.mintFee.toString());
        returnString.append(String.format("|%-131s|\n", "End Mint Fee").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "End NFT").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    }
}
