/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.FileHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author averypozzobon
 */
public class FileHandler {
    
    public FileHandler() {
        
    }
    
    public void writeBytes(String path, byte[] bytes) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(path,false);
        fos.write(bytes);
    }
    
    public void writeObject(String path, Object object) throws IOException {
        FileOutputStream fos = new FileOutputStream(path,false);
        ObjectOutputStream objectOut = new ObjectOutputStream(fos);
        objectOut.writeObject(object);
    }
    
    public Object readObject(String path) throws FileNotFoundException, IOException, ClassNotFoundException {
        File f = new File(path);
        if(f.exists() && !f.isDirectory()) { 
            FileInputStream fis = new FileInputStream(path);
            ObjectInputStream objectIn = new ObjectInputStream(fis);
            return objectIn.readObject();
        }
        return null;
    }
    
    public byte[] readBytes(String path) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(path);
        byte[] encoded = fis.readAllBytes();
        return encoded;
    }
    
    public void renameFile(String old_f, String new_f) throws Exception {
        File f_old = new File(old_f);
        File f_new = new File(new_f);
        if (!f_old.exists())
            throw new Exception("File Does Not Exist");
        boolean result = f_old.renameTo(f_new);
        if (!result)
            throw new Exception("Unable to Set Pending");
    }
    
    public void deleteFile(String path) throws Exception {
        try {
            File f = new File(path);
            boolean result = f.delete();
        } catch (Exception e) {}
    }
    
}
