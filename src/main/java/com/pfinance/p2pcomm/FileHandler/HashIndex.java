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
import java.util.ArrayList;

/**
 *
 * @author averypozzobon
 */
public class HashIndex implements Serializable {
    private ArrayList<HashEntry> hashes = new ArrayList<>();
    
    public ArrayList<HashEntry> getHashes() {return hashes;}
    
    public HashIndex() {
        
    }
    
    public void addHash(HashEntry entry) {
        hashes.add(entry);
    }
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {
            outputStream.writeObject(this);
        }

        return out.toByteArray();
    }
}
