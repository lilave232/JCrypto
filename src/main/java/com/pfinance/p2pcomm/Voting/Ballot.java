/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Voting;

import java.util.ArrayList;

/**
 *
 * @author averypozzobon
 */
public class Ballot {
    String id = null;
    int type = -1;
    private int yes = 0;
    private int no = 0;
    ArrayList<String> votes = new ArrayList<String>();
    
    public Ballot(String id, int type) {
        this.id = id;
        this.type = type;
    }
    
    public synchronized void vote(SignedVote vote) {
        if (!votes.contains(vote.stakeHash)) {
            if (vote.vote == VoteResult.YES) {
                //System.out.println("Vote Yes");
                yes++;
            } else {
                //System.out.println("Vote No");
                no++; 
            }
            votes.add(vote.stakeHash);
        }
    }
    
    public String getId() {return id;}
    
    public synchronized int getNo() {return no;}
    public synchronized int getYes() {return yes;}
}
