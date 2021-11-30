/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Statistics;

import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Objects;

/**
 *
 * @author averypozzobon
 */
public class TransactionInOut {
    private Long date;
    private String hash;
    private Integer output;
    private BigDecimal amount;
    private int type; //0 = in, 1 = out, 2 = pending in, 3 = pending out;
    
    public TransactionInOut(Long date, String hash, Integer output, BigDecimal amount) {
        this.date = date;
        this.hash = hash;
        this.output = output;
        
        this.amount = amount;
        if (this.amount.compareTo(BigDecimal.ZERO) >= 0) {
            this.type = 0;
        } else {
            this.type = 1;
        }
    }
    
    public void setType(int x) {
        this.type = x;
    }
    
    @Override
    public String toString() {
        return this.hash;
    }
    
    public int getType(){
        return this.type;
    }
    
    public Long getDate() {
        if (type == 0) {
            return this.date;
        } else {
            return this.date;
        }
    }
    
    public String getDateToString() {
        if (type == 0) {
            return new Timestamp(this.date).toString();
        } else {
            return new Timestamp(this.date).toString();
        }
    }
    
    public BigDecimal getAmount() {
        return this.amount;
    }
    
    public Integer getOutput() {
        return this.output;
    }
    
    public void addAmount(BigDecimal amount) {
        this.amount = this.amount.add(amount);
        if (this.amount.compareTo(BigDecimal.ZERO) >= 0) {
            this.type = 0;
        } else {
            this.type = 1;
        }
    }
    
    public String getHash() {
        return this.hash;
    }
    
    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TransactionInOut o1 = (TransactionInOut) o;
      return this.hash.equals(o1.hash);
    }
}
