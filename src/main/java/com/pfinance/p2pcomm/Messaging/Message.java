/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Messaging;

/**
 *
 * @author averypozzobon
 */
public class Message {
    public static final int STRING = 0;
    public static final int CONNECTION = 1;
    public static final int MINE = 2;
    public static final int VERIFY = 3;
    public static final int NVERIFY = 4;
    public static final int DOWNLOADHASHREQUEST = 5;
    public static final int DOWNLOADHASHRECEIVED = 6;
    public static final int DOWNLOADBLOCKREQUEST = 7;
    public static final int DOWNLOADBLOCKRECEIVED = 8;
    public static final int BROADCASTTXN = 9;
    public static final int TXNACCEPTED = 10;
    public static final int TXNFAILED = 11;
    public static final int BLOCKVALIDATIONREQUEST = 12;
    public static final int BLOCKVALIDATIONVOTE = 13;
    public static final int BROADCASTTXNPENDING = 14;
    public static final int REQUESTPENDING = 15;
    public static final int PENALTYASSESSED = 16;
}
