/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Wallet;

import java.io.Serializable;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class Key implements Serializable {
    private ECKeyPair key = null;
    private String address = null;
    
    public Key(ECKeyPair key, String address) {
        this.key = key;
        this.address = address;
    }
    
    public ECKeyPair getKey() {
        return key;
    }
    
    public String getAddress() {
        return address;
    }
 
}
