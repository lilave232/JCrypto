/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.FileHandler;

import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author averypozzobon
 */
public class Validator implements Serializable {
    public final static long serialVersionUID = 3534229586728269544L;
    private String stakeHash;
    private String borrowContractHash;
    private float balance;
    
    public Validator(String stakeHash, String borrowContractHash) {
        this.stakeHash = stakeHash;
        this.borrowContractHash = borrowContractHash;
    }
    
    public float getBalance() {
        return this.balance;
    }
    
    public String getStakeHash() {
        return this.stakeHash;
    }
    
    public String getBorrowContractHash() {
        return this.borrowContractHash;
    }
    
    public void setBalance(float balance) {
        this.balance = balance;
    } 
    
    @Override
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n","Stake Hash: " + this.stakeHash));
        returnString.append(String.format("|%-131s|\n","Stake Amount: " + String.valueOf(this.balance)));
        return returnString.toString();
    }
    
    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Validator o1 = (Validator) o;
      return Objects.equals(this.stakeHash, o1.stakeHash);
    }
}
