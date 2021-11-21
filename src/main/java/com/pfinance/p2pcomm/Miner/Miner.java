/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Miner;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Voting.SignedVote;
import com.pfinance.p2pcomm.Voting.VoteResult;
import com.pfinance.p2pcomm.Voting.VoteType;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author averypozzobon
 */
public class Miner {
    private Session session;
    private Timer t;
    private TimerTask tt;
    
    
    public Miner(Session session) {
        this.session = session;
    }
    
    public void startMiner() throws IOException, ClassNotFoundException { 
        t = new Timer();
        System.out.println("Starting Miner");
        tt = new TimerTask() {  
            @Override  
            public void run() {  
                try {
                    System.out.println(String.valueOf(System.currentTimeMillis()) + ": Mining");
                    mine();
                } catch (Exception e) {e.printStackTrace();}
            };  
        };
        Long mineTime = this.session.getScheduler().getMineTime(this.session.getWallet().getStakeContract().getHash());
        Date d = new Date(mineTime);
        if ((mineTime + 180000) > System.currentTimeMillis()) {
            System.out.println("Scheduled Time: " + String.valueOf(mineTime));
            t.schedule(tt, d);
        }
        
        
        tt = new TimerTask() {  
            @Override  
            public void run() {  
                try {
                    System.out.println(String.valueOf(System.currentTimeMillis()) + ": Resetting");
                    resetMiner();
                } catch (Exception e) {e.printStackTrace();}
            };  
        };
        d = new Date(this.session.getScheduler().getEndTime());
        System.out.println("Time for Reset: " + String.valueOf(this.session.getScheduler().getEndTime()));
        t.schedule(tt, d);
    }
    
    public void stopMiner() {
        System.out.println("Stopping Miner");
        if (tt != null) tt.cancel();
        t.cancel();
        t.purge();
    }
    
    public void resetMiner() throws Exception {
        this.session.getBlockchain().newBlock(this.session.getWallet().getStakeContract().getHash(), this.session.getWallet().getKey());
    }
    
    public void mine() throws IOException, Exception {
        synchronized (session.getPeer().getServer().getHandler()) {
            session.getPeer().getBallotBox().setRequired(session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size()/2);
            boolean result = session.getBlockchain().verifyBlock(session.getBlockchain().block);
            if (!result) {
                System.out.println("Could not validate block");
                //session.getBlockchain().newBlock(session.getWallet().getStakeContract().getHash(), session.getWallet().getKey());
                return;
            }
            String object = DatatypeConverter.printBase64Binary(session.getBlockchain().block.toBytes());
            JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BLOCKVALIDATIONREQUEST, data);
            int voteResult = VoteResult.NO;
            if (result) voteResult = VoteResult.YES;
            SignedVote vote = new SignedVote(voteResult,session.getWallet().getAddress(),session.getWallet().getStakeContract().getHash(),session.getWallet().getKey().getKey());
            session.getPeer().getBallotBox().addVote(session.getBlockchain().block.getHash(),VoteType.BLOCK, vote);
            String voteObject = DatatypeConverter.printBase64Binary(vote.toBytes());
            JsonObject value = Json.createObjectBuilder().add("block", data.getString("data")).add("vote", voteObject).build();
            session.getPeer().sendMessage(Message.BLOCKVALIDATIONVOTE, value);
            int checkVote = session.getPeer().getBallotBox().checkVotes(session.getBlockchain().block.getHash(),VoteType.BLOCK);
            if (checkVote == VoteResult.YES) {
                System.out.println("Block Confirmed");
                session.getBlockchain().addBlock(session.getBlockchain().block);
                session.getPeer().getBallotBox().clearVotes(VoteType.BLOCK);
                String hashObject = StringEscapeUtils.escapeJson(DatatypeConverter.printBase64Binary(this.session.getBlockchain().getHashIndex().toBytes()));
                JsonObject hashes = Json.createObjectBuilder().add("hashes", hashObject).build();
                session.getPeer().getServer().sendMessage(Message.DOWNLOADHASHRECEIVED, hashes);
            }
        }
    }
    
    public boolean isNull() {
        if (this.t == null) return true;
        else return false;
    }
    
}
