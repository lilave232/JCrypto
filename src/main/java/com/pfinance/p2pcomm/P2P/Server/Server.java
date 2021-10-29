package com.pfinance.p2pcomm.P2P.Server;


import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.P2P.Peer.Peer;
import com.pfinance.p2pcomm.Voting.BallotBox;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author averypozzobon
 */
public class Server extends Thread {
    public static String port = null;
    private ServerSocket serverSocket = null;
    public static String hostName = null;
    public static String hostAddress = null;
    private final Set<ServerThread> serverThreads;
    private final Peer peer;
    private final ServerMessageHandler handler;
    
    public Server(String address, String port, Peer peer) throws IOException {
        this.handler = new ServerMessageHandler(this);
        this.serverThreads = new HashSet<>();
        this.port = port;
        this.serverSocket = new ServerSocket(Integer.valueOf(this.port));
        this.hostName = address;
        this.peer = peer;
    }
    
    @SuppressWarnings("deprecation") 
    @Override
    public void run() {
        try {
            while(!this.isInterrupted()) {
                ServerThread serverThread = new ServerThread(serverSocket.accept(),this);
                serverThreads.add(serverThread);
                serverThread.start();
            }
        } catch (IOException e) {serverThreads.forEach(t->t.stop());}
    }
    
    public void sendMessage(Integer type, JsonObject data) {
        try {serverThreads.forEach(t->{t.sendMessage(type, data);});
        } catch (Exception e) {}
    }
    
    public void stopThread() throws IOException {
        this.serverSocket.close();
        serverThreads.clear();
        interrupt();
    }
    
    public ServerSocket getServerSocket() {return this.serverSocket;}
    public Set<ServerThread> getServerThreads() {return serverThreads;}
    public Peer getPeer() {return this.peer;}
    public ServerMessageHandler getHandler() {return this.handler;}
    @Override public String toString() {return hostName + ":" + port;}
    
}
