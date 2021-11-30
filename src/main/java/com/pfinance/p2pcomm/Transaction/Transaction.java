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
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 3084956333598879728L;
    private ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
    private String timestamp = null;
    private String hash = null;
    public String input_hashes = "";
    public String output_hashes = "";
    
    public Transaction() {
        String time = Long.toString(System.currentTimeMillis());
        this.timestamp = time;
        this.hash = DigestUtils.sha256Hex(input_hashes+output_hashes+this.timestamp);
    }
    
    public Transaction(String time) {
        this.timestamp = time;
        this.hash = DigestUtils.sha256Hex(input_hashes+output_hashes+this.timestamp);
    }
    
    public void addInput(TransactionInput input) {
        inputs.add(input);
        input_hashes += input.getHash();
        this.hash = DigestUtils.sha256Hex(input_hashes+output_hashes+this.timestamp);
    }
    public void addOutput(TransactionOutput output) {
        outputs.add(output);
        output_hashes += output.getHash();
        this.hash = DigestUtils.sha256Hex(input_hashes+output_hashes+this.timestamp);
    }
    
    public BigDecimal sum() {
        BigDecimal sumValue = BigDecimal.ZERO;
        for (TransactionOutput output : outputs) {
            sumValue = sumValue.add(output.value);
        }
        return sumValue;
    }

    
    public String getHash() {return this.hash;}
    public ArrayList<TransactionInput> getInputs() {return this.inputs;}
    public ArrayList<TransactionOutput> getOutputs() {return this.outputs;}
    public String getTimestamp() {return this.timestamp;}
    
    public String toString() {
        StringBuffer returnString = new StringBuffer();
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        //TRANSACTION DETAILS
        returnString.append(String.format("|%-131s|\n", "Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n","Hash: " + this.hash));
        returnString.append(String.format("|%-131s|\n","Timestamp: " + this.timestamp));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        //INPUTS
        returnString.append(String.format("|%-131s|\n", "Inputs").replace(' ', '-'));
        returnString.append(String.format("|%-32s|", "Previous Txn"));
        returnString.append(String.format("%-32s|", "Output Index"));
        returnString.append(String.format("%-32s|", "Output Signature"));
        returnString.append(String.format("%-32s|", ""));
        returnString.append(String.format("\n|%-131s|\n", "").replace(' ', '-'));
        for (int i = 0; i < this.inputs.size(); i++) {
            returnString.append(this.inputs.get(i).toString());
            returnString.append(String.format("\n|%-131s|\n", "").replace(' ', '-'));
        }
        returnString.append(String.format("|%-131s|\n", "End Inputs").replace(' ', '-'));
        //END INPUTS
        //OUTPUTS
        returnString.append(String.format("|%-131s|\n", "Outputs").replace(' ', '-'));
        returnString.append(String.format("|%-32s|", "Address"));
        returnString.append(String.format("%-32s|", "Value"));
        returnString.append(String.format("%-32s|", ""));
        returnString.append(String.format("%-32s|", ""));
        returnString.append(String.format("\n|%-131s|\n", "").replace(' ', '-'));
        for (int i = 0; i < this.outputs.size(); i++) {
            returnString.append(this.outputs.get(i).toString());
            returnString.append(String.format("\n|%-131s|\n", "").replace(' ', '-'));
        }
        returnString.append(String.format("|%-131s|\n", "End Outputs").replace(' ', '-'));
        //End Outputs
        returnString.append(String.format("|%-131s|\n", "End Transaction").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
    
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ObjectOutputStream outputStream = new ObjectOutputStream(out)) {outputStream.writeObject(this);}
        return out.toByteArray();
    } 
}
