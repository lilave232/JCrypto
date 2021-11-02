/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.P2P.Peer;

import com.pfinance.p2pcomm.Blockchain.Block;
import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.FileHandler.HashIndex;
import static com.pfinance.p2pcomm.Main.prompt;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.P2P.Server.Server;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Voting.SignedVote;
import com.pfinance.p2pcomm.Voting.VoteResult;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author averypozzobon
 */
public class PeerMessageHandler {
    Peer peer = null;
    public ArrayList<String> blocksRequested = new ArrayList<String>();
    
    public boolean isRequested(String hash) {return blocksRequested.contains(hash);}
   
    
    public PeerMessageHandler(Peer peer) {
        this.peer = peer;
    }
    
    public void handle(BufferedReader bufferedReader, PeerThread thread) throws Exception {
        String msg = bufferedReader.readLine();
        //System.out.println(msg);
        JsonObject jsonObject = Json.createReader(new StringReader(msg)).readObject();
        if (jsonObject.containsKey("type") && jsonObject.containsKey("data")) {
            JsonObject data = jsonObject.getJsonObject("data");
            switch(jsonObject.getInt("type")) {
                default -> {
                    if (data.containsKey("message")) {
                        System.out.println("Peer Received: " + data.getString("message"));
                    }
                }
                case Message.CONNECTION -> thread.peer.addListener(data.getString("hostName"), data.getString("hostPort"));
                case Message.DOWNLOADHASHRECEIVED -> {
                    synchronized(this) {
                        byte[] object = DatatypeConverter.parseBase64Binary(StringEscapeUtils.unescapeJson(data.getString("hashes")));
                        ByteArrayInputStream in = new ByteArrayInputStream(object);
                        ObjectInputStream is = new ObjectInputStream(in);
                        HashIndex incoming = (HashIndex) is.readObject();
                        for (int i = 0; i < incoming.getHashes().size(); i++) {
                            if (this.blocksRequested.contains(incoming.getHashes().get(i).hash) || this.peer.getSession().getBlockFileHandler().getBlock(incoming.getHashes().get(i).hash) != null) {} 
                            else {
                                System.out.println("Requesting Block " + incoming.getHashes().get(i).hash);
                                JsonObject hash = Json.createObjectBuilder()
                                        .add("hash", incoming.getHashes().get(i).hash)
                                        .build();
                                this.peer.sendMessage(Message.DOWNLOADBLOCKREQUEST, hash);
                                this.blocksRequested.add(incoming.getHashes().get(i).hash);
                            }
                        }
                    }
                    
                }
                case Message.DOWNLOADBLOCKRECEIVED -> {
                    synchronized(this) {
                        if (this.peer.getSession().getBlockFileHandler().getBlock(data.getString("hash")) != null)
                            return;
                        byte[] object = DatatypeConverter.parseBase64Binary(data.getString("block"));
                        ByteArrayInputStream in = new ByteArrayInputStream(object);
                        ObjectInputStream is = new ObjectInputStream(in);
                        Block block = (Block) is.readObject();
                        System.out.println("Received Block: " + block.getHash());
                        this.peer.getSession().getBlockchain().addBlock(block);
                        this.blocksRequested.remove(block.getHash());
                        if (this.blocksRequested.isEmpty()) {
                            System.out.println("Chain Downloaded!!");
                        }
                        
                    }
                }
                case Message.BROADCASTTXNPENDING -> {
                    synchronized(this.peer.getSession()) {
                        byte[] object = DatatypeConverter.parseBase64Binary(data.getString("data"));
                        ByteArrayInputStream in = new ByteArrayInputStream(object);
                        ObjectInputStream is = new ObjectInputStream(in);
                        Object txn = (Object) is.readObject();
                        if (this.peer.getSession().getValidation()) {
                            if (this.peer.getSession().getBlockchain().addData(txn)) {
                                this.peer.getSession().getBlockchain().addPendingTxn(txn);
                            }
                        } else {
                            this.peer.getSession().getBlockchain().addPendingTxn(txn);
                        }
                    }
                }
                case Message.TXNACCEPTED -> {
                    synchronized(this) {
                        System.out.println("Transaction Accepted");
                        byte[] object = DatatypeConverter.parseBase64Binary(data.getString("data"));
                        ByteArrayInputStream in = new ByteArrayInputStream(object);
                        ObjectInputStream is = new ObjectInputStream(in);
                        Object txn = (Object) is.readObject();
                        if (this.peer.getSession().getValidation()) {
                            if (this.peer.getSession().getBlockchain().addData(txn)) {
                                this.peer.getSession().getBlockchain().addPendingTxn(txn);
                            }
                        }  else {
                            this.peer.getSession().getBlockchain().addPendingTxn(txn);
                        }
                    }
                }
                case Message.TXNFAILED -> {
                    synchronized(this) {
                        System.out.println("Transaction Failed");
                    }
                }
                case Message.PENALTYASSESSED -> {
                    synchronized(this) {
                        //ADD PENALTY
                    }
                }
            }
        }
    }
}
