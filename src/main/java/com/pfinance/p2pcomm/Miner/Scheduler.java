/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Miner;

import com.pfinance.p2pcomm.Blockchain.Block;
import com.pfinance.p2pcomm.FileHandler.Validator;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Session;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 *
 * @author averypozzobon
 */
public class Scheduler {
    private Session session;
    private HashMap<String,Long> schedule = new HashMap<>();
    private Long startTime;
    private Long endTime;
    
    public Scheduler(Session session) {
        this.session = session;
    }
    
    public void schedule() throws IOException, FileNotFoundException, ClassNotFoundException {
        ArrayList<Validator> validators = session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement());
        if (validators.isEmpty()) return;
        validators.sort(Comparator.comparing(Validator::getBalance).reversed());
        if (session.getBlockchain().getHashIndex().getHashes().isEmpty()) return;
        String lastHash = session.getBlockchain().getHashIndex().getHashes().get(session.getBlockchain().getHashIndex().getHashes().size()-1).hash;
        Block lastBlock = session.getBlockFileHandler().getBlock(lastHash);
        if (lastBlock == null)return;
        this.startTime = Long.valueOf(lastBlock.getTimestamp()) + 180000;
        Long currentTime = System.currentTimeMillis();
        if (currentTime > this.startTime) {
            this.startTime = currentTime - ((currentTime - this.startTime) % (validators.size() * 180000));
        }
        Long time = (this.startTime);
        int x = session.getBlockchain().getHashIndex().getHashes().size() % validators.size();
        for (int i = 0; i < validators.size(); i++) {
            Validator validator = validators.get(x);
            schedule.put(validator.getStakeHash(), time);
            time += 180000;
            x++;
            if (x == validators.size()) x = 0;
        }
        this.endTime = time;
    }
    
    public void clearSchedule() {
        schedule.clear();
    }
    
    public Long getEndTime() {return this.endTime;}
    public Long getMineTime(String stakeHash) {return this.schedule.getOrDefault(stakeHash, Long.valueOf(0));}
    public String toString() {
        StringBuffer val = new StringBuffer();
        schedule.forEach((K,V) -> {
            if (K.equals(this.session.getWallet().getStakeContract().getHash())) val.append(K).append("|").append(String.valueOf(V)).append("(Me)\n");
            else val.append(K).append("|").append(String.valueOf(V)).append("\n");
        });
        return val.toString();
    }
}
