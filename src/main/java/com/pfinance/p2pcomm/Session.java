/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm;

import com.pfinance.p2pcomm.Contracts.BorrowContract;
import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.Blockchain.*;
import com.pfinance.p2pcomm.Contracts.StakeContract;
import com.pfinance.p2pcomm.FileHandler.HashIndex;
import com.pfinance.p2pcomm.FileHandler.Validator;
import com.pfinance.p2pcomm.FileHandler.ValidatorIndex;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Miner.Miner;
import com.pfinance.p2pcomm.Miner.Scheduler;
import com.pfinance.p2pcomm.P2P.Peer.Peer;
import com.pfinance.p2pcomm.P2P.Server.ServerMessageHandler;
import com.pfinance.p2pcomm.Transaction.UTXO;
import com.pfinance.p2pcomm.Wallet.Wallet;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author averypozzobon
 */
public class Session {
    private Wallet activeWallet = null;
    private BlockFiles blockFileHandler = new BlockFiles(this);
    private BlockValidation blockValidator = new BlockValidation(this);
    private Blockchain blockchain = new Blockchain(this);
    private Peer peer = null;
    private String path = null;
    private ValidatorIndex validators = new ValidatorIndex();
    private Scheduler scheduler = new Scheduler(this);
    private Boolean validation = false;
    private Miner miner = new Miner(this);
    
    public Session() {
        
    }
    
    public void setPath(String path) throws IOException, ClassNotFoundException {
        this.path = System.getProperty("user.dir") + "/blockchains/" + path;
        Files.createDirectories(Paths.get(this.path));
        Files.createDirectories(Paths.get(this.path + "/wallets/"));
        blockFileHandler.loadValidators();
        blockFileHandler.getAddresses(); 
        this.blockchain.load();
    }
    
    public void setWallet(Wallet wallet) throws IOException, FileNotFoundException, ClassNotFoundException { 
        this.activeWallet = wallet; 
        blockFileHandler.getAddresses(); 
        this.activeWallet.loadBorrowContract();
        this.activeWallet.loadStakeContract();
    }
    
    public void updateWallet() throws IOException, ClassNotFoundException, Exception {
        this.activeWallet.loadBorrowContract();
        this.activeWallet.loadStakeContract();
        blockFileHandler.loadValidators();
        blockFileHandler.getAddresses();
        this.getValidation();
    }
    
    public void connectPeer() throws UnknownHostException, Exception {
        System.out.println("Hostname: " + InetAddress.getLocalHost().getHostName());
        peer = new Peer(this);
        System.out.println("Port?");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String port = bufferedReader.readLine().toLowerCase();
        peer.connect(port);
        
    }
    
    public ArrayList<Wallet> getWallets() {
        ArrayList<Wallet> returnArray = new ArrayList<>();
        String[] walletNames = this.blockFileHandler.getWallets();
        System.out.println(String.join(",", walletNames));
        for (int i = 0; i < walletNames.length; i++) {
            try {
                returnArray.add(new Wallet(this).loadWallet(walletNames[i]));
            } catch (IOException ex) {
                Logger.getLogger(Session.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return returnArray;
    }
    
    public Wallet getWallet() {return this.activeWallet;}
    public Peer getPeer() {return this.peer;}
    public Blockchain getBlockchain() {return this.blockchain;}
    public BlockValidation getBlockValidator() {return this.blockValidator;}
    public BlockFiles getBlockFileHandler() {return this.blockFileHandler;}
    public String getPath() {return this.path;}
    
    public void setValidation() throws Exception {
        if (this.validation) {
            miner.stopMiner();
            this.blockchain.block = null;
            this.validation = false;
            return;
        }
        if (activeWallet.getStakeContract() == null){
            miner.stopMiner();
            this.validation = false;
            return;
        }
        Validator user = this.validators.getValidator(activeWallet.getStakeContract().getHash());
        if (user == null) {
            miner.stopMiner();
            this.validation = false;
            return;
        }
        if (user.getBalance() < blockValidator.getStakeRequirement()) {
            miner.stopMiner();
            this.validation = false;
            return;
        }
        activeWallet.getKey();
        this.validation = true;
        if (this.blockchain.block == null)this.blockchain.newBlock(activeWallet.getStakeContract().getHash(), activeWallet.getKey());
    }
    
    public boolean getValidation() throws Exception {
        if (activeWallet.getStakeContract() == null) {
            this.validation = false;
            this.blockchain.block = null;
            return this.validation;
        }
        Validator user = this.validators.getValidator(activeWallet.getStakeContract().getHash());
        if (user == null) {
            this.validation = false;
            return this.validation;
        }
        if (user.getBalance() < blockValidator.getStakeRequirement()) {
            this.validation = false;
            return this.validation;
        }
        if (this.validation) {
            activeWallet.getKey();
        }
        
        return this.validation;
    }
    
    public boolean getValidationAvailable() throws Exception {
        if (activeWallet.getStakeContract() == null) {return false;}
        Validator user = this.validators.getValidator(activeWallet.getStakeContract().getHash());
        if (user == null) {return false;}
        if (user.getBalance() < blockValidator.getStakeRequirement()) {return false;}
        return true;
    }
    
    public void setValidators(ValidatorIndex validator) {this.validators = validator;}
    public ValidatorIndex getValidators() {return this.validators;}
    
    public Scheduler getScheduler() {return this.scheduler;}
    public Miner getMiner() {return this.miner;}
}
