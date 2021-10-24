/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Statistics;

import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import java.sql.Timestamp;
import java.util.Objects;

/**
 *
 * @author averypozzobon
 */
public class TransactionInOut {
    private Long date;
    private String hash;
    private float amount;
    private int type; //0 = in, 1 = out;
    
    public TransactionInOut(Long date, String hash, float amount) {
        this.date = date;
        this.hash = hash;
        this.amount = amount;
        if (this.amount >= 0) {
            this.type = 0;
        } else {
            this.type = 1;
        }
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
    
    public float getAmount() {
        return this.amount;
    }
    
    public void addAmount(float amount) {
        this.amount += amount;
        if (this.amount >= 0) {
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
