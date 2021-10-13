/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Blockchain;

import com.pfinance.p2pcomm.Contracts.BorrowContract;
import com.pfinance.p2pcomm.Contracts.LendContract;
import com.pfinance.p2pcomm.Contracts.StakeContract;
import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.FileHandler.Validator;
import com.pfinance.p2pcomm.FileHandler.ValidatorIndex;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.P2P.Server.ServerThread;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Penalty;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;

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
    
    public BorrowContract getBorrowContract(String hash) throws IOException, FileNotFoundException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        return (BorrowContract) handler.readObject(session.getPath() + "/contracts/borrow/" + hash + "/contract");
    }
    
    public StakeContract getStakeContract(String hash)  throws IOException, FileNotFoundException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        return (StakeContract) handler.readObject(session.getPath() + "/contracts/stake/" + hash + "/contract");
    }
    
    public float getBorrowedBalance(Validator validator) throws IOException, FileNotFoundException, ClassNotFoundException {
        StakeContract contract = getStakeContract(validator.getStakeHash());
        if (contract == null) return 0;
        float returnValue = getBorrowBalance(contract.getBorrowContractHash()) + getPenaltyBalance(contract.getHash());
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
    
    public UTXO loadUTXO(String path) throws IOException, FileNotFoundException, ClassNotFoundException {
        FileHandler handler = new FileHandler();
        return (UTXO) handler.readObject(path);
    }
    
    public void saveUTXO(Transaction transaction) throws IOException {
        Files.createDirectories(Paths.get(session.getPath() + "/utxos/"));
        int index = 0;
        FileHandler handler = new FileHandler();
        for (TransactionOutput output : transaction.getOutputs()) {
            UTXO utxo = new UTXO(output,transaction.getHash(),index,null);
            handler.writeObject(session.getPath() + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(index), utxo);
            if (this.walletAddresses.contains(output.address)) {
                String path = this.getWalletPath(output.address);
                Files.createDirectories(Paths.get(path + "/utxos/"));
                handler.writeObject(path + "/utxos/" + utxo.getPreviousHash() + "|" + String.valueOf(index), utxo);
            }
            index += 1;
        }
    }
    
    public void deleteUTXO(Transaction transaction) throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        FileHandler handler = new FileHandler();
        for (TransactionInput input : transaction.getInputs()) {
            UTXO utxo = this.loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            if (session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex()))
                session.getBlockchain().getPendingUTXOs().remove(utxo.getPreviousHash() + "|" + utxo.getIndex());
            File f = new File(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
            handler.deleteFile(f.getPath());
            if (this.walletAddresses.contains(utxo.getAddress())) {
                String path = this.getWalletPath(utxo.getAddress());
                File f_wallet = new File(path + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
                handler.deleteFile(f_wallet.getPath());
            }
        }
    }
    
    public void saveUTXOLend(Transaction transaction, BorrowContract contract) throws IOException {
        FileHandler handler = new FileHandler();
        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            TransactionOutput output = transaction.getOutputs().get(i);
            UTXO utxo = new UTXO(output,transaction.getHash(),i,null);
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
        FileHandler handler = new FileHandler();
        handler.writeObject(session.getPath() + "/blocks/" + block.getHash(),block);
        
        for (int i = 0; i < block.data.size(); i++) {
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
            }
            deletePendingObject(data);
            if (this.lentFundsUpdated) {
                this.session.getWallet().generateBaseOutputs(String.valueOf(System.currentTimeMillis()));
            }
        }
        this.lentFundsUpdated = false;
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
