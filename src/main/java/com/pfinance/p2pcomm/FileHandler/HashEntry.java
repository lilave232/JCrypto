/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.FileHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author averypozzobon
 */
public class HashEntry implements Serializable {
    public int index = 0;
    public String hash = null;
    
    public HashEntry(int index, String hash) {
        this.index = index;
        this.hash = hash;
    }
}
