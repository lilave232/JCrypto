/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Wallet;

import com.pfinance.p2pcomm.Blockchain.Block;
import com.pfinance.p2pcomm.FileHandler.*;
import com.pfinance.p2pcomm.Contracts.*;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
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
import javax.json.Json;
import javax.json.JsonObject;
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
                //System.out.println(new BigInteger(1,this.seed));
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
    
    public BigDecimal getBorrowedBalance(String hash) throws IOException, FileNotFoundException, ClassNotFoundException {
        BigDecimal returnValue = BigDecimal.ZERO;
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
            returnValue = returnValue.add(utxo.toFloat());
        }
        return returnValue;
    }
    
    public String getName() { return this.name; }
    public String getAddress() { return this.address; }
    public byte[] getSeed() {return this.seed;}
    public BorrowContract getBorrowContract() {return this.borrowContract;}
    
    public ArrayList<NFT> getNFTs() {
        try {
            return this.session.getBlockFileHandler().getWalletNFTs(this.address);
        } catch (IOException ex) {
            return new ArrayList<NFT>();
        }
    }
    
    public StakeContract getStakeContract() {return this.stakeContract;}
    
    public Key getKey(String pwd) throws Exception {
        if (seed == null) {
            loadKey(pwd);
        }
        KeyGenerator generator = new KeyGenerator();
        return generator.generate(this.seed);
    }
    
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
        
        BigDecimal usableBalance = getUsableBalance();
        BigDecimal borrowBalance = getBorrowedBalance();
        BigDecimal lentBalance = getLentBalance();
        BigDecimal penaltyBalance = getPenaltyBalance();
        StringBuilder returnString = new StringBuilder();
        returnString.append(String.format("|%-131s|\n", this.name + " Wallet Balance").replace(' ', '-'));
        returnString.append(String.format("|%-131s|\n", "Usable Balance: " + usableBalance));
        returnString.append(String.format("|%-131s|\n", "Borrowed Balance: " + borrowBalance));
        returnString.append(String.format("|%-131s|\n", "Penalties Incurred: " + penaltyBalance));
        returnString.append(String.format("|%-131s|\n", "Net Borrowed: " + String.valueOf(borrowBalance.add(penaltyBalance))));
        returnString.append(String.format("|%-131s|\n", "Lent Balance: " + lentBalance));
        returnString.append(String.format("|%-131s|\n", "").replace(' ', '-'));
        return returnString.toString();
    }
    
    public BigDecimal getUsableBalance() {
        try {
            BigDecimal usableBalance = BigDecimal.ZERO;
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
                        usableBalance = usableBalance.add(loadUTXO(file.getPath()).toFloat());
                    }
                        
                }
            }
            return usableBalance; 
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
        
    }
    
    public BigDecimal getPenaltyBalance() {
        try {
            BigDecimal balance = BigDecimal.ZERO;
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
                    if (penalty != null) balance = balance.add(penalty.getTransaction().sum());
                }
            }
            return balance; 
        } catch (Exception e) {
            return BigDecimal.ZERO;
        } 
    }
    
    public BigDecimal getBorrowedBalance() {
        try {
            BigDecimal balance = BigDecimal.ZERO;
            File f = new File(session.getPath() + "/wallets/" + this.name + "/contracts/borrow/lentFunds");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile() && !file.getName().contains(".pending");
                }
            });
            if (files != null) {
                for (File file : files) {
                    balance = balance.add(loadUTXO(file.getPath()).toFloat());
                }
            }
            return balance; 
        } catch (Exception e) {
            return BigDecimal.ZERO;
        } 
    }
    
    public ArrayList<TransactionOutput> getBaseOutputs(String timestamp) {
        try {
            FileHandler handler = new FileHandler();
            ArrayList<TransactionOutput> outputs = (ArrayList<TransactionOutput>) handler.readObject(session.getPath() + "/wallets/" + session.getWallet().name + "/baseOutputs");
            if (outputs == null)
                return generateBaseOutputs(timestamp);
            BigDecimal sum = BigDecimal.ZERO;
            sum = outputs.stream().map(output -> output.value).reduce(sum, (accumulator, _item) -> accumulator.add(_item));
            if (sum.compareTo(session.getBlockValidator().getReward(timestamp)) != 0) return generateBaseOutputs(timestamp);
            return outputs;
        } catch (IOException | ClassNotFoundException e) {return generateBaseOutputs(timestamp);}
    }
    
    public ArrayList<TransactionOutput> generateBaseOutputs(String timestamp) {
        System.out.println("Generating Base Outputs");
        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        HashMap<String,BigDecimal> outputValues = new HashMap<>();
        if (this.stakeContract == null) return new ArrayList<TransactionOutput>();
        Validator user = session.getValidators().getValidator(this.stakeContract.getHash());
        try {
            ArrayList<TransactionOutput> tempOutputs = new ArrayList<>();
            tempOutputs.addAll(session.getBlockFileHandler().getLentFunds(this.stakeContract.getBorrowContractHash()));
            tempOutputs.addAll(session.getBlockFileHandler().getPenaltyOutputs(this.stakeContract.getHash()));
            tempOutputs.forEach(output -> {
                outputValues.put(output.address, outputValues.getOrDefault(output.address, BigDecimal.ZERO).add(output.value));
            });
            outputValues.forEach((k,v)-> {
                TransactionOutput output = new TransactionOutput(k,(v.divide(user.getBalance(),MathContext.DECIMAL32)).multiply(session.getBlockValidator().getReward(timestamp)));
                System.out.println(output.toString());
                outputs.add(output);
            });
            FileHandler handler = new FileHandler();
            System.out.println("Saving Base Outputs");
            handler.writeObject(session.getPath() + "/wallets/" + session.getWallet().name + "/baseOutputs", outputs);
            return outputs;
        } catch (IOException e) {
            e.printStackTrace();
            return outputs;
        } catch (Exception ex) {
            ex.printStackTrace();
            return outputs;
        }
    }
    
    public BigDecimal getLentBalance() {
        try {
            BigDecimal balance = BigDecimal.ZERO;
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
                    
                    balance = balance.add(contract.getLendTransaction().getOutputs().get(0).value);
                }
            }
            return balance; 
        } catch (Exception e) {
            return BigDecimal.ZERO;
        } 
    }
    
    public ArrayList<LendContract> getLendContracts() {
        ArrayList<LendContract> lendContracts = new ArrayList<LendContract>();
        try {
            File f = new File(session.getPath() + "/wallets/" + this.name + "/contracts/lendContracts/");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile();
                }
            });
            FileHandler handler = new FileHandler();
            for (File file : files) {
                LendContract contract = (LendContract) handler.readObject(file.getPath());
                lendContracts.add(contract);
            }
            
        } catch (Exception e) {
            return new ArrayList<LendContract>();
        }
        return lendContracts;
    }
    
    public void getQuote() {
        System.out.println("Gathering Quotes...");
        session.minFee = null;
        try {
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                session.setMinFee(session.getBlockchain().getFeePerByte());
            } else {
                JsonObject data = Json.createObjectBuilder().add("data", "getQuote") .build();
                session.getPeer().sendMessage(Message.GETQUOTE, data);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            
        }
    }
    
    
    //TRANSACTIONS, CONTRACTS
    
    public Transaction createTransaction(ArrayList<TransactionOutput> outputs, BigDecimal fee) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        Transaction txn = new Transaction(this.getKey());
        BigDecimal outputValue = BigDecimal.ZERO;
        BigDecimal inputValue = BigDecimal.ZERO;
        BigDecimal startingFee = fee;
        outputValue = outputs.stream().map(output -> {
            try {
                txn.addOutput(output,this.getKey());
            } catch (Exception ex) {
                Logger.getLogger(Wallet.class.getName()).log(Level.SEVERE, null, ex);
            }
            return output;
        }).map(output -> output.value).reduce(outputValue, (accumulator, _item) -> accumulator.add(_item));
        
        
        if (session.getMinFee() == null) {
            session.setMinFee(new BigDecimal(0));
        }
        fee = startingFee.add(session.getMinFee().multiply(new BigDecimal(txn.toBytes().length + new TransactionOutput(this.address,(outputValue.add(fee))).toBytes().length)));
        
        ArrayList<UTXO> utxos = getUTXOInputs(outputValue.add(fee),session.getMinFee());
        
        for (UTXO utxo : utxos) {
            txn.addInput(utxo.getInput(this.getKey()),this.getKey());
            inputValue = inputValue.add(utxo.toFloat());
        }   
        fee = startingFee.add(session.getMinFee().multiply(new BigDecimal(txn.toBytes().length + new TransactionOutput(this.address,(outputValue.add(fee))).toBytes().length)));
        if (inputValue.compareTo((outputValue.add(fee))) > 0) {
            txn.addOutput(new TransactionOutput(this.address,inputValue.subtract((outputValue.add(fee)))),this.getKey());
        } else if (inputValue.compareTo((outputValue.add(fee))) < 0) {
            throw new Exception("Insufficient Funds");
        }
        System.out.println("Quoted Fee: " + fee);
        return txn;
    }
    
    public BorrowContract createBorrowContract() throws Exception {
        if (this.borrowContract != null) return null;
        BigDecimal fee = session.getMinFee();
        if (session.getMinFee() != null) fee = BigDecimal.ZERO;
        BigDecimal startFee = new BigDecimal(new BorrowContract(this.address, new Transaction(this.getKey()), this.getKey().getKey()).toBytes().length).multiply(fee);
        BorrowContract contract = new BorrowContract(this.address, createTransaction(new ArrayList<TransactionOutput>(),startFee),this.getKey().getKey());
        return contract;
    }
    
    public LendContract createLendContract(BorrowContract contract, BigDecimal amount) throws Exception {
        BigDecimal startFee = new BigDecimal(new LendContract(this.address,contract.getHash(),new Transaction(this.getKey()),this.getKey().getKey()).toBytes().length).multiply(session.getMinFee());
        LendContract lcontract = new LendContract(this.address,contract.getHash(),createTransaction(new TransactionOutput(contract.getBorrowerAddress(),amount).toList(),startFee),this.getKey().getKey());
        return lcontract;
    }
    
    public EndLendContract endLendContract(LendContract contract) throws Exception {
        BigDecimal startFee = new BigDecimal(new EndLendContract(contract.getHash(),contract.getBorrowContractHash(),new Transaction(this.getKey()), this.getKey()).toBytes().length).multiply(session.getMinFee());
        EndLendContract elcontract = new EndLendContract(contract.getHash(),contract.getBorrowContractHash(),createTransaction(new ArrayList<TransactionOutput>(),startFee), this.getKey());
        return elcontract;
    }
    
    public StakeContract createStakeContract() throws Exception {
        if (this.borrowContract == null) return null;
        BigDecimal startFee = new BigDecimal(new StakeContract(this.borrowContract.getHash(),new Transaction(this.getKey()),getKey().getKey()).toBytes().length).multiply(session.getMinFee());
        StakeContract contract = new StakeContract(this.borrowContract.getHash(),createTransaction(new ArrayList<TransactionOutput>(),startFee),getKey().getKey());
        return contract;
    }
    
    
    public NFT createNFT(String type, String title, String description, byte[] data) throws Exception {
        BigDecimal startFee = new BigDecimal(new NFT(this.address,new Transaction(this.getKey()),type,getKey().getKey(), title, description, data).toBytes().length).multiply(session.getMinFee());
        NFT nft = new NFT(this.address,createTransaction(new ArrayList<TransactionOutput>(),startFee),type,getKey().getKey(), title, description, data);
        return nft;
    }
    
    public ListNFT listNFT(NFT nft) throws Exception {
        BigDecimal startFee = new BigDecimal(new ListNFT(nft.getHash(),new Transaction(this.getKey()),getKey()).toBytes().length).multiply(session.getMinFee());
        ListNFT list = new ListNFT(nft.getHash(),createTransaction(new ArrayList<TransactionOutput>(),startFee),getKey());
        return list;
    }
    
    public DelistNFT delistNFT(NFT nft) throws Exception {
        DelistNFT delist = new DelistNFT(nft.getHash(),getKey());
        return delist;
    }
    
    public NFT getNFT(String hash) {
        try {
            NFT nft = (NFT) new FileHandler().readObject(this.session.getPath() + "/wallets/" + this.getName() + "/contracts/nfts/" + hash + "/nft");
            return nft;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public NFTTransfer transferNFT(Transaction saleTransaction, String nftHash, String transferToAddress) {
        try {
            NFT nft = getNFT(nftHash);
            if (nft == null) return null;
            HashIndex index = (HashIndex) new FileHandler().readObject(this.session.getPath() + "/contracts/nfts/" + nft.getHash() + "/hashIndex");
            if (index == null) return null;
            String previousHash = index.getHashes().get(index.getHashes().size()-1).hash;
            NFTTransfer transfer = new NFTTransfer(saleTransaction, previousHash, nft.getHash(), transferToAddress, this.getKey().getKey());
            return transfer;
        } catch (Exception e) {
            return null;
        }
    }
    
    public NFTTransfer transferNFT(Bid saleTransaction, String nftHash, String transferToAddress) {
        try {
            NFT nft = getNFT(nftHash);
            if (nft == null) return null;
            HashIndex index = (HashIndex) new FileHandler().readObject(this.session.getPath() + "/contracts/nfts/" + nft.getHash() + "/hashIndex");
            if (index == null) return null;
            String previousHash = index.getHashes().get(index.getHashes().size()-1).hash;
            NFTTransfer transfer = new NFTTransfer(saleTransaction, previousHash, nft.getHash(), transferToAddress, this.getKey().getKey());
            return transfer;
        } catch (Exception e) {
            return null;
        }
    }
    
    public Bid createBid(NFT nft, BigDecimal amount) throws Exception {
        String address = session.getBlockFileHandler().getNFTOwner(nft.getHash());
        BigDecimal startFee = new BigDecimal(new Bid(nft.getHash(),new Transaction(this.getKey()),this.getKey()).toBytes().length).multiply(session.getMinFee());
        Transaction txn = createTransaction(new TransactionOutput(address,amount).toList(),startFee);
        Bid bid = new Bid(nft.getHash(),txn,this.getKey());
        return bid;
    }
    
    public HashMap<String,Integer> listBidsByNFT() throws IOException {
        HashMap<String,Integer> returnMap = new HashMap<String,Integer>();
        ArrayList<NFT> nfts = session.getBlockFileHandler().getWalletNFTs(this.address);
        nfts.forEach(nft -> {
            returnMap.put(nft.getHash(), getBidCount(nft.getHash()));
        });
        return returnMap;
    }
    
    public ArrayList<Bid> getBids(String nftHash) {
        ArrayList<Bid> returnBids = new ArrayList<Bid>();
        File f = new File(session.getPath() + "/wallets/" + this.name + "/contracts/listNFTs/" + nftHash + "/bids");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return new ArrayList<Bid>();
        for (File file : files) {
            returnBids.add(getBid(file.getPath()));
        }
        return returnBids;
    } 
    
    public Integer getBidCount(String nftHash) {
        ArrayList<Bid> returnBids = new ArrayList<Bid>();
        File f = new File(session.getPath() + "/wallets/" + this.name + "/contracts/listNFTs/" + nftHash + "/bids");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return 0;
        return files.length;
    }
    
    public Bid getBid(String path) {
        try {
            Bid bid = (Bid) new FileHandler().readObject(path);
            return bid;
        } catch (Exception e) {}
        return null;
    }
    
    public ArrayList<UTXO> getUTXOInputs(BigDecimal value, BigDecimal fee) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        if (value.compareTo(BigDecimal.ZERO) == 0) return new ArrayList<UTXO>();
        ArrayList<UTXO> utxos = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
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
                    value = value.add(new BigDecimal(utxo.getInput(this.getKey()).toBytes().length).multiply(fee));
                    totalValue = totalValue.add(utxo.toFloat());
                    if (totalValue.compareTo(value) >= 0) break;
                }
            }
        }
        return utxos;
    }
}
