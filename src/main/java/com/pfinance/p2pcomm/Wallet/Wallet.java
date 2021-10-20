/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Wallet;

import com.pfinance.p2pcomm.FileHandler.*;
import com.pfinance.p2pcomm.Contracts.*;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.ECKeyPair;

/**
 *
 * @author averypozzobon
 */
public class Wallet {
    private String name = "";
    private String address = "";
    private byte[] seed = null;
    private BorrowContract borrowContract = null;
    private StakeContract stakeContract = null;
    private ArrayList<Transaction> lentFunds = new ArrayList<>();
    private HashIndex index = new HashIndex();
    private Session session;
    
    public Wallet(Session session) {
        this.session = session;
    }
    
    public List<Object> createWallet(String name, String pwd) {
        this.name = name;
        String mnemonic = null;
        if (Files.exists(Paths.get(session.getPath() + "/wallets/" + this.name))) {
            return null;
        } else {
            try {
                KeyGenerator generator = new KeyGenerator();
                String entropy = generator.createEntropy();
                mnemonic = generator.generateMnemonic(entropy);
                System.out.println("Mnemonic is: " + mnemonic);
                System.out.println("Remember to securely store your mnemonic offline!");
                this.seed = generator.getMaster(mnemonic);
                this.address = generator.generate(this.seed).getAddress();
                saveWallet(name, pwd);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
        return Arrays.asList(this, mnemonic);
    }
    
    public Wallet importWallet(String name, String pwd, String mnemonic) {
        try {
            KeyGenerator generator = new KeyGenerator();
            this.name = name;
            this.seed = generator.getMaster(mnemonic);
            this.address = generator.generate(this.seed).getAddress();
            saveWallet(name, pwd);
            this.session.getBlockFileHandler().indexWallet(this.address);
        } catch (Exception e) {e.printStackTrace();}
        return this;
    }
    
    public Wallet loadWallet(String name) throws IOException {
        this.name = name;
        this.address = loadAddress();
        this.seed = null;
        return this;
    }
    
    public Key loadKey(String pwd) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, GeneralSecurityException, Exception {
        KeyGenerator generator = new KeyGenerator();
        this.seed = loadSeed(pwd);
        return generator.generate(this.seed);
    }
    
    public void saveWallet(String name, String pwd) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, GeneralSecurityException {
        Files.createDirectories(Paths.get(session.getPath() + "/wallets/" + this.name));
        saveSeed(this.seed,pwd);
        saveAddress(this.address);
    }
    
    public void saveSeed(byte[] seed,String pwd) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchPaddingException, IllegalBlockSizeException, IllegalBlockSizeException, BadPaddingException, BadPaddingException, GeneralSecurityException, IOException {
        FileHandler handler = new FileHandler();
        handler.writeBytes(session.getPath() + "/wallets/" + this.name + "/seed",new Cryptography().encrpyt(pwd, seed));
    }
    
    public void saveAddress(String address) throws IOException {
        FileHandler handler = new FileHandler();
        handler.writeBytes(session.getPath() + "/wallets/" + this.name + "/address",address.getBytes());
    }
    
    public byte[] loadSeed(String pwd) throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, IllegalBlockSizeException, BadPaddingException, BadPaddingException, NoSuchPaddingException, GeneralSecurityException, IOException {
        FileHandler handler = new FileHandler();
        byte[] encoded = handler.readBytes(session.getPath() + "/wallets/" + this.name + "/seed");
        byte[] decoded = Cryptography.decrypt(pwd, encoded);
        return decoded;
    }
    
    public String loadAddress() throws IOException {
        FileHandler handler = new FileHandler();
        byte[] byteArray = handler.readBytes(session.getPath() + "/wallets/" + this.name + "/address");
        return new String(byteArray,"UTF-8");
    }
    
    public UTXO loadUTXO(String path) throws IOException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        return (UTXO) handler.readObject(path);
    }
    
    public void loadBorrowContract() {
        try {
            FileHandler handler = new FileHandler();
            this.borrowContract = (BorrowContract) handler.readObject(session.getPath() + "/wallets/" + this.name + "/contracts/borrow/contract");
        } catch (Exception e) {} 
    }
    
    public LendContract loadLendContract(String path) {
        try {
            FileHandler handler = new FileHandler();
            return (LendContract) handler.readObject(path);
        } catch (Exception e) {} 
        return null;
    }
    
    public void loadStakeContract() {
        try {
            FileHandler handler = new FileHandler();
            this.stakeContract = (StakeContract) handler.readObject(session.getPath() + "/wallets/" + this.name + "/contracts/stake/contract");
        } catch (Exception e) {} 
    }
    
    public float getBorrowedBalance(String hash) throws IOException, FileNotFoundException, ClassNotFoundException {
        float returnValue = 0;
        File f = new File(session.getPath() + "/contracts/borrow/" + hash + "/lentFunds");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return returnValue;
        for (File file : files) {
            UTXO utxo = loadUTXO(file.getPath());
            returnValue += utxo.toFloat();
        }
        return returnValue;
    }
    
    public String getName() { return this.name; }
    public String getAddress() { return this.address; }
    public byte[] getSeed() {return this.seed;}
    public BorrowContract getBorrowContract() {return this.borrowContract;}
    public StakeContract getStakeContract() {return this.stakeContract;}
    
    public Key getKey() throws Exception {
        if (seed == null) {
            System.out.println("Enter Password?");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String response = bufferedReader.readLine();
            loadKey(response);
        }
        KeyGenerator generator = new KeyGenerator();
        return generator.generate(this.seed);
    }
    
        
    public String getBalance() throws IOException, FileNotFoundException, ClassNotFoundException {
        
        float usableBalance = getUsableBalance();
        float borrowBalance = getBorrowedBalance();
        float lentBalance = getLentBalance();
        float penaltyBalance = getPenaltyBalance();
        StringBuilder returnString = new StringBuilder();
        returnString.append(String.format("|%-131s|\n", this.name + " Wallet Balance").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Usable Balance: " + usableBalance));
        returnString.append(String.format("|%-131s|\n", "Borrowed Balance: " + borrowBalance));
        returnString.append(String.format("|%-131s|\n", "Penalties Incurred: " + penaltyBalance));
        returnString.append(String.format("|%-131s|\n", "Net Borrowed: " + String.valueOf(borrowBalance - penaltyBalance)));
        returnString.append(String.format("|%-131s|\n", "Lent Balance: " + lentBalance));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
    
    public float getUsableBalance() {
        try {
            float usableBalance = 0;
            File f = new File(session.getPath() + "/wallets/" + this.name + "/utxos/");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile();
                }
            });
            if (files != null) {
                for (File file : files) {
                    UTXO utxo = loadUTXO(file.getPath());
                    if (!session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) {
                        usableBalance += loadUTXO(file.getPath()).toFloat();
                    }
                        
                }
            }
            return usableBalance; 
        } catch (Exception e) {
            return 0;
        }
        
    }
    
    public float getPenaltyBalance() {
        try {
            float balance = 0;
            File f = new File(session.getPath() + "/wallets/" + this.name + "/contracts/stake/penalties");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile();
                }
            });
            if (files != null) {
                for (File file : files) {
                    Penalty penalty = (Penalty) new FileHandler().readObject(file.getPath());
                    if (penalty != null) balance += penalty.getTransaction().sum();
                }
            }
            return balance; 
        } catch (Exception e) {
            return 0;
        } 
    }
    
    public float getBorrowedBalance() {
        try {
            float balance = 0;
            File f = new File(session.getPath() + "/wallets/" + this.name + "/contracts/borrow/lentFunds");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile() && !file.getName().contains(".pending");
                }
            });
            if (files != null) {
                for (File file : files) {
                    balance += loadUTXO(file.getPath()).toFloat();
                }
            }
            return balance; 
        } catch (Exception e) {
            return 0;
        } 
    }
    
    public ArrayList<TransactionOutput> getBaseOutputs(String timestamp) {
        try {
            FileHandler handler = new FileHandler();
            ArrayList<TransactionOutput> outputs = (ArrayList<TransactionOutput>) handler.readObject(session.getPath() + "/wallets/" + session.getWallet().name + "/baseOutputs");
            if (outputs == null)
                return generateBaseOutputs(timestamp);
            float sum = 0;
            sum = outputs.stream().map(output -> output.value).reduce(sum, (accumulator, _item) -> accumulator + _item);
            if (Float.compare(sum, session.getBlockValidator().getReward(timestamp)) != 0) return generateBaseOutputs(timestamp);
            return outputs;
        } catch (IOException | ClassNotFoundException e) {return generateBaseOutputs(timestamp);}
    }
    
    public ArrayList<TransactionOutput> generateBaseOutputs(String timestamp) {
        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        HashMap<String,Float> outputValues = new HashMap<>();
        if (this.stakeContract == null) return new ArrayList<TransactionOutput>();
        Validator user = session.getValidators().getValidator(this.stakeContract.getHash());
        try {
            ArrayList<TransactionOutput> tempOutputs = new ArrayList<>();
            tempOutputs.addAll(session.getBlockFileHandler().getLentFunds(this.stakeContract.getBorrowContractHash()));
            tempOutputs.addAll(session.getBlockFileHandler().getPenaltyOutputs(this.stakeContract.getHash()));
            tempOutputs.forEach(output -> {
                outputValues.put(output.address, outputValues.getOrDefault(output.address, (float)0) + output.value);
            });
            outputValues.forEach((k,v)-> {
                TransactionOutput output = new TransactionOutput(k,(v/user.getBalance())*session.getBlockValidator().getReward(timestamp));
                outputs.add(output);
            });
            FileHandler handler = new FileHandler();
            handler.writeObject(session.getPath() + "/wallets/" + session.getWallet().name + "/baseOutputs", outputs);
            return outputs;
        } catch (IOException e) {
            return outputs;
        } 
    }
    
    public float getLentBalance() {
        try {
            float balance = 0;
            File f = new File(session.getPath() + "/wallets/" + this.name + "/contracts/lendContracts/");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile();
                }
            });
            if (files != null) {
                for (File file : files) {
                    FileHandler handler = new FileHandler();
                    LendContract contract = (LendContract) handler.readObject(file.getPath());
                    
                    balance += contract.getLendTransaction().getOutputs().get(0).value;
                }
            }
            return balance; 
        } catch (Exception e) {
            return 0;
        } 
    }
    
    
    //TRANSACTIONS, CONTRACTS
    
    public Transaction createTransaction(ArrayList<TransactionOutput> outputs) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        Transaction txn = new Transaction();
        float outputValue = 0;
        float inputValue = 0;
        outputValue = outputs.stream().map(output -> {
            txn.addOutput(output);
            return output;
        }).map(output -> output.value).reduce(outputValue, (accumulator, _item) -> accumulator + _item);
        for (UTXO utxo : getUTXOInputs(outputValue)) {
            txn.addInput(utxo.getInput(this.getKey()));
            inputValue += utxo.toFloat();
        }
            
        if (inputValue > outputValue) {
            txn.addOutput(new TransactionOutput(this.address,inputValue - outputValue));
        } else if (inputValue < outputValue) {
            throw new Exception("Insufficient Funds");
        }
        return txn;
    }
    
    public BorrowContract createBorrowContract() throws Exception {
        if (this.borrowContract != null) return null;
        BorrowContract contract = new BorrowContract(this.address, new Transaction(),this.getKey().getKey());
        //this.borrowContract = contract;
        return contract;
    }
    
    public LendContract createLendContract(BorrowContract contract, float amount) throws Exception {
        LendContract lcontract = new LendContract(this.address,contract.getHash(),createTransaction(new TransactionOutput(contract.getBorrowerAddress(),amount).toList()),this.getKey().getKey());
        return lcontract;
    }
    
    public StakeContract createStakeContract() throws Exception {
        if (this.borrowContract == null) return null;
        StakeContract contract = new StakeContract(this.borrowContract.getHash(),new Transaction(),getKey().getKey());
        return contract;
    }
    
    public ArrayList<UTXO> getUTXOInputs(float value) throws IOException, FileNotFoundException, ClassNotFoundException {
        ArrayList<UTXO> utxos = new ArrayList<>();
        float totalValue = 0;
        File f = new File(session.getPath() + "/wallets/" + this.name + "/utxos/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files != null) {
            for (File file : files) {
                UTXO utxo = loadUTXO(file.getPath());
                if (!session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) {
                    utxos.add(utxo);
                    totalValue += utxo.toFloat();
                    if (totalValue >= value) break;
                }
            }
        }
        return utxos;
    }
    
}
