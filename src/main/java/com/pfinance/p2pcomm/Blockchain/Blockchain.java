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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;
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
    private BigDecimal totalFloat = BigDecimal.ZERO;
    private BigDecimal feePerByte = BigDecimal.ZERO;
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
            session.setMinFee(BigDecimal.ZERO);
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
                LendContract lcontract = wallet.createLendContract(wallet.getBorrowContract(), new BigDecimal(10));
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
            session.minFee = null;
        } catch (Exception e) {e.printStackTrace();}
    }
    
    public void newBlock(String stakeHash, Key key) throws ClassNotFoundException, Exception {
        this.blockValidator.blockUtxosUsed = new ArrayList<>();
        String time = Long.toString(System.currentTimeMillis());
        if (session.getPeer() != null && session.getValidation()) {
            session.getScheduler().clearSchedule();
            session.getScheduler().schedule();
            time = Long.toString(this.session.getScheduler().getMineTime(stakeHash));
            if (System.currentTimeMillis() > Long.valueOf(time)) time = Long.toString(System.currentTimeMillis());
        }
        ArrayList<TransactionOutput> outputs = session.getWallet().getBaseOutputs(time);
        if (this.index.getHashes().isEmpty())
            block = new Block(DigestUtils.sha256Hex("Genesis Block"),stakeHash,time,key.getAddress(),outputs,key);
        else
            block = new Block(this.index.getHashes().get(this.index.getHashes().size()-1).hash,stakeHash,time,key.getAddress(),outputs, key);
        
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
            System.out.println("Block Verified");
            if (block.data.get(0) instanceof Transaction) {
                Transaction reward = (Transaction) block.data.get(0);
                this.totalFloat = this.totalFloat.add(reward.sum());
            }
            session.getBlockFileHandler().saveBlock(block);
            System.out.println("Block Saved");
            this.index.addHash(new HashEntry(this.index.getHashes().size(),block.getHash()));
            System.out.println("Hash Added");
            saveIndex();
            System.out.println("Index Saved");
            saveFloat();
            this.blockValidator.setStakeRequirement(this.totalFloat.multiply(new BigDecimal(0.001)));
            System.out.println("BigDecimal Saved");
            System.out.println("Finished Adding New Block");
        }
        if (this.index.getHashes().size() > 3) this.blockValidator.setValidateHeader(true);
        if (this.session.getValidation()) newBlock(this.session.getWallet().getStakeContract().getHash(),this.session.getWallet().getKey());
        else this.block = null;
    }
    
    public boolean checkFee(Object object, int size) {
        Transaction transaction = getTransaction(object);
        if (transaction == null) return false;
        try {
            BigDecimal fee = getFee(transaction);
            if (fee.compareTo(this.feePerByte.multiply(new BigDecimal(size))) == -1) {return false;}
            else {return true;}
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean verifyTransaction(Object object) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        if (object instanceof Transaction) {
            if (!checkFee(object,((Transaction) object).toBytes().length))return false;
            System.out.println("Fee Okay!");
            if (this.blockValidator.verifyTransaction((Transaction) object)) {block.addData(object, getFee((Transaction) object)); return true;}
        } else if (object instanceof BorrowContract) {
            if (!checkFee(object,((BorrowContract) object).toBytes().length))return false;
            if (this.blockValidator.verifyBorrowContract((BorrowContract) object)) {block.addData(object, getFee(((BorrowContract) object).getValidatorCommission())); return true;}
        } else if (object instanceof LendContract) {
            if (!checkFee(object,((LendContract) object).toBytes().length))return false;
            if (this.blockValidator.verifyLendContract((LendContract) object)) {block.addData(object, getFee(((LendContract) object).getLendTransaction())); return true;}
        } else if (object instanceof StakeContract) {
            if (this.session.getBlockFileHandler().getStakeContract(((StakeContract) object).getHash()) != null) {return false;}
            if (!checkFee(object,((StakeContract) object).toBytes().length))return false;
            if (this.blockValidator.verifyStakeContract((StakeContract) object)) {block.addData(object, getFee(((StakeContract) object).getValidatorCommission())); return true;}
        } else if (object instanceof Penalty) {
            if (this.blockValidator.verifyPenalty((Penalty) object)){block.addData(object, BigDecimal.ZERO);return true;}
        } else if (object instanceof NFT) {
            if (!checkFee(object,((NFT) object).toBytes().length))return false;
            if (this.blockValidator.verifyNFT((NFT) object)){block.addData(object,getFee(((NFT) object).getMintFee()));return true;}
        } else if (object instanceof NFTTransfer) {
            if (session.getBlockchain().block.data.contains(object)) return false;
            if (this.blockValidator.verifyNFTTransfer((NFTTransfer) object)){
                if (((NFTTransfer) object).getSaleTransaction() != null) {
                    block.addData(object,getFee(((NFTTransfer) object).getSaleTransaction()));
                } else if (((NFTTransfer) object).getBidTransaction() != null) {
                    block.addData(object,getHeldFee(((NFTTransfer) object).getBidTransaction().getTransaction(),((NFTTransfer) object).getNFTHash()));
                } else {
                    return false;
                }
                return true;
            }
        } else if (object instanceof EndLendContract) {
            if (!checkFee(object,((EndLendContract) object).toBytes().length))return false;
            if (this.blockValidator.verifyEndLendContract((EndLendContract)object)) {block.addData(object, getFee(((EndLendContract) object).getValidatorCommission()));return true;}
        } else if (object instanceof ListNFT) {
            if (!checkFee(object,((ListNFT) object).toBytes().length))return false;
            if (this.session.getBlockFileHandler().getPendingObject(((ListNFT) object).getNFTHash()) != null) return false;
            if (this.blockValidator.verifyListNFT((ListNFT)object)) {block.addData(object, getFee(((ListNFT) object).getValidatorCommission()));return true;}
        } else if (object instanceof Bid) {
            if (!checkFee(object,((Bid) object).toBytes().length))return false;
            if (this.session.getBlockFileHandler().getPendingObject(((Bid) object).getContractHash()) != null) return false;
            if (this.blockValidator.verifyBid((Bid)object)) {block.addData(object, BigDecimal.ZERO); return true;}
        } else if (object instanceof DelistNFT) {
            if (this.session.getBlockFileHandler().getPendingObject(((DelistNFT) object).getNFTHash()) != null) return false;
            if (this.blockValidator.verifyDeListNFT((DelistNFT)object)) {block.addData(object, BigDecimal.ZERO); return true;}
        }
        return false;
    }
    
    public BigDecimal getHeldFee(Transaction transaction, String contractHash) throws IOException, ClassNotFoundException {
        BigDecimal inputSum = sumHeldInputs(transaction.getInputs(),contractHash);
        BigDecimal fee = inputSum.subtract(transaction.sum());
        return fee;
    }
    
    public BigDecimal sumHeldInputs(ArrayList<TransactionInput> inputs, String contractHash) throws IOException, ClassNotFoundException, FileNotFoundException {
        BigDecimal inputSum = BigDecimal.ZERO;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (utxo == null) return BigDecimal.ZERO;
            //if (!Cryptography.verify(input.outputSignature, utxo.getAddress().getBytes(),input.getKey())) return BigDecimal.ZERO;
            //if (!DigestUtils.sha256Hex(input.getKey().toByteArray()).equals(utxo.getAddress())) return 0;
            inputSum = inputSum.add(utxo.toFloat());
        }
        return inputSum;
    }
    
    public Transaction getTransaction (Object object) {
        if (object instanceof Transaction) {
            return (Transaction)object;
        } else if (object instanceof BorrowContract) {
            return ((BorrowContract) object).getValidatorCommission();
        } else if (object instanceof LendContract) {
            return ((LendContract) object).getLendTransaction();
        } else if (object instanceof StakeContract) {
            return ((StakeContract) object).getValidatorCommission();
        } else if (object instanceof NFT) {
            return ((NFT) object).getMintFee();
        } else if (object instanceof Bid) {
            return ((Bid) object).getTransaction();
        } else if (object instanceof EndLendContract) {
            return ((EndLendContract) object).getValidatorCommission();
        } else if (object instanceof ListNFT) {
            return ((ListNFT) object).getValidatorCommission();
        }
        return null;
    }
    
    
    public BigDecimal getFee(Transaction transaction) throws IOException, ClassNotFoundException {
        BigDecimal inputSum = sumInputs(transaction.getInputs());
        BigDecimal fee = inputSum.subtract(transaction.sum());
        return fee;
    }
    
    public BigDecimal sumInputs(ArrayList<TransactionInput> inputs) throws IOException, ClassNotFoundException, FileNotFoundException {
        BigDecimal inputSum = BigDecimal.ZERO;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (utxo == null) return BigDecimal.ZERO;
            //if (!Cryptography.verify(input.outputSignature, utxo.getAddress().getBytes(),input.getKey())) return BigDecimal.ZERO;
            //if (!DigestUtils.sha256Hex(input.getKey().toByteArray()).equals(utxo.getAddress())) return BigDecimal.ZERO;
            inputSum = inputSum.add(utxo.toFloat());
        }
        return inputSum;
    }
    
    public boolean addData(Object object) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        synchronized (this.session) {
            boolean flag = false;
            if (this.block == null) return false;
            if (verifyTransaction(object)) {flag = true;}
            return flag;
        }
        
    }
    
    public void addPendingTxn(Object data) throws IOException, ClassNotFoundException, FileNotFoundException, Exception {
        if (data instanceof Transaction) {addPending((Transaction) data);}
        else if (data instanceof BorrowContract) {addPending(((BorrowContract) data).getValidatorCommission());}
        else if (data instanceof LendContract) {addPending(((LendContract) data).getLendTransaction());}
        else if (data instanceof StakeContract) {addPending(((StakeContract) data).getValidatorCommission());}
        else if (data instanceof Penalty) {if (!this.blockValidator.verifyPenaltyPending((Penalty) data)) return;}
        else if (data instanceof NFT) {addPending(((NFT) data).getMintFee());}
        else if (data instanceof NFTTransfer) {
            if (((NFTTransfer) data).getSaleTransaction() != null) addPending(((NFTTransfer) data).getSaleTransaction());
            else if (((NFTTransfer) data).getBidTransaction() != null) addPending(((NFTTransfer) data).getBidTransaction().getTransaction());
        }
        else if (data instanceof EndLendContract) {addPending(((EndLendContract) data).getValidatorCommission());}
        else if (data instanceof ListNFT) {addPending(((ListNFT) data).getValidatorCommission());}
        else if (data instanceof Bid) {addPending(((Bid) data).getTransaction());}
        session.getBlockFileHandler().savePendingObject(data);
    }
    
    public void addPending(Transaction transaction) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        for (TransactionInput input : transaction.getInputs()) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (utxo == null) {
                return;
            }
            if (!this.pendingUTXOs.contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) {
                this.pendingUTXOs.add(utxo.getPreviousHash() + "|" + utxo.getIndex());
            }
        }
    }
    
    public Penalty generatePenalty(StakeContract contract, Key key, String timestamp) {
        Transaction transaction = new Transaction(timestamp, key);
        TransactionInput baseInput = new TransactionInput(DigestUtils.sha256Hex("Penalty"),0);
        ArrayList<TransactionOutput> outputs = generatePenaltyOutputs(contract,timestamp);
        for (TransactionOutput output : outputs) {
            transaction.addOutput(output,key);
        }
        transaction.addInput(baseInput,key);
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
                HashMap<String,BigDecimal> outputValues = new HashMap<>();
                for (File file : files) {
                    LendContract contract = this.session.getBlockFileHandler().loadLendContract(file.getPath());
                    if (contract != null) {
                        if (contract.getLendTransaction().getOutputs().size() > 0) {
                            Validator user = session.getValidators().getValidator(stakeContract.getHash());
                            TransactionOutput loutput = contract.getLendTransaction().getOutputs().get(0);
                            TransactionOutput baseOutput = new TransactionOutput(contract.getLenderAddress(),(loutput.value.divide(user.getBalance(),MathContext.DECIMAL32)).multiply(session.getBlockValidator().getReward(timestamp)).negate());
                            outputValues.put(baseOutput.address, outputValues.getOrDefault(baseOutput.address, BigDecimal.ZERO).add(baseOutput.value));
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
        if (obj instanceof BigDecimal) {
            this.totalFloat = (BigDecimal)obj;
            loadFloat = false;
        }
        ArrayList<HashEntry> not_found = new ArrayList<>();
        for (HashEntry entry : this.index.getHashes()) {
            Block b = session.getBlockFileHandler().getBlock(entry.hash);
            if (b != null) {
                if (loadFloat == false) continue;
                if (b.data.get(0) instanceof Transaction) {
                    Transaction reward = (Transaction) b.data.get(0);
                    this.totalFloat = this.totalFloat.add(reward.sum());
                }
            } else {
                not_found.add(entry);
            }
        }
        this.index.removeAllHashes(not_found);
        saveIndex();
        this.blockValidator.setStakeRequirement(this.totalFloat.multiply(new BigDecimal(0.001)));
    }
    
    public void saveIndex() {
        try {
            FileHandler handler = new FileHandler();
            handler.writeBytes(session.getPath() + "/blocks/index",index.toBytes()); 
        } catch (IOException e) {}
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
    
    public void setFee(BigDecimal fee) {this.feePerByte = fee;session.minFee = fee;System.out.println("Fee Set To: " + this.feePerByte);}
    public BigDecimal getFeePerByte() {return this.feePerByte;}
    
    public Integer size() {return this.index.getHashes().size();}
    public HashIndex getHashIndex() {return this.index;}
    public ArrayList<String> getPendingUTXOs() {return this.pendingUTXOs;}
    public BigDecimal getTotalFloat() {return this.totalFloat;}
    public void setHashIndex(HashIndex index) throws IOException {this.index = index; saveIndex();}
}
