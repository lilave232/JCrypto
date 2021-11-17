/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Blockchain;

import com.pfinance.p2pcomm.Contracts.BorrowContract;
import com.pfinance.p2pcomm.Contracts.EndLendContract;
import com.pfinance.p2pcomm.Contracts.LendContract;
import com.pfinance.p2pcomm.Contracts.ListNFT;
import com.pfinance.p2pcomm.Contracts.NFT;
import com.pfinance.p2pcomm.Contracts.NFTTransfer;
import com.pfinance.p2pcomm.Contracts.StakeContract;
import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.FileHandler.HashEntry;
import com.pfinance.p2pcomm.FileHandler.HashIndex;
import com.pfinance.p2pcomm.FileHandler.Validator;
import com.pfinance.p2pcomm.FileHandler.ValidatorIndex;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.P2P.Server.ServerThread;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Bid;
import com.pfinance.p2pcomm.Transaction.Penalty;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import com.pfinance.p2pcomm.Wallet.Wallet;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
public class BlockFiles {
    
    private Session session = null;
    private boolean lentFundsUpdated = false;
    private ArrayList<String> walletAddresses = new ArrayList<>();
    
    public BlockFiles(Session session) {
        this.session = session;
    }
    
        
    public void loadValidators() throws IOException, FileNotFoundException, ClassNotFoundException {
        ValidatorIndex index = (ValidatorIndex) new FileHandler().readObject(session.getPath() + "/validators");
        if (index != null) session.setValidators(index);
    }
    
    public void saveValidators() throws IOException {
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/validators",session.getValidators());
    }
    
    
    public String[] getBlockchains() throws IOException {
        File f = new File(System.getProperty("user.dir") + "/blockchains/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        try {
            String[] chains = new String[files.length];
            for (int x = 0; x < files.length; x++) {
                chains[x] = files[x].getName();
            }
            return chains;
        } catch (NullPointerException e) {
            return new String[0];
        }    
    }
    
    public ArrayList<String> getBlocks() throws IOException {
        File f = new File(session.getPath() + "/blocks/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        try {
            ArrayList<String> blocks = new ArrayList<String>();
            for (int x = 0; x < files.length; x++) {
                blocks.add(files[x].getName());
            }
            return blocks;
        } catch (NullPointerException e) {
            return new ArrayList<>();
        } 
    }
    
    public String[] getWallets() {
        File f = new File(session.getPath() + "/wallets/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.getName().equals("utxos") && !file.getName().equals("contracts");
            }
        });
        try {
          String[] wallets = new String[files.length];
            for (int x = 0; x < files.length; x++) {
                wallets[x] = files[x].getName();
            }  
            return wallets;
        } catch (NullPointerException e) {
            return new String[0];
        }
    }
    
    public ArrayList<String> getAddresses() throws IOException {
        String[] wallets = this.getWallets();
        ArrayList<String> addresses = new ArrayList<>();
        ArrayList<String> stakeAddresses = new ArrayList<>();
        FileHandler handler = new FileHandler();
        for (String wallet : wallets) {
            addresses.add(new String(handler.readBytes(session.getPath() + "/wallets/" + wallet + "/address"),"UTF-8"));
        }
        this.walletAddresses = addresses;
        return addresses;
    }
    
    public ArrayList<String> getWalletAddresses() {return this.walletAddresses;}
    
    public String getWalletPath(String address) throws IOException {
        String[] files = this.getWallets();
        FileHandler handler = new FileHandler();
        for (int x = 0; x < files.length; x++) {
            String addr = new String(handler.readBytes(session.getPath() + "/wallets/" + files[x] + "/address"),"UTF-8");
            if (addr.equals(address)) return session.getPath() + "/wallets/" + files[x];
        }
        return null;
    }
    
    public String[] getStakeContracts() {
        File f = new File(session.getPath() + "/contracts/stake");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        String[] contracts = new String[files.length];
        for (int x = 0; x < files.length; x++) {
            contracts[x] = files[x].getName();
        }
        return contracts;
    }
    
    public String[] getBorrowContracts() {
        File f = new File(session.getPath() + "/contracts/borrow");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        String[] contracts = new String[files.length];
        for (int x = 0; x < files.length; x++) {
            contracts[x] = files[x].getName();
        }
        return contracts;
    }
    
    public BorrowContract[] getBorrowContractObjects() throws IOException, ClassNotFoundException, IOException {
        File f = new File(session.getPath() + "/contracts/borrow");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        BorrowContract[] contracts = new BorrowContract[files.length];
        for (int x = 0; x < files.length; x++) {
            contracts[x] = getBorrowContract(files[x].getName());
        }
        return contracts;
    }
    
    public BorrowContract getBorrowContract(String hash) throws IOException, FileNotFoundException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        return (BorrowContract) handler.readObject(session.getPath() + "/contracts/borrow/" + hash + "/contract");
    }
    
    public BorrowContract getBorrowContract(String hash, String address) throws IOException, FileNotFoundException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        String path = this.getWalletPath(address);
        return (BorrowContract) handler.readObject(path + "/contracts/borrow/contract");
    }
    
    public StakeContract getStakeContract(String hash, String address)  throws IOException, FileNotFoundException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        String path = this.getWalletPath(address);
        return (StakeContract) handler.readObject(path + "/contracts/stake/contract");
    }
    
    public StakeContract getStakeContract(String hash)  throws IOException, FileNotFoundException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        return (StakeContract) handler.readObject(session.getPath() + "/contracts/stake/" + hash + "/contract");
    }
    
    public float getBorrowedBalance(Validator validator, String address) throws IOException, FileNotFoundException, ClassNotFoundException {
        StakeContract contract = getStakeContract(validator.getStakeHash(), address);
        if (contract == null) return 0;
        float returnValue = getBorrowBalance(contract.getBorrowContractHash(),address) + getPenaltyBalance(contract.getHash());
        return returnValue;
    }
    
    public float getBorrowedBalance(Validator validator) throws IOException, FileNotFoundException, ClassNotFoundException {
        StakeContract contract = getStakeContract(validator.getStakeHash());
        if (contract == null) return 0;
        float returnValue = getBorrowBalance(contract.getBorrowContractHash()) + getPenaltyBalance(contract.getHash());
        return returnValue;
    }
    
    public float getBorrowBalance(String hash, String address) throws IOException, FileNotFoundException, ClassNotFoundException {
        float returnValue = 0;
        FileHandler handler = new FileHandler();
        String path = this.getWalletPath(address);
        File f = new File(path + "/contracts/borrow/lentFunds");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        for (int x = 0; x < files.length; x++) {
            UTXO utxo = loadUTXO(files[x].getPath());
            returnValue += utxo.toFloat();
        }
        return returnValue;
    }
     
    public float getBorrowBalance(String hash) throws IOException, FileNotFoundException, ClassNotFoundException {
        float returnValue = 0;
        FileHandler handler = new FileHandler();
        File f = new File(session.getPath() + "/contracts/borrow/" + hash + "/lentFunds");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        for (int x = 0; x < files.length; x++) {
            UTXO utxo = loadUTXO(files[x].getPath());
            returnValue += utxo.toFloat();
        }
        return returnValue;
    }
    
    public float getPenaltyBalance(String hash) throws IOException, FileNotFoundException, ClassNotFoundException {
        float returnValue = 0;
        FileHandler handler = new FileHandler();
        File f = new File(session.getPath() + "/contracts/stake/" + hash + "/penalties");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return 0;
        for (int x = 0; x < files.length; x++) {
            Penalty penalty = (Penalty) handler.readObject(files[x].getPath());
            if (penalty != null) returnValue += penalty.getTransaction().sum();
        }
        return returnValue;
    }
    
    public float getPenaltyBalance(String hash,String address) throws IOException, FileNotFoundException, ClassNotFoundException {
        float returnValue = 0;
        FileHandler handler = new FileHandler();
        String path = this.getWalletPath(address);
        File f = new File(path + "/contracts/stake/penalties");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return 0;
        for (int x = 0; x < files.length; x++) {
            Penalty penalty = (Penalty) handler.readObject(files[x].getPath());
            if (penalty != null) returnValue += penalty.getTransaction().sum();
        }
        return returnValue;
    }
    
    public UTXO loadUTXO(String path) throws IOException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        return (UTXO) handler.readObject(path);
    }
    
    public void saveUTXO(Transaction transaction, String address) throws IOException {
        int index = 0;
        FileHandler handler = new FileHandler();
        for (TransactionOutput output : transaction.getOutputs()) {
            UTXO utxo = new UTXO(output,transaction.getTimestamp(),transaction.getHash(),index,null);
            if (address.equals(output.address)) {
                String path = this.getWalletPath(output.address);
                Files.createDirectories(Paths.get(path + "/utxos/"));
                handler.writeObject(path + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(index), utxo);
            }
            index += 1;
        }
    }
    
    public void saveUTXO(Transaction transaction) throws IOException {
        Files.createDirectories(Paths.get(session.getPath() + "/utxos/"));
        int index = 0;
        FileHandler handler = new FileHandler();
        for (TransactionOutput output : transaction.getOutputs()) {
            UTXO utxo = new UTXO(output,transaction.getTimestamp(),transaction.getHash(),index,null);
            handler.writeObject(session.getPath() + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(index), utxo);
            if (this.walletAddresses.contains(output.address)) {
                String path = this.getWalletPath(output.address);
                Files.createDirectories(Paths.get(path + "/utxos/"));
                handler.writeObject(path + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(index), utxo);
            }
            index += 1;
        }
    }
    
    public void holdUTXO(Transaction transaction, String contractHash) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        for (TransactionInput input : transaction.getInputs()) {
            Files.createDirectories(Paths.get(session.getPath() + "/held_utxos/" + contractHash + "/"));
            UTXO utxo = this.loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex()))
                session.getBlockchain().getPendingUTXOs().remove(utxo.getPreviousHash() + "|" + utxo.getIndex());
            File f = new File(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            handler.deleteFile(f.getPath());
            utxo.setOut(transaction.getTimestamp(), transaction.getHash());
            handler.writeObject(session.getPath() + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            if (this.walletAddresses.contains(utxo.getAddress())) {
                String path = this.getWalletPath(utxo.getAddress());
                Files.createDirectories(Paths.get(path + "/held_utxos/" + contractHash + "/"));
                File f_wallet = new File(path + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
                handler.deleteFile(f_wallet.getPath());
                handler.writeObject(path + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            }
        }
    }
    
    public void holdUTXO(Transaction transaction, String contractHash, String address) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        for (TransactionInput input : transaction.getInputs()) {
            UTXO utxo = this.loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            utxo.setOut(transaction.getTimestamp(), transaction.getHash());
            handler.writeObject(session.getPath() + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            if (address.equals(utxo.getAddress())) {
                String path = this.getWalletPath(address);
                Files.createDirectories(Paths.get(path + "/held_utxos/" + contractHash + "/"));
                File f_wallet = new File(path + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
                handler.deleteFile(f_wallet.getPath());
                handler.writeObject(path + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            }
        }
    }
    
    public void deleteUTXO(Transaction transaction, String address) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        for (TransactionInput input : transaction.getInputs()) {
            String path = this.getWalletPath(address);
            UTXO utxo = this.loadUTXO(path + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex()))
                session.getBlockchain().getPendingUTXOs().remove(utxo.getPreviousHash() + "|" + utxo.getIndex());
            if (address.equals(utxo.getAddress())) {
                Files.createDirectories(Paths.get(path + "/used_utxos/"));
                File f_wallet = new File(path + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
                handler.deleteFile(f_wallet.getPath());
                utxo.setOut(transaction.getTimestamp(), transaction.getHash());
                handler.writeObject(path + "/used_utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            }
        }
    }
    
    public void deleteUTXO(Transaction transaction) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        for (TransactionInput input : transaction.getInputs()) {
            Files.createDirectories(Paths.get(session.getPath() + "/used_utxos/"));
            UTXO utxo = this.loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex()))
                session.getBlockchain().getPendingUTXOs().remove(utxo.getPreviousHash() + "|" + utxo.getIndex());
            File f = new File(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            handler.deleteFile(f.getPath());
            utxo.setOut(transaction.getTimestamp(), transaction.getHash());
            handler.writeObject(session.getPath() + "/used_utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            if (this.walletAddresses.contains(utxo.getAddress())) {
                String path = this.getWalletPath(utxo.getAddress());
                Files.createDirectories(Paths.get(path + "/used_utxos/"));
                File f_wallet = new File(path + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
                handler.deleteFile(f_wallet.getPath());
                utxo.setOut(transaction.getTimestamp(), transaction.getHash());
                handler.writeObject(path + "/used_utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            }
        }
    }
    
    public void releaseHeldUTXO(String contractHash) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        File f = new File(session.getPath() + "/held_utxos/" + contractHash + "/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null);
        for (File file : files) {
            UTXO utxo = this.loadUTXO(file.getPath());
            handler.renameFile(file.getPath(), session.getPath() + "/utxos/" + file.getName());
            if (this.walletAddresses.contains(utxo.getAddress())) {
                String path = this.getWalletPath(utxo.getAddress());
                handler.renameFile(path + "/held_utxos/" + contractHash + "/" + file.getName(), path + "/utxos/" + file.getName());
                if (file == files[files.length-1]) {
                    handler.deleteFolder(path + "/held_utxos/" + contractHash + "/");
                }
            }
        }
        handler.deleteFolder(f.getPath());
    }
    
    public void deleteHeldUTXO(Transaction transaction, String contractHash) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        for (TransactionInput input : transaction.getInputs()) {
            UTXO utxo = this.loadUTXO(session.getPath() + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex()))
                session.getBlockchain().getPendingUTXOs().remove(utxo.getPreviousHash() + "|" + utxo.getIndex());
            File f = new File(session.getPath() + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            handler.deleteFile(f.getPath());
            if (this.walletAddresses.contains(utxo.getAddress())) {
                String path = this.getWalletPath(utxo.getAddress());
                Files.createDirectories(Paths.get(path + "/used_utxos/"));
                File f_wallet = new File(path + "/held_utxos/" + contractHash + "/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
                handler.deleteFile(f_wallet.getPath());
                utxo.setOut(transaction.getTimestamp(), transaction.getHash());
                handler.writeObject(path + "/used_utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex), utxo);
            }
        }
    }
    
    public void saveUTXOLend(Transaction transaction, BorrowContract contract, String address) throws IOException {
        FileHandler handler = new FileHandler();
        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            TransactionOutput output = transaction.getOutputs().get(i);
            UTXO utxo = new UTXO(output,transaction.getTimestamp(),transaction.getHash(),i,null);
            if (i == 0 && output.address.equals(contract.getBorrowerAddress())) {   
                //IF YOU ARE THE BORROWER
                if (address.equals(output.address)) {
                    String path = this.getWalletPath(output.address);
                    Files.createDirectories(Paths.get(path + "/contracts/borrow/lentFunds"));
                    handler.writeObject(path + "/contracts/borrow/lentFunds/" + utxo.getPreviousHash() + "|" + String.valueOf(i), utxo);
                    this.lentFundsUpdated = true;
                }
            } else {
                if (address.equals(output.address)) {
                    String path = this.getWalletPath(output.address);
                    Files.createDirectories(Paths.get(path + "/utxos/"));
                    handler.writeObject(path + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(i), utxo);
                }
            }
        }
    }
    
    public void saveUTXOLend(Transaction transaction, BorrowContract contract) throws IOException {
        FileHandler handler = new FileHandler();
        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            TransactionOutput output = transaction.getOutputs().get(i);
            UTXO utxo = new UTXO(output,transaction.getTimestamp(),transaction.getHash(),i,null);
            if (i == 0 && output.address.equals(contract.getBorrowerAddress())) {
                Files.createDirectories(Paths.get(session.getPath() + "/contracts/borrow/" + contract.getHash() + "/lentFunds"));
                handler.writeObject(session.getPath() + "/contracts/borrow/" + contract.getHash() + "/lentFunds/" +  utxo.getPreviousHash() + "|" + String.valueOf(i), utxo);   
                //IF YOU ARE THE BORROWER
                if (this.walletAddresses.contains(output.address)) {
                    String path = this.getWalletPath(output.address);
                    Files.createDirectories(Paths.get(path + "/contracts/borrow/lentFunds"));
                    handler.writeObject(path + "/contracts/borrow/lentFunds/" + utxo.getPreviousHash() + "|" + String.valueOf(i), utxo);
                    this.lentFundsUpdated = true;
                }
            } else {
                handler.writeObject(session.getPath() + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(i), utxo);
                if (this.walletAddresses.contains(output.address)) {
                    String path = this.getWalletPath(output.address);
                    Files.createDirectories(Paths.get(path + "/utxos/"));
                    handler.writeObject(path + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(i), utxo);
                }
            }
        }
    }
    
    public void saveStakeContract(StakeContract stakeContract, String address) throws IOException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        if (address.equals(stakeContract.getAddress())) {
            String path = getWalletPath(stakeContract.getAddress());
            Files.createDirectories(Paths.get(path + "/contracts/stake"));
            handler.writeObject(path + "/contracts/stake/contract", stakeContract);
        }
    }
    
    public void saveStakeContract(StakeContract stakeContract) throws IOException, FileNotFoundException, ClassNotFoundException {
        Files.createDirectories(Paths.get(session.getPath() + "/contracts/stake/" + stakeContract.getHash()));
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/contracts/stake/" + stakeContract.getHash() + "/contract", stakeContract);
        if (getWalletAddresses().contains(stakeContract.getAddress())) {
            String path = getWalletPath(stakeContract.getAddress());
            Files.createDirectories(Paths.get(path + "/contracts/stake"));
            handler.writeObject(path + "/contracts/stake/contract", stakeContract);
        }
        Validator validator = new Validator(stakeContract.getHash(),stakeContract.getBorrowContractHash());
        validator.setBalance(getBorrowedBalance(validator));
        session.getValidators().addValidator(validator);
        saveValidators();
    }
    
    public ArrayList<TransactionOutput> getLentFunds(String hash) {
        ArrayList<TransactionOutput> returnArray = new ArrayList<>();
        File f = new File(session.getPath() + "/contracts/borrow/" + hash + "/lendContracts");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files != null) {
            for (File file : files) {
                LendContract contract = loadLendContract(file.getPath());
                if (contract != null) {
                    if (contract.getLendTransaction().getOutputs().size() > 0) {
                        returnArray.add(new TransactionOutput(contract.getLenderAddress(),contract.getLendTransaction().getOutputs().get(0).value));
                    }
                }
            }
        }
        return returnArray;
    }
    
    public ArrayList<TransactionOutput> getPenaltyOutputs(String hash) {
        ArrayList<TransactionOutput> returnArray = new ArrayList<>();
        File f = new File(session.getPath() + "/contracts/stake/" + hash + "/penalties");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files != null) {
            for (File file : files) {
                Penalty penalty = loadPenalty(file.getPath());
                if (penalty != null) {
                    returnArray.addAll(penalty.getTransaction().getOutputs());
                }
                
            }
        }
        return returnArray;
    }
    
    public ArrayList<NFT> getListedNFTs() throws IOException {
        ArrayList<NFT> returnArray = new ArrayList<>();
        File f = new File(session.getPath() + "/contracts/listNFTs");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (files != null) {
            for (File file : files) {
                NFT nft = loadNFT(session.getPath() + "/contracts/nfts/" + file.getName() + "/nft");
                if (nft != null) {
                    returnArray.add(nft);
                }
                
            }
        }
        returnArray.sort((o1, o2) -> o1.getInceptionDate().compareTo(o2.getInceptionDate()));
        return returnArray;
    }
    
    public ArrayList<NFT> getWalletNFTs(String address) throws IOException {
        ArrayList<NFT> returnArray = new ArrayList<>();
        String path = getWalletPath(address);
        File f = new File(path + "/contracts/nfts");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (files != null) {
            for (File file : files) {
                NFT nft = loadNFT(file.getPath() + "/nft");
                if (nft != null) {
                    returnArray.add(nft);
                }
                
            }
        }
        returnArray.sort((o1, o2) -> o1.getInceptionDate().compareTo(o2.getInceptionDate()));
        return returnArray;
    }
    
    public NFT loadNFT(String path) {
        try {
            FileHandler handler = new FileHandler();
            return (NFT) handler.readObject(path);
        } catch (Exception e) {} 
        return null;
    }
    
    public Penalty loadPenalty(String path) {
        try {
            FileHandler handler = new FileHandler();
            return (Penalty) handler.readObject(path);
        } catch (Exception e) {} 
        return null;
    }
    
    public LendContract loadLendContract(String path) {
        try {
            FileHandler handler = new FileHandler();
            return (LendContract) handler.readObject(path);
        } catch (Exception e) {} 
        return null;
    }
    
    public void saveLendContract(LendContract lendContract, String address) throws IOException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        if (address.equals(lendContract.getLenderAddress())) {
            String path = this.getWalletPath(lendContract.getLenderAddress());
            Files.createDirectories(Paths.get(path + "/contracts/lendContracts"));
            handler.writeObject(path + "/contracts/lendContracts/" + lendContract.getHash(), lendContract);
        }
    }
    
    public void saveNFTList(ListNFT object, String address) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        String currentOwner = getNFTOwner(object.getNFTHash());
        if (address.equals(currentOwner)) {
            String path = this.getWalletPath(address);
            Files.createDirectories(Paths.get(path + "/contracts/listNFTs/" + object.getNFTHash()));
            handler.writeObject(path + "/contracts/listNFTs/" + object.getNFTHash() + "/nft", object);
        }
    }
    
    public void saveLendContract(LendContract lendContract) throws IOException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        Files.createDirectories(Paths.get(session.getPath() + "/contracts/borrow/" + lendContract.getBorrowContractHash() + "/lendContracts"));
        handler.writeObject(session.getPath() + "/contracts/borrow/" + lendContract.getBorrowContractHash() + "/lendContracts/" + lendContract.getHash(), lendContract);
        if (this.walletAddresses.contains(lendContract.getLenderAddress())) {
            String path = this.getWalletPath(lendContract.getLenderAddress());
            Files.createDirectories(Paths.get(path + "/contracts/lendContracts"));
            handler.writeObject(path + "/contracts/lendContracts/" + lendContract.getHash(), lendContract);
        }
        Validator validator = session.getValidators().getValidatorByBorrow(lendContract.getBorrowContractHash());
        if (validator != null) {
            validator.setBalance(getBorrowedBalance(validator));
            saveValidators();
        }
    }
    
    public void saveBorrowContract(BorrowContract borrowContract, String address) throws IOException {
        FileHandler handler = new FileHandler();
        if (address.equals(borrowContract.getBorrowerAddress())) {
            String path = this.getWalletPath(borrowContract.getBorrowerAddress());
            Files.createDirectories(Paths.get(path + "/contracts/borrow"));
            handler.writeObject(path + "/contracts/borrow/contract", borrowContract);
        }
    }
    
    public void saveBorrowContract(BorrowContract borrowContract) throws IOException {
        Files.createDirectories(Paths.get(session.getPath() + "/contracts/borrow/" + borrowContract.getHash()));
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/contracts/borrow/" + borrowContract.getHash() + "/contract", borrowContract);
        if (this.getWalletAddresses().contains(borrowContract.getBorrowerAddress())) {
            String path = this.getWalletPath(borrowContract.getBorrowerAddress());
            Files.createDirectories(Paths.get(path + "/contracts/borrow"));
            handler.writeObject(path + "/contracts/borrow/contract", borrowContract);
        }
    }
    
    public void saveNFT(NFT object) throws IOException {
        Files.createDirectories(Paths.get(session.getPath() + "/contracts/nfts/" + object.getHash()));
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/contracts/nfts/" + object.getHash() + "/nft", object);
        HashIndex hashIndex = new HashIndex();
        hashIndex.addHash(new HashEntry(0,object.getHash()));
        handler.writeObject(session.getPath() + "/contracts/nfts/" + object.getHash() + "/hashIndex", hashIndex);
        if (this.getWalletAddresses().contains(object.getInitiatorAddress())) {
            String path = this.getWalletPath(object.getInitiatorAddress());
            Files.createDirectories(Paths.get(path + "/contracts/nfts/" + object.getHash()));
            handler.writeObject(path + "/contracts/nfts/" + object.getHash() + "/nft", object);
        }
    }
    
    public String getNFTOwner(String nftHash) throws IOException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        HashIndex hashIndex = (HashIndex) handler.readObject(session.getPath() + "/contracts/nfts/" + nftHash + "/hashIndex");
        if (hashIndex == null) return null;
        if (hashIndex.getHashes().size() == 1) {
            NFT nft = (NFT) handler.readObject(session.getPath() + "/contracts/nfts/" + nftHash + "/nft");
            return nft.getInitiatorAddress();
        } else if (hashIndex.getHashes().size() > 1) {
            String previousHash = hashIndex.getHashes().get(hashIndex.getHashes().size()-1).hash;
            NFTTransfer transfer = (NFTTransfer) handler.readObject(session.getPath() + "/contracts/nfts/" + nftHash + "/" + previousHash);
            return transfer.getTransferAddress();
        }
        return null;
    }
    
    public void saveNFTTransfer(NFTTransfer object) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/contracts/nfts/" + object.getNFTHash() + "/" + object.getHash(), object);
        HashIndex hashIndex = (HashIndex) handler.readObject(session.getPath() + "/contracts/nfts/" + object.getNFTHash() + "/hashIndex");
        if (hashIndex == null) return;
        String currentOwner = getNFTOwner(object.getNFTHash());
        hashIndex.addHash(new HashEntry(hashIndex.getHashes().size(),object.getHash()));
        handler.writeObject(session.getPath() + "/contracts/nfts/" + object.getNFTHash() + "/hashIndex", hashIndex);
        handler.deleteFolder(session.getPath() + "/contracts/listNFTs/" + object.getNFTHash());
        if (this.getWalletAddresses().contains(currentOwner)) {
            String path = this.getWalletPath(currentOwner);
            handler.deleteFile(path + "/contracts/nfts/" + object.getNFTHash() + "/nft");
            handler.deleteFolder(path + "/contracts/listNFTs/" + object.getNFTHash());
        }
        if (this.getWalletAddresses().contains(object.getTransferAddress())) {
            String path = this.getWalletPath(object.getTransferAddress());
            NFT nft = (NFT) handler.readObject(session.getPath() + "/contracts/nfts/" + object.getNFTHash() + "/nft");
            if (nft == null) return;
            Files.createDirectories(Paths.get(path + "/contracts/nfts/" + object.getNFTHash()));
            handler.writeObject(path + "/contracts/nfts/" + object.getNFTHash() + "/nft", nft);
        }
    }
    
    public void saveNFTTransfer(NFTTransfer object,String address) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        String currentOwner = getNFTOwner(object.getNFTHash());
        if (address.equals(currentOwner)) {
            String path = this.getWalletPath(address);
            handler.deleteFile(path + "/contracts/nfts/" + object.getNFTHash() + "/nft");
            handler.deleteFolder(path + "/contracts/listNFTs/" + object.getNFTHash());
        }
        if (address.equals(object.getTransferAddress())) {
            String path = this.getWalletPath(object.getTransferAddress());
            NFT nft = (NFT) handler.readObject(session.getPath() + "/contracts/nfts/" + object.getNFTHash() + "/nft");
            if (nft == null) return;
            Files.createDirectories(Paths.get(path + "/contracts/nfts/" + object.getNFTHash()));
            handler.writeObject(path + "/contracts/nfts/" + object.getNFTHash() + "/nft", nft);
        }
    }
    
    public void saveNFTList(ListNFT object) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        Files.createDirectories(Paths.get(session.getPath() + "/contracts/listNFTs/" + object.getNFTHash()));
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/contracts/listNFTs/" + object.getNFTHash() + "/nft", object);
        String currentOwner = getNFTOwner(object.getNFTHash());
        if (this.getWalletAddresses().contains(currentOwner)) {
            String path = this.getWalletPath(currentOwner);
            Files.createDirectories(Paths.get(path + "/contracts/listNFTs/" + object.getNFTHash()));
            handler.writeObject(path + "/contracts/listNFTs/" + object.getNFTHash() + "/nft", object);
        }
    }
    
    public void saveBid(Bid bid) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        Files.createDirectories(Paths.get(session.getPath() + "/contracts/listNFTs/" + bid.getContractHash() + "/bids/"));
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/contracts/listNFTs/" + bid.getContractHash() + "/bids/" + bid.getHash(), bid);
        String currentOwner = getNFTOwner(bid.getContractHash());
        if (this.getWalletAddresses().contains(currentOwner)) {
            String path = this.getWalletPath(currentOwner);
            Files.createDirectories(Paths.get(path + "/contracts/listNFTs/" + bid.getContractHash() + "/bids/"));
            handler.writeObject(path + "/contracts/listNFTs/" + bid.getContractHash() + "/bids/" + bid.getHash(), bid);
        }
    }
    
    public void saveBid(Bid bid,String address) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        String currentOwner = getNFTOwner(bid.getContractHash());
        if (address.equals(currentOwner)) {
            String path = this.getWalletPath(address);
            Files.createDirectories(Paths.get(path + "/contracts/listNFTs/" + bid.getContractHash() + "/bids/"));
            handler.writeObject(path + "/contracts/listNFTs/" + bid.getContractHash() + "/bids/" + bid.getHash(), bid);
        }
    }
    
    public void savePenalty(Penalty penalty, String address) throws IOException, FileNotFoundException, ClassNotFoundException {
        StakeContract contract = this.getStakeContract(penalty.getStakeHash(),address);
        if (contract == null) return;
        FileHandler handler = new FileHandler();
        if (address.equals(contract.getAddress())) {
            String path = this.getWalletPath(contract.getAddress());
            Files.createDirectories(Paths.get(path + "/contracts/stake/penalties"));
            handler.writeObject(path + "/contracts/stake/penalties/" + penalty.getHash(), penalty);
            this.lentFundsUpdated = true;
        }
    }
    
    public void savePenalty(Penalty penalty) throws IOException, FileNotFoundException, ClassNotFoundException {
        StakeContract contract = this.getStakeContract(penalty.getStakeHash());
        if (contract == null) return;
        FileHandler handler = new FileHandler();
        Files.createDirectories(Paths.get(session.getPath() + "/contracts/stake/" + penalty.getStakeHash() + "/penalties"));
        handler.writeObject(session.getPath() + "/contracts/stake/" + penalty.getStakeHash() + "/penalties/" + penalty.getHash(), penalty);
        if (this.walletAddresses.contains(contract.getAddress())) {
            String path = this.getWalletPath(contract.getAddress());
            Files.createDirectories(Paths.get(path + "/contracts/stake/penalties"));
            handler.writeObject(path + "/contracts/stake/penalties/" + penalty.getHash(), penalty);
            this.lentFundsUpdated = true;
        }
        Validator validator = session.getValidators().getValidator(penalty.getStakeHash());
        if (validator != null) {
            validator.setBalance(getBorrowedBalance(validator));
            saveValidators();
        }
    }
    
    public void saveBlock(Block block) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        Files.createDirectories(Paths.get(session.getPath() + "/blocks/"));
        System.out.println("Creating Directory If Non-Existent");
        FileHandler handler = new FileHandler();
        for (int i = 0; i < block.data.size(); i++) {
            try {
                Object data = block.data.get(i);
                if (i == 0) {
                    saveUTXO((Transaction) data);
                } else if (data instanceof Transaction) {
                    saveUTXO((Transaction) data);
                    deleteUTXO((Transaction) data);
                } else if (data instanceof BorrowContract) {
                    saveBorrowContract((BorrowContract) data);
                    saveUTXO(((BorrowContract) data).getValidatorCommission());
                    deleteUTXO(((BorrowContract) data).getValidatorCommission());
                } else if (data instanceof LendContract) {
                    BorrowContract bcontract = getBorrowContract(((LendContract) data).getBorrowContractHash());
                    saveUTXOLend(((LendContract) data).getLendTransaction(),bcontract);
                    deleteUTXO(((LendContract) data).getLendTransaction());
                    saveLendContract((LendContract) data);
                } else if (data instanceof StakeContract) {
                    saveStakeContract((StakeContract) data);
                    saveUTXO(((StakeContract) data).getValidatorCommission());
                    deleteUTXO(((StakeContract) data).getValidatorCommission());
                } else if (data instanceof Penalty) {
                    savePenalty((Penalty) data);
                } else if (data instanceof NFT) {
                    saveNFT((NFT) data);
                    saveUTXO(((NFT) data).getMintFee());
                    deleteUTXO(((NFT) data).getMintFee());
                } else if (data instanceof NFTTransfer) {
                    saveNFTTransfer((NFTTransfer) data);
                    if (((NFTTransfer) data).getSaleTransaction() != null) {
                        saveUTXO(((NFTTransfer) data).getSaleTransaction());
                        deleteUTXO(((NFTTransfer) data).getSaleTransaction());
                    }
                    if (((NFTTransfer) data).getBidTransaction() != null) {
                        saveUTXO(((NFTTransfer) data).getBidTransaction().getTransaction());
                        deleteHeldUTXO(((NFTTransfer) data).getBidTransaction().getTransaction(), ((NFTTransfer) data).getNFTHash());
                    }
                    releaseHeldUTXO(((NFTTransfer) data).getNFTHash());
                } else if (data instanceof ListNFT) {
                    saveNFTList((ListNFT) data);
                    saveUTXO(((ListNFT) data).getValidatorCommission());
                    deleteUTXO(((ListNFT) data).getValidatorCommission());
                } else if (data instanceof Bid) {
                    saveBid((Bid) data);
                    holdUTXO(((Bid) data).getTransaction(),((Bid) data).getContractHash());
                    //saveUTXO(((Bid) data).getTransaction());
                    //deleteUTXO(((Bid) data).getTransaction());
                }
                deletePendingObject(data);
                if (this.lentFundsUpdated) {
                    this.session.getWallet().generateBaseOutputs(String.valueOf(System.currentTimeMillis()));
                }
            } catch (Exception e) {}
        }
        System.out.println("Added Transactions");
        handler.writeObject(session.getPath() + "/blocks/" + block.getHash(),block);
        System.out.println("Added Block");
        this.lentFundsUpdated = false;
    }
    
    public void indexWallet(String address) {
        session.getBlockchain().getHashIndex().getHashes().forEach(hash -> {
            try {
                Block block = getBlock(hash.hash);
                for (int i = 0; i < block.data.size(); i++) {
                    Object data = block.data.get(i);
                    if (i == 0) {
                        try { saveUTXO((Transaction) data,address);} catch (Exception e) {}
                    } else if (data instanceof Transaction) {
                        try { saveUTXO((Transaction) data,address); } catch (Exception e) {}
                        try { deleteUTXO((Transaction) data,address); } catch (Exception e) {}
                    } else if (data instanceof BorrowContract) {
                        try { saveBorrowContract((BorrowContract) data, address);} catch (Exception e) {}
                        try { saveUTXO(((BorrowContract) data).getValidatorCommission(),address);} catch (Exception e) {}
                        try { deleteUTXO(((BorrowContract) data).getValidatorCommission(),address);} catch (Exception e) {}
                    } else if (data instanceof LendContract) {
                        try { 
                            BorrowContract bcontract = getBorrowContract(((LendContract) data).getBorrowContractHash(),address);
                            saveUTXOLend(((LendContract) data).getLendTransaction(),bcontract,address);
                        } catch (Exception e) {}
                        try { deleteUTXO(((LendContract) data).getLendTransaction(),address);} catch (Exception e) {}
                        try { saveLendContract((LendContract) data,address);} catch (Exception e) {}
                    } else if (data instanceof StakeContract) {
                        try { saveStakeContract((StakeContract) data,address);} catch (Exception e) {}
                        try { saveUTXO(((StakeContract) data).getValidatorCommission(),address);} catch (Exception e) {}
                        try { deleteUTXO(((StakeContract) data).getValidatorCommission(),address);} catch (Exception e) {}
                    } else if (data instanceof Penalty) {
                        try { savePenalty((Penalty) data,address);} catch (Exception e) {}
                    } else if (data instanceof NFT) {
                        try { saveNFT((NFT) data);} catch (Exception e) {}
                        try { saveUTXO(((NFT) data).getMintFee(),address);} catch (Exception e) {}
                        try { deleteUTXO(((NFT) data).getMintFee(),address);} catch (Exception e) {}
                    } else if (data instanceof NFTTransfer) {
                        try { saveNFTTransfer((NFTTransfer) data,address);} catch (Exception e) {}
                        try { saveUTXO(((NFTTransfer) data).getSaleTransaction(),address);} catch (Exception e) {}
                        try { deleteUTXO(((NFTTransfer) data).getSaleTransaction(),address);} catch (Exception e) {}
                    } else if (data instanceof ListNFT) {
                        try {saveNFTList((ListNFT) data,address);} catch (Exception e) {}
                        try {saveUTXO(((ListNFT) data).getValidatorCommission(),address);} catch (Exception e) {}
                        try {deleteUTXO(((ListNFT) data).getValidatorCommission(),address);} catch (Exception e) {}
                    } else if (data instanceof Bid) {
                        try {saveBid((Bid) data,address);} catch (Exception e) {}
                        try {holdUTXO(((Bid) data).getTransaction(),((Bid) data).getContractHash(),address);}  catch (Exception e) {}
                    }
                    if (this.lentFundsUpdated) {
                        if (this.getWalletPath(address) == null) return;
                        File f = new File(this.getWalletPath(address));
                        String name = f.getName();
                        new Wallet(this.session).loadWallet(name).generateBaseOutputs(String.valueOf(System.currentTimeMillis()));
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(BlockFiles.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(BlockFiles.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    public Block getBlock(String hash) throws IOException, FileNotFoundException, ClassNotFoundException {
        Block b = (Block) new FileHandler().readObject(session.getPath() + "/blocks/" + hash);
        return b;
    }
    
    public void savePendingObject(Object obj) throws IOException {
        Files.createDirectories(Paths.get(session.getPath() + "/pending/"));
        FileHandler handler = new FileHandler();
        if (obj instanceof Transaction){
            if (getPendingObject(((Transaction) obj).getHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((Transaction) obj).getHash(),((Transaction) obj));
        }
        else if (obj instanceof BorrowContract) {
            if (getPendingObject(((BorrowContract) obj).getHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((BorrowContract) obj).getHash(),((BorrowContract) obj));
        }   
        else if (obj instanceof LendContract) {
            if (getPendingObject(((LendContract) obj).getHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((LendContract) obj).getHash(),((LendContract) obj));
        }
        else if (obj instanceof StakeContract) {
            if (getPendingObject(((StakeContract) obj).getHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((StakeContract) obj).getHash(),((StakeContract) obj));
        }
        else if (obj instanceof Penalty) {
           Files.createDirectories(Paths.get(session.getPath() + "/pending/penalties/" + ((Penalty) obj).getStakeHash()));
           handler.writeObject(session.getPath() + "/pending/penalties/" + ((Penalty) obj).getStakeHash() + "/" + ((Penalty) obj).getHash(), obj);
        }
        else if (obj instanceof NFT) {
           if (getPendingObject(((NFT) obj).getHash()) != null) return;
           handler.writeObject(session.getPath() + "/pending/" + ((NFT) obj).getHash(), obj);
        }
        else if (obj instanceof NFTTransfer) {
            if (getPendingObject(((NFTTransfer) obj).getHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((NFTTransfer) obj).getNFTHash(), obj);
        }
        else if (obj instanceof EndLendContract) {
            if (getPendingObject(((EndLendContract) obj).getLendContractHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((EndLendContract) obj).getLendContractHash(), obj);
        }
        else if (obj instanceof ListNFT) {
            if (getPendingObject(((ListNFT) obj).getNFTHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((ListNFT)obj).getNFTHash(), obj);
        }
        else if (obj instanceof Bid) {
            if (getPendingObject(((Bid) obj).getHash()) != null) return;
            handler.writeObject(session.getPath() + "/pending/" + ((Bid) obj).getHash(), obj);
        }
    }
    
    public void deletePendingObject(Object obj) throws Exception {
        FileHandler handler = new FileHandler();
        if (obj instanceof Transaction){handler.deleteFile(session.getPath() + "/pending/" + ((Transaction) obj).getHash());}
        else if (obj instanceof BorrowContract) {handler.deleteFile(session.getPath() + "/pending/" + ((BorrowContract) obj).getHash());}   
        else if (obj instanceof LendContract) {handler.deleteFile(session.getPath() + "/pending/" + ((LendContract) obj).getHash());}
        else if (obj instanceof StakeContract) {handler.deleteFile(session.getPath() + "/pending/" + ((StakeContract) obj).getHash());}
        else if (obj instanceof Penalty) {
            Penalty penalty = getSimilarPenalty((Penalty) obj);
            if (penalty != null)
            handler.deleteFile(session.getPath() + "/pending/penalties/" + penalty.getStakeHash() + "/" + penalty.getHash());
        }
        else if (obj instanceof NFT) {handler.deleteFile(session.getPath() + "/pending/" + ((NFT) obj).getHash());}
        else if (obj instanceof NFTTransfer) {handler.deleteFile(session.getPath() + "/pending/" + ((NFTTransfer) obj).getNFTHash());}
        else if (obj instanceof EndLendContract) {handler.deleteFile(session.getPath() + "/pending/" + ((EndLendContract)obj).getLendContractHash());}
        else if (obj instanceof ListNFT) {handler.deleteFile(session.getPath() + "/pending/" + ((ListNFT)obj).getNFTHash());}
        else if (obj instanceof Bid) {handler.deleteFile(session.getPath() + "/pending/" + ((Bid) obj).getHash());}
    }
    
    public String[] getPendingObjects() {
        File f = new File(session.getPath() + "/pending/");
        File[] files = recursiveListFiles(f,new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && !file.getName().equals(".DS_Store");
            }
        });
        if (files == null) return new String[0];
        String[] objects = new String[files.length];
        for (int x = 0; x < files.length; x++) {
            objects[x] = files[x].getPath();
        }
        return objects;
    }
    
    public static File[] recursiveListFiles(File dir, FileFilter filter) {
        if (!dir.isDirectory())
            return new File[0];
        List<File> fileList = new ArrayList<File>();
        recursiveListFilesHelper(dir, filter, fileList);
        return fileList.toArray(new File[fileList.size()]);
    }
    
    public static void recursiveListFilesHelper(File dir, FileFilter filter, List<File> fileList) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                recursiveListFilesHelper(f, filter, fileList);
            } else {
                if (filter.accept(f))
                    fileList.add(f);
            }
        }
    }
    
    public Penalty getSimilarPenalty(Penalty penalty) {
        ArrayList<Penalty> penalties = session.getBlockFileHandler().getPendingPenalties(penalty.getStakeHash());
        for (Penalty penaltyCheck : penalties) {
            if (!penaltyCheck.getTransaction().getOutputs().equals(penalty.getTransaction().getOutputs())) continue;
            return penaltyCheck;
        } 
        return null;
    }
    
    public ArrayList<Penalty> getPendingPenalties(String stakeHash) {
        ArrayList<Penalty> returnArray = new ArrayList<Penalty>();
        File f = new File(session.getPath() + "/pending/penalties/" + stakeHash + "/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return returnArray;
        for (int x = 0; x < files.length; x++) {
            FileHandler handler = new FileHandler();
            try {
               Penalty penalty = (Penalty) handler.readObject(files[x].getPath());
               if (penalty != null) returnArray.add(penalty);
            } catch (Exception e) {}
        }
        return returnArray;
    }
    
    public Object getPendingObject(String hash) {
        Object object = null;
        try {
            FileHandler handler = new FileHandler();
            object = handler.readObject(session.getPath() + "/pending/" + hash);
        } catch (Exception e) {}
        return object;
    }
    
    public void loadPendingObjects() {
        try {
            String[] allObjects = getPendingObjects();
            FileHandler handler = new FileHandler();
            for (String path : allObjects) {
                Object object = handler.readObject(path);
                session.getBlockchain().addPendingTxn(object);
                if (!session.getBlockchain().addData(object)) {
                    this.deletePendingObject(object);
                };
                
            }
        } catch (Exception e) {}
    }
    
    public void sendPendingObject(Object obj, ServerThread thread) throws IOException {
        String txn;
        if (obj instanceof Transaction){txn = DatatypeConverter.printBase64Binary(((Transaction) obj).toBytes());}
        else if (obj instanceof BorrowContract) {txn = DatatypeConverter.printBase64Binary(((BorrowContract) obj).toBytes());}   
        else if (obj instanceof LendContract) {txn = DatatypeConverter.printBase64Binary(((LendContract) obj).toBytes());}
        else if (obj instanceof StakeContract) {txn = DatatypeConverter.printBase64Binary(((StakeContract) obj).toBytes());} 
        else if (obj instanceof Penalty) {txn = DatatypeConverter.printBase64Binary(((Penalty) obj).toBytes());}
        else if (obj instanceof NFT) {txn = DatatypeConverter.printBase64Binary(((NFT) obj).toBytes());}
        else if (obj instanceof NFTTransfer) {txn = DatatypeConverter.printBase64Binary(((NFTTransfer) obj).toBytes());}
        else if (obj instanceof EndLendContract) {txn = DatatypeConverter.printBase64Binary(((EndLendContract) obj).toBytes());}
        else if (obj instanceof Bid) {txn = DatatypeConverter.printBase64Binary(((Bid) obj).toBytes());}
        else {return;}
        JsonObject data = Json.createObjectBuilder().add("data", txn).build();
        thread.sendMessage(Message.BROADCASTTXNPENDING, data);
    }
    
    public void sendPendingObjects(ServerThread thread) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        String[] allObjects = getPendingObjects();
        FileHandler handler = new FileHandler();
        if (allObjects == null) return;
        for (String path : allObjects) {
            Object object = handler.readObject(path);
            sendPendingObject(object,thread);
        }
    }
}
