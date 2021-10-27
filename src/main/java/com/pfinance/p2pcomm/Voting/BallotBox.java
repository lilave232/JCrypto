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
public class BallotBox {
    ArrayList<Ballot> ballots = new ArrayList<>();
    int requiredVotes = 0;
    
    public BallotBox() {
        
    }
    
    public void setRequired(int req) {
        this.requiredVotes = req;
    }
    
    public synchronized void addVote(String id, int type, SignedVote vote) {
        Ballot ballot = getBallot(id,type);
        ballot.vote(vote);
    }
    
    public synchronized int checkVotes(String id, int type) {
        Ballot ballot = getBallot(id, type);
        //System.out.println("Yes Votes:" + ballot.getYes());
        //System.out.println("No Votes:" + ballot.getNo());
        //System.out.println("Required Votes:" + this.requiredVotes);
        if (ballot.getYes() > this.requiredVotes)
            return VoteResult.YES;
        else if (ballot.getNo() > this.requiredVotes)
            return VoteResult.NO;
        else
            return VoteResult.ONGOING;
    }
    
    public void clearVotes(int type) {
        ballots.removeIf(vote -> vote.type == type);
    }
    
    private Ballot addBallot(String id, int type) {
        Ballot ballot = new Ballot(id, type);
        this.ballots.add(ballot);
        return ballot;
    }
    
    private Ballot getBallot(String id, int type) {
        for (int i = 0; i < ballots.size(); i++)
            if (ballots.get(i).getId().equals(id))
                return ballots.get(i);
        return addBallot(id, type);
    }
}
