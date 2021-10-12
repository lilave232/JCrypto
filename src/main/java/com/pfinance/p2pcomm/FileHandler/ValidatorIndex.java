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
public class ValidatorIndex implements Serializable {
    private ArrayList<Validator> validators = new ArrayList<>();
    
    public ArrayList<Validator> getValidators(float amount) {
        ArrayList<Validator> returnValue = new ArrayList<>();
        for (Validator validator : this.validators) {
            if (Float.compare(validator.getBalance(), amount) >= 0) {
                returnValue.add(validator);
            }
        }
        return this.validators;
    }
    
    public ValidatorIndex() {
        
    }
    
    public Validator getValidator(String stakeHash) {
        for (Validator validator : this.validators) {
            if (validator.getStakeHash().equals(stakeHash))
                return validator;
        }
        return null;
    }
    
    public Validator getValidatorByBorrow(String stakeHash) {
        for (Validator validator : this.validators) {
            if (validator.getBorrowContractHash().equals(stakeHash))
                return validator;
        }
        return null;
    }
    
    public void addValidator(Validator entry) {
        validators.add(entry);
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {
            outputStream.writeObject(this);
        }

        return out.toByteArray();
    }
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        for (Validator validator : validators) {
            returnString.append(validator.toString());
        }
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
}
