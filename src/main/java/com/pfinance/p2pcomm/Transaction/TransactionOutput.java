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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class TransactionOutput implements Serializable {
    private static final long serialVersionUID = -6499451418241794783L;
    public String address;
    public BigDecimal value;
    private String hash = null;
    
    public TransactionOutput(String address, BigDecimal value) {
        this.address = address;
        this.value = value;
        this.hash = DigestUtils.sha256Hex(this.address);
    }
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        if (this.address.length() > 32) {
            returnString.append(String.format("|%-32s|", this.address.substring(0, 32)));
        } else {
            returnString.append(String.format("|%-32s|", this.address));
        }
        
        returnString.append(String.format("%-32s|", this.value));
        returnString.append(String.format("%-32s|", ""));
        returnString.append(String.format("%-32s|", ""));
        return returnString.toString();
    }
    
    public String getHash() {
        return this.hash;
    }
    
    public ArrayList<TransactionOutput> toList() {
        ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        outputs.add(this);
        return outputs;
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
      TransactionOutput o1 = (TransactionOutput) o;
      return Objects.equals(this.address + ":" + this.value, o1.address + ":" + o1.value);
    }
}
