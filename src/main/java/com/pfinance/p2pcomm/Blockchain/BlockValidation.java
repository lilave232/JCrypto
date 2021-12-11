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
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;

/**
 *
 * @author averypozzobon
 */
public class BlockValidation {
    BigDecimal stakeRequirement = new BigDecimal(10);
    boolean validateHeader = false;
    Session session = null;
    ArrayList<String> blockUtxosUsed = new ArrayList<>();
    BigDecimal reward = BigDecimal.ZERO;
            
    public BlockValidation(Session session) {
        this.session = session;
        //this.currentReward = (float)(int) (20 * (1/Math.max(2*((int)(((Long.valueOf(system)/1000) - 1577836800)/315360000)),1)));
        //this.stakeRequirement = (float) (session.getBlockchain().getTotalFloat() * 0.001);
    }
    
    public void setStakeRequirement(BigDecimal value) {
        this.stakeRequirement = value;
    }
    
    public boolean validate(Block block, String previousBlock) {
        synchronized (this.session) {
            System.out.println(String.valueOf(System.currentTimeMillis()) + ": Validating block " + block.getHash());
            blockUtxosUsed = new ArrayList<String>();
            reward = BigDecimal.ZERO;
            try {
                if (validateHeader) {
                    if (session.getPeer().getHandler().isRequested(block.getHash())) {}
                    else {
                        Long scheduledTime = this.session.getScheduler().getMineTime(block.getStakeContractHash());
                        Long endScheduledTime = this.session.getScheduler().getEndTime();
                        Long currentTime = System.currentTimeMillis();
                        if (scheduledTime == 0) return false;
                        System.out.println("scheduledTime not equal to zero");
                        //if (!(Long.valueOf(block.getTimestamp()) > currentTime - 60000 && Long.valueOf(block.getTimestamp()) < currentTime + 60000)) return false;
                        if (!((currentTime >= scheduledTime && currentTime <= scheduledTime + 180000))) return false;
                        System.out.println("Scheduled Time is Accurate");
                    }
                    System.out.println("Validating Header");
                    if (!block.getHash().equals(DigestUtils.sha256Hex(block.getPreviousBlockHash()+block.getStakeContractHash()+block.getTimestamp()+block.hashData())))
                        return false;
                    System.out.println("Block Hash is Equal");
                    if (!block.getPreviousBlockHash().equals(previousBlock)) return false;
                    System.out.println("Previous Block Hash Is Equal");
                    if (!verifyStakeHash(block.getStakeContractHash())) return false;
                    System.out.println("Stake Hash Verified");
                }
                for (Object obj : block.getData().subList(1, block.getData().size())) {
                    if (obj instanceof Transaction){if (!verifyTransaction((Transaction) obj)) return false;}
                    else if (obj instanceof BorrowContract) {if (!verifyBorrowContract((BorrowContract) obj)) return false;}   
                    else if (obj instanceof LendContract) {if (!verifyLendContract((LendContract) obj)) return false;}
                    else if (obj instanceof StakeContract) {if (!verifyStakeContract((StakeContract) obj)) return false;} 
                    else if (obj instanceof Penalty) {
                        if (session.getPeer().getHandler().isRequested(block.getHash())){ if (!verifyPenaltyPending((Penalty) (obj))) return false;}
                        else {if (!verifyPenalty((Penalty)(obj))) return false;}
                    }
                    else if (obj instanceof NFT) {if (!verifyNFT((NFT) obj)) return false;}
                    else if (obj instanceof NFTTransfer) {if (!verifyNFTTransfer((NFTTransfer) obj)) return false;}
                    else if (obj instanceof ListNFT) {if (!verifyListNFT((ListNFT) obj)) return false;}
                    else if (obj instanceof Bid) {if (!verifyBid((Bid) obj)) return false;}
                    else if (obj instanceof DelistNFT) {if (!verifyDeListNFT((DelistNFT) obj)) return false;}
                    else {return false;}
                }
                System.out.println("Confirmed Transactions");
                if (!verifyBase((Transaction) block.data.get(0))) return false;
                System.out.println("Confimed Base Transaction");
                return true;
            } catch (Exception e){e.printStackTrace();return false;}
        }
        
    }
    
    public boolean verifyStakeHash(String hash) throws IOException, ClassNotFoundException {
        Validator validator = session.getValidators().getValidator(hash);
        if (validator == null) return false;
        return true;
    }
    
    public boolean verifyBase(Transaction transaction) {
        try {
            reward = getReward(transaction.getTimestamp()).add(reward);
            if (!transaction.getInputs().get(0).previousTxnHash.equals(DigestUtils.sha256Hex("Base"))
                || !transaction.getInputs().get(0).outputIndex.equals(0)
                || reward.compareTo(transaction.sum()) != 0) {return false;}
            return true;
        } catch (Exception e) {e.printStackTrace();return false;}
    }
    
    public boolean verifyTransaction(Transaction transaction) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        ArrayList<TransactionInput> inputs = transaction.getInputs();
        ArrayList<TransactionOutput> outputs = transaction.getOutputs();
        BigDecimal inputSum = BigDecimal.ZERO;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (utxo == null) return false;
            if (!Cryptography.verify(input.outputSignature, DigestUtils.sha256Hex(utxo.getPreviousHash() + utxo.getIndex().toString()).getBytes(),input.getKey())) return false;
            if (!DigestUtils.sha256Hex(input.getKey().toByteArray()).equals(utxo.getAddress())) return false;
            inputSum = inputSum.add(utxo.toFloat());
        }
        reward = reward.add(inputSum.subtract(transaction.sum()));
        //if (BigInteger.compare(inputSum, transaction.sum()) != 0) return false;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (blockUtxosUsed.contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) return false;
            blockUtxosUsed.add(utxo.getPreviousHash() + "|" + utxo.getIndex());
        }
        return true;
    }
    
    public boolean verifyNFT(NFT contract) throws ClassNotFoundException, Exception {
        String hash = DigestUtils.sha256Hex(contract.getInceptionDate()+contract.getInitiatorAddress() + DigestUtils.sha256Hex(contract.getData()) + contract.getMintFee().getHash());
        if (!contract.getHash().equals(hash)) return false;
        if (new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + hash + "/nft") != null) return false;
        if (!verifyTransaction(contract.getMintFee())) return false;
        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
        return true;
    }
    
    public boolean verifyNFTTransfer(NFTTransfer transfer) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        //System.out.println("Confirming NFT Transfer");
        String hash = "";
        if (transfer.getSaleTransaction() != null) {
            hash = DigestUtils.sha256Hex(transfer.getDate() + transfer.getNFTHash() + transfer.getPreviousHash() + transfer.getTransferAddress() + transfer.getSaleTransaction().getHash());
        }
        if (transfer.getBidTransaction() != null) {
            hash = DigestUtils.sha256Hex(transfer.getDate() + transfer.getNFTHash() + transfer.getPreviousHash() + transfer.getTransferAddress() + transfer.getBidTransaction().getHash());
        }
        if (!transfer.getHash().equals(hash)) return false;
        //System.out.println("Hash Confirmed");
        NFT nft = (NFT) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + transfer.getNFTHash() + "/nft");
        if (nft == null) return false;
        //System.out.println("NFT Found");
        HashIndex index = (HashIndex) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + transfer.getNFTHash() + "/hashIndex");
        if (index == null) return false;
        //System.out.println("Hash Index Found");
        String previousHash = index.getHashes().get(index.getHashes().size()-1).hash;
        if (!previousHash.equals(transfer.getPreviousHash())) return false;
        //System.out.println("Previous Hash Equal");
        if (index.getHashes().size() == 1) {
            if (!nft.getInitiatorAddress().equals(DigestUtils.sha256Hex(transfer.getKey().toByteArray()))) return false;
            //System.out.println("Initiator Address Equals");
            if (!Cryptography.verify(nft.getSignature(),nft.getHash().getBytes(),transfer.getKey())) return false;
            //System.out.println("Ownership Confirmed");
        } else {
            NFTTransfer previousTransfer = (NFTTransfer) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + transfer.getNFTHash() + "/" + previousHash);
            if (previousTransfer == null) return false;
            //System.out.println("Previous Transfer Found");
            if (!previousTransfer.getTransferAddress().equals(DigestUtils.sha256Hex(transfer.getKey().toByteArray()))) return false;
            //System.out.println("Confirmed Previous Transfer was to Address");
        }
        if (transfer.getSaleTransaction() != null) {
            if (!verifyTransaction(transfer.getSaleTransaction())) return false;
        } else if (transfer.getBidTransaction() != null) {
            if (!verifyBidSale(transfer.getBidTransaction())) return false;
            if (transfer.getBidTransaction().getTransaction().getInputs().size() > 0) {
                if (!transfer.getTransferAddress().equals(DigestUtils.sha256Hex(transfer.getBidTransaction().getKey().toByteArray()))) return false;
            }
        }
        
        //System.out.println("Sale Transaction Verified");
        if (!Cryptography.verify(transfer.getSignature(),hash.getBytes(),transfer.getKey())) return false;
        //System.out.println("Ownership Verified");
        return true;
    }
    
    public boolean verifyListNFT(ListNFT listNFT) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        
        String hash = DigestUtils.sha256Hex(listNFT.getTimestamp() + listNFT.getNFTHash() + listNFT.getValidatorCommission().getHash());
        if (!listNFT.getHash().equals(hash)) return false;
        //System.out.println("Hash is Equal");
        NFT nft = (NFT) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + listNFT.getNFTHash() + "/nft");
        if (nft == null) return false;
        //System.out.println("Found NFT");
        HashIndex index = (HashIndex) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + listNFT.getNFTHash() + "/hashIndex");
        if (index == null) return false;
        //System.out.println("Obtained Index");
        if (index.getHashes().size() == 1) {
            if (!nft.getInitiatorAddress().equals(DigestUtils.sha256Hex(listNFT.getKey().toByteArray()))) return false;
        //    System.out.println("Verified Owner Address");
            if (!Cryptography.verify(nft.getSignature(),nft.getHash().getBytes(),listNFT.getKey())) return false;
        //    System.out.println("Verified Ownership");
        } else {
            NFTTransfer previousTransfer = (NFTTransfer) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + listNFT.getNFTHash() + "/" + index.getHashes().get(index.getHashes().size()-1).hash);
            if (previousTransfer == null) return false;
            if (!previousTransfer.getTransferAddress().equals(DigestUtils.sha256Hex(listNFT.getKey().toByteArray()))) return false;
        //    System.out.println("Verified Owner Address");
        }
        if (!verifyTransaction(listNFT.getValidatorCommission())) return false;
        //System.out.println("Commission Transaction Verified");
        if (!Cryptography.verify(listNFT.getSignature(),hash.getBytes(),listNFT.getKey())) return false;
        //System.out.println("Listing Verified");
        return true;
    }
    
    public boolean verifyDeListNFT(DelistNFT delistNFT) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        String hash = DigestUtils.sha256Hex(delistNFT.getTimestamp() + delistNFT.getNFTHash());
        if (!delistNFT.getHash().equals(hash)) return false;
        //System.out.println("Hash is Equal");
        NFT nft = (NFT) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + delistNFT.getNFTHash() + "/nft");
        if (nft == null) return false;
        ListNFT listNFT = (ListNFT) new FileHandler().readObject(session.getPath() + "/contracts/listNFTs/" + delistNFT.getNFTHash() + "/nft");
        if (!Cryptography.verify(listNFT.getSignature(), listNFT.getHash().getBytes(), delistNFT.getKey())) return false;
        //System.out.println("Commission Transaction Verified");
        if (!Cryptography.verify(delistNFT.getSignature(),hash.getBytes(),delistNFT.getKey())) return false;
        //System.out.println("Listing Verified");
        return true;
    }
    
    public boolean verifyBidSale(Bid bid) throws IOException, FileNotFoundException, ClassNotFoundException {
        //System.out.println("Verifying Bid");
        String hash = DigestUtils.sha256Hex(bid.getTimestamp()+bid.getContractHash()+bid.getTransaction().getHash());
        if (!bid.getHash().equals(hash)) return false;
        //System.out.println("Hash Equal");
        ArrayList<TransactionInput> inputs = bid.getTransaction().getInputs();
        ArrayList<TransactionOutput> outputs = bid.getTransaction().getOutputs();
        BigDecimal inputSum = BigDecimal.ZERO;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/held_utxos/" + bid.getContractHash() + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (utxo == null) return false;
            if (!DigestUtils.sha256Hex(input.getKey().toByteArray()).equals(utxo.getAddress())) return false;
            inputSum = inputSum.add(utxo.toFloat());
        }
        //System.out.println("Inputs Verified");
        reward = reward.add(inputSum.subtract(bid.getTransaction().sum()));
        //if (BigInteger.compare(inputSum, transaction.sum()) != 0) return false;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/held_utxos/" + bid.getContractHash() + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (blockUtxosUsed.contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) return false;
            blockUtxosUsed.add(utxo.getPreviousHash() + "|" + utxo.getIndex());
        }
        //System.out.println("Outputs Verified");
        NFT nft = (NFT) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + bid.getContractHash() + "/nft");
        if (nft == null) return false;
        //System.out.println("NFT Exists");
        ListNFT lnft = (ListNFT) new FileHandler().readObject(session.getPath() + "/contracts/listNFTs/" + bid.getContractHash() + "/nft");
        if (lnft == null) return false;
        //System.out.println("NFT Listed");
        if (!Cryptography.verify(bid.getSignature(), hash.getBytes(), bid.getKey())) return false;
        //System.out.println("Signature Verified");
        return true;
    }
    
    public boolean verifyBid(Bid bid) throws IOException, FileNotFoundException, ClassNotFoundException {
        //System.out.println("Verifying Bid");
        Bid pendingBid = (Bid) new FileHandler().readObject(session.getPath() + "/contracts/listNFTs/" + bid.getContractHash() + "/bids/" + bid.getHash());
        if (pendingBid != null) {return verifyBidSale(bid);}
        String hash = DigestUtils.sha256Hex(bid.getTimestamp()+bid.getContractHash()+bid.getTransaction().getHash());
        if (!bid.getHash().equals(hash)) return false;
        //System.out.println("Hash Equal");
        ArrayList<TransactionInput> inputs = bid.getTransaction().getInputs();
        ArrayList<TransactionOutput> outputs = bid.getTransaction().getOutputs();
        BigDecimal inputSum = BigDecimal.ZERO;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (utxo == null) return false;
            if (!DigestUtils.sha256Hex(input.getKey().toByteArray()).equals(utxo.getAddress())) return false;
            inputSum = inputSum.add(utxo.toFloat());
        }
        //System.out.println("Inputs Verified");
        //reward += inputSum - bid.getTransaction().sum();
        //if (BigInteger.compare(inputSum, transaction.sum()) != 0) return false;
        for (TransactionInput input : inputs) {
            UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (blockUtxosUsed.contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) return false;
            blockUtxosUsed.add(utxo.getPreviousHash() + "|" + utxo.getIndex());
        }
        //System.out.println("Outputs Verified");
        NFT nft = (NFT) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + bid.getContractHash() + "/nft");
        if (nft == null) return false;
        //System.out.println("NFT Exists");
        ListNFT lnft = (ListNFT) new FileHandler().readObject(session.getPath() + "/contracts/listNFTs/" + bid.getContractHash() + "/nft");
        if (lnft == null) return false;
        //System.out.println("NFT Listed");
        if (!Cryptography.verify(bid.getSignature(), hash.getBytes(), bid.getKey())) return false;
        //System.out.println("Signature Verified");
        return true;
    }
    
    public boolean verifyBorrowContract(BorrowContract contract) throws ClassNotFoundException, Exception {
        String hash = DigestUtils.sha256Hex(contract.getInceptionDate()+contract.getBorrowerAddress()+contract.getValidatorCommission().getHash());
        if (!contract.getHash().equals(hash)) return false;
        if (!verifyTransaction(contract.getValidatorCommission())) return false;
        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
        return true;
    }
    
    public boolean verifyLendContract(LendContract contract) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        BorrowContract bcontract = session.getBlockFileHandler().getBorrowContract(contract.getBorrowContractHash());
        String hash = DigestUtils.sha256Hex(contract.getInceptionDate()+contract.getLenderAddress()+contract.getBorrowContractHash()+contract.getLendTransaction().getHash());
        if (!contract.getHash().equals(hash)) return false;
        if (bcontract == null) return false;
        if (!verifyTransaction(contract.getLendTransaction())) return false;
        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
        return true;
    }
    
    public boolean verifyStakeContract(StakeContract contract) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        BorrowContract bcontract = session.getBlockFileHandler().getBorrowContract(contract.getBorrowContractHash());
        String hash = DigestUtils.sha256Hex(contract.getInceptionDate() + contract.getBorrowContractHash() + contract.getValidatorCommission().getHash());
        if (!contract.getHash().equals(hash)) return false;
        if (bcontract == null) return false;
        if (!verifyTransaction(contract.getValidatorCommission())) return false;
        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
        return true;
    }
    
    public boolean verifyEndLendContract(EndLendContract contract) throws IOException, ClassNotFoundException {
//        System.out.println("Verifying End Lend Contract");
//        BorrowContract bcontract = session.getBlockFileHandler().getBorrowContract(contract.getBorrowContractHash());
//        LendContract lcontract = session.getBlockFileHandler().loadLendContract(session.getPath() + "/contracts/borrow/" + 
//                                                                                contract.getBorrowContractHash() + "/lendContracts/" +
//                                                                                contract.getLendContractHash());
//        
//        String hash = DigestUtils.sha256Hex(contract.getTimestamp() + contract.getLendContractHash() + contract.getBorrowContractHash() + contract.getValidatorCommission().getHash());
//        if (!contract.getHash().equals(hash)) return false;
//        System.out.println("Hash Confirmed");
//        if (bcontract == null) return false;
//        System.out.println("Borrow Found");
//        if (lcontract == null) return false;
//        System.out.println("Lend Found");
//        if (!Cryptography.verify(contract.getSignature(),hash.getBytes(),contract.getKey())) return false;
//        System.out.println("Contract Signature Verified");
//        if (!Cryptography.verify(lcontract.getSignature(),lcontract.getHash().getBytes(),contract.getKey())) return false;
//        System.out.println("Previous Contract Signature Verified");
        return false;
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
        BigDecimal reward = getReward(penalty.getTimestamp());
        if (!penalty.getTransaction().getInputs().get(0).previousTxnHash.equals(DigestUtils.sha256Hex("Penalty"))) return false;
        if (!penalty.getTransaction().getInputs().get(0).outputIndex.equals(0)) return false;
        if (penalty.getTransaction().sum().compareTo(reward) != 0) return false;
        ArrayList<TransactionOutput> checkOutputs = session.getBlockchain().generatePenaltyOutputs(contract,penalty.getTimestamp());
        if (!checkOutputs.equals(penalty.getTransaction().getOutputs())) return false;
        return true;
    }
    
    public void setValidateHeader(boolean x) {this.validateHeader = x;}
    public BigDecimal getStakeRequirement() {return this.stakeRequirement;}
    public BigDecimal getReward(String timestamp) {return new BigDecimal((int) (20 * (1/Math.max(2*((int)(((Long.valueOf(timestamp)/1000) - 1577836800)/315360000)),1))));}
}
