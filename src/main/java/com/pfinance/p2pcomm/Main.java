/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm;

import com.pfinance.p2pcomm.Blockchain.Block;
import com.pfinance.p2pcomm.Blockchain.BlockValidation;
import com.pfinance.p2pcomm.Blockchain.Blockchain;
import com.pfinance.p2pcomm.Contracts.BorrowContract;
import com.pfinance.p2pcomm.Contracts.EndLendContract;
import com.pfinance.p2pcomm.Contracts.LendContract;
import com.pfinance.p2pcomm.Contracts.ListNFT;
import com.pfinance.p2pcomm.Contracts.NFT;
import com.pfinance.p2pcomm.Contracts.NFTTransfer;
import com.pfinance.p2pcomm.Contracts.StakeContract;
import com.pfinance.p2pcomm.FileHandler.Validator;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Miner.Miner;
import com.pfinance.p2pcomm.P2P.Peer.Peer;
import com.pfinance.p2pcomm.Transaction.Bid;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Voting.SignedVote;
import com.pfinance.p2pcomm.Voting.VoteResult;
import com.pfinance.p2pcomm.Voting.VoteType;
import com.pfinance.p2pcomm.Wallet.Wallet;
import com.pfinance.p2pcomm.Websocket.WebServer;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectStreamClass;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

/**
 *
 * @author averypozzobon
 */
public class Main {
    public static Session session = new Session();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        prompt();
    }
    
    public static void prompt() {
        try {
            
            if (session.getPath() == null) {setupActivePath();} 
            else if (session.getWallet() == null) {setupActiveWallet();}
            else {mainMenu();}
            prompt();
        } catch (Exception e) {e.printStackTrace();prompt();}
    }
    
    public static void mainMenu() throws IOException, FileNotFoundException, ClassNotFoundException, Exception {
        Wallet activeWallet = session.getWallet();
        session.updateWallet();
        System.out.println("Active Wallet: " + activeWallet.getName());
        System.out.println("Options: ");
        System.out.println("[0]Change Wallet");
        System.out.println("[1]Show Address");
        if (session.getPeer() == null) System.out.println("[2]Start Peer");
        if (session.getPeer() != null) System.out.println("[3]Connect Peer");
        if (session.getPeer() != null) System.out.println("[4]List Peers");
        System.out.println("[5]Initiate Blockchain");
        if (session.getPeer() != null) System.out.println("[6]Download Chain");
        System.out.println("[7]Get Balance");
        if (session.getPeer() != null && session.getValidation()) System.out.println("[8]New Block");
        if (session.getPeer() != null) System.out.println("[9]Send Transaction");
        if (session.getPeer() != null && activeWallet.getBorrowContract() == null) System.out.println("[10]Borrow Contract");
        if (session.getPeer() != null) System.out.println("[11]Lend Contract");
        if (session.getPeer() != null && activeWallet.getBorrowContract() != null && activeWallet.getStakeContract() == null) System.out.println("[12]Stake Contract");
        if (session.getPeer() != null && !session.getValidation() && session.getValidationAvailable()) System.out.println("[14]Start Validation");
        else if (session.getPeer() != null && session.getValidation() && session.getValidationAvailable()) System.out.println("[14]Stop Validation");
//        if (session.getPeer() != null) System.out.println("[15]End Lend Contract");
        if (session.getPeer() != null) System.out.println("[16]List NFT");
        if (session.getPeer() != null) System.out.println("[17]Bid on NFT");
        if (session.getPeer() != null) System.out.println("[18]Show Bids");
        if (session.getPeer() != null) System.out.println("[19]Accept Bid and Transfer");
        if (session.getPeer() != null) System.out.println("[20]Run Webserver");
        
        System.out.println("[E]Exit");
        System.out.println("Selection?");
        
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String response = bufferedReader.readLine().toLowerCase();
        
        if (response.equals("0")) {setupActiveWallet();activeWallet = session.getWallet();}
        else if (response.equals("1")) {System.out.println(activeWallet.getAddress());}
        else if (response.equals("2") && session.getPeer() == null) {session.connectPeer();}
        else if (response.equals("3") && session.getPeer() != null) {session.getPeer().addPeerListener();}
        else if (response.equals("4") && session.getPeer() != null) {session.getPeer().listConnections();}
        else if (response.equals("5")) {session.getBlockchain().initialize();}
        else if (response.equals("6") && session.getPeer() != null) {
            JsonObject data = Json.createObjectBuilder().add("data", "Download") .build();
            session.getPeer().sendMessage(Message.DOWNLOADHASHREQUEST, data);
        }
        else if (response.equals("7")) {System.out.println(activeWallet.getBalance());}
        else if (response.equals("8") && session.getPeer() != null && session.getValidation()) {
            session.getBlockchain().newBlock(activeWallet.getStakeContract().getHash(), activeWallet.getKey());
        }
        else if (response.equals("9") && session.getPeer() != null) {
            System.out.println("Address?");
            String address = bufferedReader.readLine();
            System.out.println("Amount to Send?");
            float value = Float.valueOf(bufferedReader.readLine());
            System.out.println("Fee Amount?");
            float fee = Float.valueOf(bufferedReader.readLine());
            TransactionOutput output = new TransactionOutput(address,value);
            Transaction txn = activeWallet.createTransaction(output.toList(),fee);
            String object = DatatypeConverter.printBase64Binary(txn.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(txn);
                if (result) {session.getBlockchain().addPendingTxn(txn);System.out.println("Transaction Accepted");} 
                else {System.out.println("Transaction Failed");} 
            }
            
        }
        else if (response.equals("10") && session.getPeer() != null && activeWallet.getBorrowContract() == null) {
            System.out.println("Fee Amount?");
            float fee = Float.valueOf(bufferedReader.readLine());
            BorrowContract contract = activeWallet.createBorrowContract(fee);
            String object = DatatypeConverter.printBase64Binary(contract.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
        }
        else if (response.equals("11") && session.getPeer() != null) {
            String[] contracts = session.getBlockFileHandler().getBorrowContracts();
            for (int i = 0; i < contracts.length; i++) {
                if (activeWallet.getBorrowContract() != null) {
                    if (contracts[i].equals(activeWallet.getBorrowContract().getHash())) {System.out.println("[" + i + "]" + contracts[i] + " (Me)");} 
                    else {System.out.println("[" + i + "]" + contracts[i]);}
                }
                else {System.out.println("[" + i + "]" + contracts[i]);}
            }
            System.out.println("Selection?");
            Integer selection = Integer.valueOf(bufferedReader.readLine());
            System.out.println("Amount To Lend?");
            float value = Float.valueOf(bufferedReader.readLine());
            System.out.println("Fee Amount?");
            float fee = Float.valueOf(bufferedReader.readLine());
            LendContract lcontract = activeWallet.createLendContract(session.getBlockFileHandler().getBorrowContract(contracts[selection]), value, fee);
            String object = DatatypeConverter.printBase64Binary(lcontract.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(lcontract);
                if (result) {session.getBlockchain().addPendingTxn(lcontract);System.out.println("Transaction Accepted");} 
                else {System.out.println("Transaction Failed");} 
            }
        }
        else if (response.equals("12") && session.getPeer() != null && activeWallet.getBorrowContract() != null && activeWallet.getStakeContract() == null) {
            System.out.println("Fee Amount?");
            float fee = Float.valueOf(bufferedReader.readLine());
            StakeContract contract = activeWallet.createStakeContract(fee);
            String object = DatatypeConverter.printBase64Binary(contract.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
        }
        else if (response.equals("14") && session.getPeer() != null) {session.setValidation();}
//        else if (response.equals("15") && session.getPeer() != null) {
//            ArrayList<LendContract> contracts = activeWallet.getLendContracts();
//            for (int i = 0; i < contracts.size(); i++) {
//                float value = 0;
//                LendContract contract = contracts.get(i);
//                if (contract.getLendTransaction().getOutputs().size() > 0) value = contract.getLendTransaction().getOutputs().get(0).value;
//                System.out.println("[" + i + "]" + contract.getHash() + " " + value);
//            }
//            System.out.println("Selection?");
//            Integer selection = Integer.valueOf(bufferedReader.readLine());
//            LendContract contract = contracts.get(selection);
//            System.out.println("Fee Amount?");
//            float fee = Float.valueOf(bufferedReader.readLine());
//            EndLendContract endcontract = activeWallet.endLendContract(contract,fee);
//            String object = DatatypeConverter.printBase64Binary(endcontract.toBytes());
//            JsonObject data = Json.createObjectBuilder().add("data", object).build();
//            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
//        }
        else if (response.equals("16") && session.getPeer() != null) {
            ArrayList<NFT> nfts = activeWallet.getNFTs();
            for (int i = 0; i < nfts.size(); i++) {
                float value = 0;
                NFT nft = nfts.get(i);
                System.out.println("[" + i + "]" + nft.getHash());
            }
            System.out.println("Selection?");
            Integer selection = Integer.valueOf(bufferedReader.readLine());
            NFT nft = nfts.get(selection);
            System.out.println("Fee Amount?");
            float fee = Float.valueOf(bufferedReader.readLine());
            ListNFT list = activeWallet.listNFT(nft, fee);
            String object = DatatypeConverter.printBase64Binary(list.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(list);
                if (result) {session.getBlockchain().addPendingTxn(list);System.out.println("Transaction Accepted");} 
                else {System.out.println("Transaction Failed");} 
            }
        }
        else if (response.equals("17") && session.getPeer() != null) {
            ArrayList<NFT> nfts = session.getBlockFileHandler().getListedNFTs();
            for (int i = 0; i < nfts.size(); i++) {
                float value = 0;
                NFT nft = nfts.get(i);
                System.out.println("[" + i + "]" + nft.getHash());
            }
            System.out.println("Selection?");
            Integer selection = Integer.valueOf(bufferedReader.readLine());
            NFT nft = nfts.get(selection);
            System.out.println("Bid Amount?");
            float amount = Float.valueOf(bufferedReader.readLine());
            System.out.println("Fee Amount?");
            float fee = Float.valueOf(bufferedReader.readLine());
            Bid bid = activeWallet.createBid(nft, amount, fee);
            
            String object = DatatypeConverter.printBase64Binary(bid.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(bid);
                if (result) {session.getBlockchain().addPendingTxn(bid);System.out.println("Transaction Accepted");} 
                else {System.out.println("Transaction Failed");} 
            }
        }
        else if (response.equals("18") && session.getPeer() != null) {
            HashMap<String, Integer> nfts = activeWallet.listBidsByNFT();
            for (int i = 0; i < nfts.keySet().size(); i++) {
                System.out.println("[" + i + "]" + nfts.keySet().toArray()[i] + "(" + nfts.get(nfts.keySet().toArray()[i]) + ")");
            }
            System.out.println("Selection?");
            Integer selection = Integer.valueOf(bufferedReader.readLine());
            ArrayList<Bid> bids = activeWallet.getBids((String) nfts.keySet().toArray()[selection]);
            for (int i = 0; i < bids.size(); i++) {
                float amount = 0;
                if (bids.get(i).getTransaction().getOutputs().size() > 0) {
                    amount = bids.get(i).getTransaction().getOutputs().get(0).value;
                }
                System.out.println("[" + i + "]" + DigestUtils.sha256Hex(bids.get(i).getKey().toByteArray()) + " " + amount);
            }
            //if (bids.size() == 0)return;
            //System.out.println("Selection?");
            //selection = Integer.valueOf(bufferedReader.readLine());
        }
        else if (response.equals("19")  && session.getPeer() != null) {
            HashMap<String, Integer> nfts = activeWallet.listBidsByNFT();
            for (int i = 0; i < nfts.keySet().size(); i++) {
                System.out.println("[" + i + "]" + nfts.keySet().toArray()[i] + "(" + nfts.get(nfts.keySet().toArray()[i]) + ")");
            }
            System.out.println("Selection?");
            Integer selection = Integer.valueOf(bufferedReader.readLine());
            String nftHash = (String) nfts.keySet().toArray()[selection];
            ArrayList<Bid> bids = activeWallet.getBids(nftHash);
            for (int i = 0; i < bids.size(); i++) {
                float amount = 0;
                if (bids.get(i).getTransaction().getOutputs().size() > 0) {
                    amount = bids.get(i).getTransaction().getOutputs().get(0).value;
                }
                System.out.println("[" + i + "]" + DigestUtils.sha256Hex(bids.get(i).getKey().toByteArray()) + " " + amount);
            }
            if (bids.size() == 0)return;
            System.out.println("Selection?");
            selection = Integer.valueOf(bufferedReader.readLine());
            NFTTransfer transfer = activeWallet.transferNFT(bids.get(selection), nftHash, DigestUtils.sha256Hex(bids.get(selection).getKey().toByteArray()));
            String object = DatatypeConverter.printBase64Binary(transfer.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(transfer);
                if (result) {session.getBlockchain().addPendingTxn(transfer);System.out.println("Transaction Accepted");} 
                else {System.out.println("Transaction Failed");} 
            }
        }
        else if (response.equals("20") && session.getPeer() != null) {
            WebServer webserver = new WebServer(session);
            webserver.start();
        }
        else if (response.equals("21")) {System.out.println(session.getValidators());}
        else if (response.equals("22")) {System.out.println(session.getBlockchain().block);}
        else if (response.equals("23")) {System.out.println(session.getScheduler());}
        else if (response.equals("24")) {session.getStats().getWalletInOuts().forEach(in -> {System.out.println(in);});}
        else if (response.equals("e")) {System.exit(0);}
    }
    
    
    public static void setupActivePath() throws IOException, ClassNotFoundException {
        String[] chains = session.getBlockFileHandler().getBlockchains();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Options: ");
        System.out.println("[0]Create Blockchain");
        System.out.println("[1]Load Blockchain");
        System.out.println("[E]Exit");
        System.out.println("Selection?");
        String response = bufferedReader.readLine().toLowerCase();
        if (response.equals("0")) {
            System.out.println("Chain Name?");
            String name = bufferedReader.readLine();
            session.setPath(name);
        } else if (response.equals("1")) {
            for (int i = 0; i < chains.length; i++) {System.out.println("[" + i + "]" + chains[i]);}
            System.out.println("Selection?");
            Integer selection = Integer.valueOf(bufferedReader.readLine());
            if (chains.length > selection) {String name = chains[selection];session.setPath(name);}
        } else if (response.equals("e")) {System.exit(0);}
    }
    
    public static void setupActiveWallet() throws IOException, FileNotFoundException, ClassNotFoundException {
        String[] wallets = session.getBlockFileHandler().getWallets();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Options: ");
        System.out.println("[0]Create Wallet");
        System.out.println("[1]Import Wallet");
        if (wallets.length != 0) {System.out.println("[2]Load Wallet");}
        System.out.println("[E]Exit");
        System.out.println("Selection?");
        String response = bufferedReader.readLine().toLowerCase();
        if (response.equals("0")) {
            System.out.println("Wallet Name?");
            String name = bufferedReader.readLine();
            System.out.println("Wallet Password?");
            String pwd = bufferedReader.readLine();
            session.setWallet((Wallet) new Wallet(session).createWallet(name, pwd).get(0));
        } else if (response.equals("1")) {
            System.out.println("Wallet Mnemonic?");
            String mnemonic = bufferedReader.readLine();
            System.out.println("Wallet Name?");
            String name = bufferedReader.readLine();
            System.out.println("Wallet Password?");
            String pwd = bufferedReader.readLine();
            session.setWallet(new Wallet(session).importWallet(name, pwd, mnemonic));
        } else if (response.equals("2")) {
            for (int i = 0; i < wallets.length; i++) {System.out.println("[" + i + "]" + wallets[i]);}
            System.out.println("Selection?");
            Integer selection = Integer.valueOf(bufferedReader.readLine());
            if (wallets.length > selection) {String name = wallets[selection];session.setWallet(new Wallet(session).loadWallet(name));}
        } else if (response.equals("e")) {System.exit(0);}
    }
}
