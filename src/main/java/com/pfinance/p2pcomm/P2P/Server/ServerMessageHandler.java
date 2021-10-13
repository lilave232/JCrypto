/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.P2P.Server;

import com.pfinance.p2pcomm.Blockchain.Block;
import com.pfinance.p2pcomm.Contracts.StakeContract;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import com.pfinance.p2pcomm.FileHandler.HashIndex;
import com.pfinance.p2pcomm.FileHandler.Validator;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.P2P.Peer.PeerThread;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Penalty;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Voting.BallotBox;
import com.pfinance.p2pcomm.Voting.SignedVote;
import com.pfinance.p2pcomm.Voting.VoteResult;
import com.pfinance.p2pcomm.Voting.VoteType;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.math.BigInteger;
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
public class ServerMessageHandler {
    Server server = null;
    private ArrayList<String> transactionsBroadcast = new ArrayList<String>();
    
    
    public ServerMessageHandler(Server server) {this.server = server;}
    
    public void handle(BufferedReader bufferedReader, ServerThread thread) throws Exception {
        String msg = bufferedReader.readLine();
        JsonObject jsonObject = Json.createReader(new StringReader(msg)).readObject();
        
        if (jsonObject.containsKey("type") && jsonObject.containsKey("data")) {
            JsonObject data = jsonObject.getJsonObject("data");
            switch(jsonObject.getInt("type")) {
                default -> {if (data.containsKey("message")) {System.out.println("Server Received: " + jsonObject.getString("message"));}}
                case Message.CONNECTION -> {
                    this.server.getPeer().addListener(data.getString("hostName"), data.getString("hostPort"));
                    this.server.sendMessage(Message.CONNECTION,data);
                }
                case Message.DOWNLOADHASHREQUEST -> {
                    String object = StringEscapeUtils.escapeJson(DatatypeConverter.printBase64Binary(this.server.getPeer().getSession().getBlockchain().getHashIndex().toBytes()));
                    JsonObject hashes = Json.createObjectBuilder().add("hashes", object).build();
                    thread.sendMessage(Message.DOWNLOADHASHRECEIVED, hashes);
                }
                case Message.DOWNLOADBLOCKREQUEST -> {
                    Block block = this.server.getPeer().getSession().getBlockFileHandler().getBlock(data.getString("hash"));
                    String object = DatatypeConverter.printBase64Binary(block.toBytes());
                    JsonObject value = Json.createObjectBuilder().add("hash", data.getString("hash")).add("block", object).build();
                    thread.sendMessage(Message.DOWNLOADBLOCKRECEIVED, value);
                }
                case Message.REQUESTPENDING -> {
                    if (session.getValidation()) {
                        session.getBlockFileHandler().sendPendingObjects(thread);
                    }
                }
                case Message.BROADCASTTXN -> {
                    synchronized(this) {
                        if (session.getValidation()) {
                            byte[] object = DatatypeConverter.parseBase64Binary(data.getString("data"));
                            ByteArrayInputStream in = new ByteArrayInputStream(object);
                            ObjectInputStream is = new ObjectInputStream(in);
                            Object txn = (Object) is.readObject();
                            boolean result = this.server.getPeer().getSession().getBlockchain().addData(txn);
                            if (result) {this.server.getPeer().getSession().getBlockchain().addPendingTxn(txn);thread.sendMessage(Message.TXNACCEPTED, data);} 
                            else {thread.sendMessage(Message.TXNFAILED, data);}    
                        }
                    }
                }
                case Message.BLOCKVALIDATIONREQUEST -> {
                    synchronized(this) {
                        if (session.getValidation()) {
                            byte[] object = DatatypeConverter.parseBase64Binary(data.getString("data"));
                            ByteArrayInputStream in = new ByteArrayInputStream(object);
                            ObjectInputStream is = new ObjectInputStream(in);
                            Block block = (Block) is.readObject();
                            
                            boolean result = session.getBlockchain().verifyBlock(block);
                            int voteResult = VoteResult.NO;
                            if (result) voteResult = VoteResult.YES;
                            else thread.sendMessage(Message.PENALTYASSESSED, data);
                            SignedVote vote = new SignedVote(voteResult,session.getWallet().getAddress(),session.getWallet().getStakeContract().getHash(),session.getWallet().getKey().getKey());
                            this.server.getPeer().getBallotBox().addVote(block.getHash(),VoteType.BLOCK,vote);
                            String voteObject = DatatypeConverter.printBase64Binary(vote.toBytes());
                            JsonObject value = Json.createObjectBuilder().add("block", data.getString("data")).add("vote", voteObject).build();
                            this.server.getPeer().sendMessage(Message.BLOCKVALIDATIONVOTE, value);
                        }
                    }                   
                }
                case Message.BLOCKVALIDATIONVOTE -> {
                    synchronized(this) {
                        if (session.getValidation()) {
                            System.out.println("Vote Received");
                            session.getPeer().getBallotBox().setRequired(session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size()/2);
                            byte[] object = DatatypeConverter.parseBase64Binary(data.getString("vote"));
                            ByteArrayInputStream in = new ByteArrayInputStream(object);
                            ObjectInputStream is = new ObjectInputStream(in);
                            SignedVote vote = (SignedVote) is.readObject();
                     
                            Validator validator = session.getValidators().getValidator(vote.getStakeHash());
                            if (validator == null) return;
                            if (Float.compare(validator.getBalance(), session.getBlockValidator().getStakeRequirement()) < 0) return;
                            if (!vote.getHash().equals(DigestUtils.sha256Hex(vote.getDate() + String.valueOf(vote.getVote()) + vote.getAddress() + vote.getStakeHash()))) return;
                            if (!vote.getAddress().equals(DigestUtils.sha256Hex(vote.getKey().toByteArray()))) return;
                            if (!Cryptography.verify(vote.getSignature(), vote.getHash().getBytes(), vote.getKey())) return;
                            StakeContract contract = session.getBlockFileHandler().getStakeContract(vote.getStakeHash());
                            if (contract == null) return;
                            if (!contract.getAddress().equals(vote.getAddress())) return;
                            
                            object = DatatypeConverter.parseBase64Binary(data.getString("block"));
                            in = new ByteArrayInputStream(object);
                            is = new ObjectInputStream(in);
                            Block block = (Block) is.readObject();
                            
                            if (session.getBlockFileHandler().getBlock(block.getHash()) != null)
                                return;

                            session.getPeer().getBallotBox().addVote(block.getHash(),VoteType.BLOCK, vote);
                            int checkVote = session.getPeer().getBallotBox().checkVotes(block.getHash(),VoteType.BLOCK);
                            if (checkVote == VoteResult.YES) {System.out.println("Block Confirmed");session.getBlockchain().addBlock(block);}
                            if (checkVote == VoteResult.NO) {
                                System.out.println("Assessing Penalty");
                                Penalty penalty = session.getBlockchain().generatePenalty(contract, String.valueOf(System.currentTimeMillis()));
                                session.getBlockchain().addPendingTxn(penalty);
                                session.getBlockchain().addData(penalty);
                            }
                        }
                    }
                }
            }
        }
    }
}
