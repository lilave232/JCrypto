/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author averypozzobon
 */
public class TransactionOutput implements Serializable {
    private static final long serialVersionUID = -6499451418241794783L;
    public String address;
    public float value;
    
    public TransactionOutput(String address, float value) {
        this.address = address;
        this.value = value;
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
    
    public ArrayList<TransactionOutput> toList() {
        ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        outputs.add(this);
        return outputs;
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
