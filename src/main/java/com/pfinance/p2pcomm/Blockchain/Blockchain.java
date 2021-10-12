/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Blockchain;

import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.FileHandler.*;
import com.pfinance.p2pcomm.Transaction.*;
import com.pfinance.p2pcomm.Contracts.*;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Wallet.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class Blockchain {
    public Block block = null;
    BlockValidation blockValidator = null;
    private HashIndex index = new HashIndex();
    private Session session = null;
    private float totalFloat;
    private ArrayList<String> pendingUTXOs = new ArrayList<>();
    
    public Blockchain(Session session) {
        try {
            this.session = session;
            this.blockValidator = session.getBlockValidator();
        }
        catch (Exception e){e.printStackTrace();}
    }
    
    public void load() throws IOException, ClassNotFoundException {
        loadIndex();
        loadAllBlocks();
        if (this.index.getHashes().size() > 3) this.blockValidator.setValidateHeader(true);
    }
    
    public void initialize() {
        try {
            Wallet wallet = this.session.getWallet();
            Key key = wallet.getKey();
            //Genesis Block 1
            if (this.index.getHashes().size() < 1) {
                newBlock("0000000000000000",key);
                BorrowContract bcontract = wallet.createBorrowContract();
                addData(bcontract);
                addBlock(this.block);
            }
            //Genesis Block 2
            if (this.index.getHashes().size() < 2) {
                newBlock("0000000000000000",key);
                wallet.loadBorrowContract();
                LendContract lcontract = wallet.createLendContract(wallet.getBorrowContract(), 10);
                addData(lcontract);
                addBlock(this.block);
            }
            //Genesis Block 3
            if (this.index.getHashes().size() < 3) {
                newBlock("0000000000000000",key);
                wallet.loadBorrowContract();
                StakeContract scontract = wallet.createStakeContract();
                addData(scontract);
                addBlock(this.block);
            }
        } catch (Exception e) {e.printStackTrace();}
    }
    
    public void newBlock(String stakeHash, Key key) throws ClassNotFoundException, Exception {
        String time = Long.toString(System.currentTimeMillis());
        if (session.getPeer() != null && session.getValidation()) {
            session.getScheduler().clearSchedule();
            session.getScheduler().schedule();
            time = Long.toString(this.session.getScheduler().getMineTime(stakeHash));
            if (System.currentTimeMillis() > Long.valueOf(time)) time = Long.toString(System.currentTimeMillis());
        }
        ArrayList<TransactionOutput> outputs = session.getWallet().getBaseOutputs(time);
        if (this.index.getHashes().isEmpty())
            block = new Block(DigestUtils.sha256Hex("Genesis Block"),stakeHash,time,key.getAddress(),outputs,key.getKey().getPublicKey());
        else
            block = new Block(this.index.getHashes().get(this.index.getHashes().size()-1).hash,stakeHash,time,key.getAddress(),outputs, key.getKey().getPublicKey());
        
        session.getBlockFileHandler().loadPendingObjects();
        if (session.getPeer() != null && session.getValidation()) {
            if (!session.getMiner().isNull()) session.getMiner().stopMiner();
            session.getPeer().sendMessage(Message.REQUESTPENDING, Json.createObjectBuilder().add("data", Message.REQUESTPENDING).build());
            session.getMiner().startMiner();
        }
            
    }
    
    public synchronized boolean verifyBlock(Block block) throws IOException {
        String previousBlock = DigestUtils.sha256Hex("Genesis Block");
        if (this.index.getHashes().size() > 0) previousBlock = this.index.getHashes().get(this.index.getHashes().size()-1).hash;
        return this.blockValidator.validate(block, previousBlock);
    }
    
    public synchronized void addBlock(Block block) throws IOException, ClassNotFoundException, Exception {
        if (verifyBlock(block)) {
            if (block.data.get(0) instanceof Transaction) {
                Transaction reward = (Transaction) block.data.get(0);
                this.totalFloat += reward.sum();
            }
            this.index.addHash(new HashEntry(this.index.getHashes().size(),block.getHash()));
            session.getBlockFileHandler().saveBlock(block);
            saveIndex();
            saveFloat();
            System.out.println("Block Saved");
        }
        if (this.index.getHashes().size() > 3) this.blockValidator.setValidateHeader(true);
        if (this.session.getValidation()) newBlock(this.session.getWallet().getStakeContract().getHash(),this.session.getWallet().getKey());
        else this.block = null;
    }
    
    public boolean verifyTransaction(Object object) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        if (object instanceof Transaction) {
            if (this.blockValidator.verifyTransaction((Transaction) object)) {block.addData(object); return true;}
        } else if (object instanceof BorrowContract) {
            if (this.blockValidator.verifyBorrowContract((BorrowContract) object)) {block.addData(object); return true;}
        } else if (object instanceof LendContract) {
            if (this.blockValidator.verifyLendContract((LendContract) object)) {block.addData(object); return true;}
        } else if (object instanceof StakeContract) {
            if (this.blockValidator.verifyStakeContract((StakeContract) object)) {block.addData(object); return true;}
        } else if (object instanceof Penalty) {
            if (this.blockValidator.verifyPenalty((Penalty) object)){block.addData(object);return true;}
        }
        return false;
    }
    
    public boolean addData(Object object) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        boolean flag = false;
        if (this.block == null) return false;
        if (verifyTransaction(object)) {flag = true;}
        return flag;
    }
    
    public void addPendingTxn(Object data) throws IOException, ClassNotFoundException, FileNotFoundException, Exception {
        if (data instanceof Transaction) {addPending((Transaction) data);}
        else if (data instanceof BorrowContract) {addPending(((BorrowContract) data).getValidatorCommission());}
        else if (data instanceof LendContract) {addPending(((LendContract) data).getLendTransaction());}
        else if (data instanceof StakeContract) {addPending(((StakeContract) data).getValidatorCommission());}
        else if (data instanceof Penalty) {if (!this.blockValidator.verifyPenaltyPending((Penalty) data)) return;}
        session.getBlockFileHandler().savePendingObject(data);
    }
    
    public void addPending(Transaction transaction) throws IOException, FileNotFoundException, ClassNotFoundException {
        for (TransactionInput input : transaction.getInputs()) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (!this.pendingUTXOs.contains(utxo.getPreviousHash() + "|" + utxo.getIndex()))
                this.pendingUTXOs.add(utxo.getPreviousHash() + "|" + utxo.getIndex());
        }
    }
    
    public Penalty generatePenalty(StakeContract contract, String timestamp) {
        Transaction transaction = new Transaction();
        transaction.setTimestamp(timestamp);
        TransactionInput baseInput = new TransactionInput(DigestUtils.sha256Hex("Penalty"),0,DigestUtils.sha256("Penalty"),null);
        ArrayList<TransactionOutput> outputs = generatePenaltyOutputs(contract,timestamp);
        for (TransactionOutput output : outputs) {
            transaction.addOutput(output);
        }
        transaction.addInput(baseInput);
        Penalty penalty = new Penalty(contract.getHash(),timestamp,transaction);
        return penalty;
    }
    
    public ArrayList<TransactionOutput> generatePenaltyOutputs(StakeContract stakeContract, String timestamp) {
        ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        try {
            File f = new File(session.getPath() + "/contracts/borrow/" + stakeContract.getBorrowContractHash() + "/lendContracts");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile();
                }
            });
            if (files != null) {
                HashMap<String,Float> outputValues = new HashMap<>();
                for (File file : files) {
                    LendContract contract = this.session.getBlockFileHandler().loadLendContract(file.getPath());
                    if (contract != null) {
                        if (contract.getLendTransaction().getOutputs().size() > 0) {
                            Validator user = session.getValidators().getValidator(stakeContract.getHash());
                            TransactionOutput loutput = contract.getLendTransaction().getOutputs().get(0);
                            TransactionOutput baseOutput = new TransactionOutput(contract.getLenderAddress(),-(loutput.value/user.getBalance())*session.getBlockValidator().getReward(timestamp));
                            outputValues.put(baseOutput.address, outputValues.getOrDefault(baseOutput.address, (float)0) + baseOutput.value);
                        }
                    }
                }
                outputValues.forEach((k,v)-> {
                    TransactionOutput output = new TransactionOutput(k,v);
                    outputs.add(output);
                });
            }
            return outputs;
        } catch (Exception e) {
            return outputs;
        } 
    }
    
    public void loadAllBlocks() throws IOException, FileNotFoundException, ClassNotFoundException {
        boolean loadFloat = true;
        Object obj = new FileHandler().readObject(session.getPath() + "/totalFloat");
        if (obj instanceof Float) {
            this.totalFloat = (Float)obj;
            loadFloat = false;
        }
        for (HashEntry entry : this.index.getHashes()) {
            Block b = session.getBlockFileHandler().getBlock(entry.hash);
            if (b != null) {
                if (loadFloat == false) continue;
                if (b.data.get(0) instanceof Transaction) {
                    Transaction reward = (Transaction) b.data.get(0);
                    this.totalFloat += reward.sum();
                }
            }
        }
        this.blockValidator.setStakeRequirement(this.totalFloat * (float)0.001);
    }
    
    public void saveIndex() throws IOException {
        FileHandler handler = new FileHandler();
        handler.writeBytes(session.getPath() + "/blocks/index",index.toBytes());
    }
    
    public void loadIndex() throws IOException, FileNotFoundException, ClassNotFoundException {
        HashIndex hashes = (HashIndex) new FileHandler().readObject(session.getPath() + "/blocks/index");
        if (hashes != null) this.index = hashes;
    }
    
    public void saveFloat() throws IOException {
        new FileHandler().writeObject(session.getPath() + "/totalFloat", this.totalFloat);
    }
    
    public String toString() {
        StringBuilder returnString = new StringBuilder();
        for (int i = 0; i < this.index.getHashes().size(); i++) {
            try {
                returnString.append(session.getBlockFileHandler().getBlock(this.index.getHashes().get(i).hash).toString()).append("\n");
            } catch (Exception e) {}
        }
        return returnString.toString();
    }
    
    public Integer size() {return this.index.getHashes().size();}
    public HashIndex getHashIndex() {return this.index;}
    public ArrayList<String> getPendingUTXOs() {return this.pendingUTXOs;}
    public Float getTotalFloat() {return this.totalFloat;}
    public void setHashIndex(HashIndex index) throws IOException {this.index = index; saveIndex();}
}
