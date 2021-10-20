/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Blockchain;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.commons.codec.digest.DigestUtils;
import com.pfinance.p2pcomm.FileHandler.*;
import com.pfinance.p2pcomm.Transaction.*;
import com.pfinance.p2pcomm.Contracts.*;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Wallet.*;
import com.pfinance.p2pcomm.Session;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;

/**
 *
 * @author averypozzobon
 */
public class BlockValidation {
    float stakeRequirement = (float) 10;
    boolean validateHeader = false;
    Session session = null;
    ArrayList<String> blockUtxosUsed = new ArrayList<>();
            
    public BlockValidation(Session session) {
        this.session = session;
        //this.currentReward = (float)(int) (20 * (1/Math.max(2*((int)(((Long.valueOf(system)/1000) - 1577836800)/315360000)),1)));
        //this.stakeRequirement = (float) (session.getBlockchain().getTotalFloat() * 0.001);
    }
    
    public void setStakeRequirement(Float value) {
        this.stakeRequirement = value;
    }
    
    public boolean validate(Block block, String previousBlock) {
        blockUtxosUsed = new ArrayList<String>();
        try {
            if (validateHeader) {
                if (session.getPeer().getHandler().isRequested(block.getHash())) {}
                else {
                    Long scheduledTime = this.session.getScheduler().getMineTime(block.getStakeContractHash());
                    Long endScheduledTime = this.session.getScheduler().getEndTime();
                    Long currentTime = System.currentTimeMillis();
                    if (scheduledTime == 0) return false;
                    //if (!(Long.valueOf(block.getTimestamp()) > currentTime - 60000 && Long.valueOf(block.getTimestamp()) < currentTime + 60000)) return false;
                    if (!((currentTime >= scheduledTime && currentTime <= scheduledTime + 180000))) return false;
                }
                System.out.println("Validating Header");
                if (!block.getHash().equals(DigestUtils.sha256Hex(block.getPreviousBlockHash()+block.getStakeContractHash()+block.getTimestamp()+block.hashData())))
                    return false;
                System.out.println("Hash Is Equal");
                if (!block.getPreviousBlockHash().equals(previousBlock)) return false;
                System.out.println("Previous Block is Equal");
                if (!verifyStakeHash(block.getStakeContractHash())) return false;
                System.out.println("Confirmed Stake Hash");
            }
            if (!verifyBase((Transaction) block.data.get(0))) return false;
            System.out.println("Confirmed Base Transaction");
            for (Object obj : block.getData().subList(1, block.getData().size())) {
                if (obj instanceof Transaction){if (!verifyTransaction((Transaction) obj)) return false;}
                else if (obj instanceof BorrowContract) {if (!verifyBorrowContract((BorrowContract) obj)) return false;}   
                else if (obj instanceof LendContract) {if (!verifyLendContract((LendContract) obj)) return false;}
                else if (obj instanceof StakeContract) {if (!verifyStakeContract((StakeContract) obj)) return false;} 
                else if (obj instanceof Penalty) {
                    if (session.getPeer().getHandler().isRequested(block.getHash())){ if (!verifyPenaltyPending((Penalty) (obj))) return false;}
                    else {if (!verifyPenalty((Penalty)(obj))) return false;}
                }
                else {return false;}
            }
            return true;
        } catch (Exception e){e.printStackTrace();return false;}
    }
    
    public boolean verifyStakeHash(String hash) throws IOException, ClassNotFoundException {
        Validator validator = session.getValidators().getValidator(hash);
        if (validator == null) return false;
        return true;
    }
    
    public boolean verifyBase(Transaction transaction) {
        try {
            float reward = getReward(transaction.getTimestamp());
            if (!transaction.getInputs().get(0).previousTxnHash.equals(DigestUtils.sha256Hex("Base"))
                || !transaction.getInputs().get(0).outputIndex.equals(0)
                || Float.compare(transaction.sum(), reward) != 0) {return false;}
            return true;
        } catch (Exception e) {e.printStackTrace();return false;}
    }
    
    public boolean verifyTransaction(Transaction transaction) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        ArrayList<TransactionInput> inputs = transaction.getInputs();
        ArrayList<TransactionOutput> outputs = transaction.getOutputs();
        float inputSum = 0;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (utxo == null) return false;
            if (!Cryptography.verify(input.outputSignature, utxo.getAddress().getBytes(),input.getKey())) return false;
            if (!DigestUtils.sha256Hex(input.getKey().toByteArray()).equals(utxo.getAddress())) return false;
            inputSum += utxo.toFloat();
        }
        if (Float.compare(inputSum, transaction.sum()) != 0) return false;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (blockUtxosUsed.contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) return false;
            blockUtxosUsed.add(utxo.getPreviousHash() + "|" + utxo.getIndex());
        }
        return true;
    }
    
    public boolean verifyBorrowContract(BorrowContract contract) throws ClassNotFoundException, Exception {
        String hash = DigestUtils.sha256Hex(contract.getInceptionDate()+contract.getBorrowerAddress()+contract.getValidatorCommission().getHash());
        if (!verifyTransaction(contract.getValidatorCommission())) return false;
        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
        return true;
    }
    
    public boolean verifyLendContract(LendContract contract) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        BorrowContract bcontract = session.getBlockFileHandler().getBorrowContract(contract.getBorrowContractHash());
        String hash = DigestUtils.sha256Hex(contract.getInceptionDate()+contract.getLenderAddress()+contract.getBorrowContractHash()+contract.getLendTransaction().getHash());
        if (bcontract == null) return false;
        if (!verifyTransaction(contract.getLendTransaction())) return false;
        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
        return true;
    }
    
    public boolean verifyStakeContract(StakeContract contract) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        BorrowContract bcontract = session.getBlockFileHandler().getBorrowContract(contract.getBorrowContractHash());
        String hash = DigestUtils.sha256Hex(contract.getInceptionDate() + contract.getBorrowContractHash() + contract.getValidatorCommission());
        if (bcontract == null) return false;
        if (!verifyTransaction(contract.getValidatorCommission())) return false;
        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
        return true;
    }
    
    public boolean verifyPenalty(Penalty penalty) {
        ArrayList<Penalty> penalties = session.getBlockFileHandler().getPendingPenalties(penalty.getStakeHash());
        for (Penalty penaltyCheck : penalties) {
            if (!penaltyCheck.getStakeHash().equals(penalty.getStakeHash())) continue;
            if (!penalty.getTransaction().getInputs().get(0).previousTxnHash.equals(DigestUtils.sha256Hex("Penalty"))) continue;
            if (!penalty.getTransaction().getInputs().get(0).outputIndex.equals(0)) continue;
            if (!penaltyCheck.getTransaction().getOutputs().equals(penalty.getTransaction().getOutputs())) continue;
            return true;
        } 
        return false;
    }
    
    public boolean verifyPenaltyPending(Penalty penalty) throws IOException, FileNotFoundException, ClassNotFoundException {
        StakeContract contract = session.getBlockFileHandler().getStakeContract(penalty.getStakeHash());
        if (contract == null) return false;
        float reward = getReward(penalty.getTimestamp());
        if (!penalty.getTransaction().getInputs().get(0).previousTxnHash.equals(DigestUtils.sha256Hex("Penalty"))) return false;
        if (!penalty.getTransaction().getInputs().get(0).outputIndex.equals(0)) return false;
        if (Float.compare(penalty.getTransaction().sum(), -reward) != 0) return false;
        ArrayList<TransactionOutput> checkOutputs = session.getBlockchain().generatePenaltyOutputs(contract,penalty.getTimestamp());
        if (!checkOutputs.equals(penalty.getTransaction().getOutputs())) return false;
        return true;
    }
    
    public void setValidateHeader(boolean x) {this.validateHeader = x;}
    public float getStakeRequirement() {return this.stakeRequirement;}
    public float getReward(String timestamp) {return (float)(int) (20 * (1/Math.max(2*((int)(((Long.valueOf(timestamp)/1000) - 1577836800)/315360000)),1)));}
}
