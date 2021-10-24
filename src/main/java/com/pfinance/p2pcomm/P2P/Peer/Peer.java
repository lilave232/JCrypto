/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.P2P.Peer;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.P2P.Server.Server;
import com.pfinance.p2pcomm.P2P.Server.ServerThread;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Voting.BallotBox;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class Peer {
    
    private BallotBox box = new BallotBox();
    private Server server;
    private Session session = null;
    private ArrayList<PeerThread> peerThreads = new ArrayList<>();
    private PeerMessageHandler handler = new PeerMessageHandler(this);
    public static ArrayList<String> knownPeers = new ArrayList<>();
    
    public Peer(Session session) {
        this.session = session;
    }
    
    public void connect(String port) throws IOException {
        server = new Server(port,this);
        server.start();
        loadPeerFile();
    }
    
    public void disconnect() throws IOException {
        Thread[] threads = new Thread[Thread.currentThread().getThreadGroup().activeCount()];
        Thread.currentThread().getThreadGroup().enumerate(threads);
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] instanceof PeerThread) {
                ((PeerThread)threads[i]).stopThread();
                System.out.println("Stopping Peer Thread");
            }
            else if (threads[i] instanceof ServerThread) {
                ((ServerThread)threads[i]).stopThread();
                System.out.println("Stopping Server Thread");
            } else if (threads[i] instanceof Server) {
                ((Server)threads[i]).stopThread();
                System.out.println("Stopping Server");
            }
        }
    }
    
    public void addPeerListener() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("> enter (space separated) hostname:port# peers to receive messages from, s:kip");
        String input = bufferedReader.readLine();
        String[] inputValues = input.split(" ");
        if (!input.equals("s")) for (int i = 0; i < inputValues.length; i++) {
            String[] address = inputValues[i].split(":");
            Socket socket = null;
            try {
                addListener(address[0],address[1]);
            } catch (Exception e) {
                if (socket != null) socket.close();
                else System.out.println("invalid input. skipping to next step.");
            }
        }
    }
    
    public synchronized void addListener(String address, String port) throws Exception {
        Socket socket = null;
        try {
            if (!validatePeers(address + ":" + port)) {
                System.out.println("Connecting to: " + address + ":" + port);
                socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByName(address),Integer.valueOf(port)), 2000);
                PeerThread peerThread = new PeerThread(socket, this);
                peerThread.start();
                peerThreads.add(peerThread);
                writePeerToFile(address, port);
            }
        } catch (Exception e) {
            if (socket != null) socket.close();
            else System.out.println("invalid input. skipping to next step.");
        }
    }
    
    private void loadPeerFile()  {
        try {
            BufferedReader br = new BufferedReader(new FileReader(session.getPath() + "/knownPeers"));
            String line;
            while ((line = br.readLine()) != null) {
               String[] address = line.split(":");
                Socket socket = null;
                try {
                    knownPeers.add(line);
                    addListener(address[0],address[1]);
                } catch (Exception e) {
                    if (socket != null) socket.close();
                    else System.out.println("invalid input. skipping to next step.");
                }
            }
        } 
        catch (FileNotFoundException e) {} 
        catch (IOException e) {
            System.err.println(e);
        }
    }
    
    private void writePeerToFile(String address, String port) {
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(session.getPath() + "/knownPeers", true)))) {
            if (knownPeers.contains(address + ":" + port)) return;
            out.println(address + ":" + port);
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    synchronized boolean validatePeers(String inputValue) {
        boolean flag = false;
        Thread[] threads = new Thread[Thread.currentThread().getThreadGroup().activeCount()];
        Thread.currentThread().getThreadGroup().enumerate(threads);
        ArrayList<String> peers = new ArrayList<String>();
        for (int i = 0; i < threads.length;i++) {
            if (threads[i] instanceof PeerThread && !peers.contains(((PeerThread)threads[i]).toString())) {
                peers.add(((PeerThread)threads[i]).toString());
            }
            else if (threads[i] instanceof Server && !peers.contains(((Server)threads[i]).toString()))
                peers.add(((Server)threads[i]).toString());
        }
        if (peers.contains(inputValue)) flag=true;
        return flag;
    }
    
    public void sendMessage(Integer type, JsonObject data) {
        try {
            peerThreads.forEach(t->{
                t.sendMessage(type, data);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void listConnections() {
        Thread[] threads = new Thread[Thread.currentThread().getThreadGroup().activeCount()];
        Thread.currentThread().getThreadGroup().enumerate(threads);
        ArrayList<PeerThread> peerThreads = new ArrayList<PeerThread>();
        ArrayList<ServerThread> serverThreads = new ArrayList<ServerThread>();
        ArrayList<Server> servers = new ArrayList<Server>();
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] instanceof PeerThread) peerThreads.add((PeerThread)threads[i]);
            else if (threads[i] instanceof ServerThread) serverThreads.add((ServerThread)threads[i]);
            else if (threads[i] instanceof Server) servers.add((Server)threads[i]);
        }
        if (!peerThreads.isEmpty()) {
            System.out.println("Outgoing Connections: " + String.valueOf(peerThreads.size()));
            //System.out.println(String.format("%-20s","PeerThread") + " | (Connected to) ServerThread\n" + 
            //                    "---------------------------------------------------------------------");
            //peerThreads.forEach(x -> System.out.println(String.format("%-20s",x) + " | " + x.getHostAddress() + ":" + x.getPort()));
        } else System.out.println("This peer is not listening to messages coming from any other peer");
        if (!serverThreads.isEmpty()) {
            System.out.println("Incoming Connections: " + String.valueOf(serverThreads.size()));
            //System.out.println(String.format("%-20s","Server") + " | ServerThread | (Connected to) PeerThread\n" + 
            //                    "---------------------------------------------------------------------");
            //for (int i = 0; i < serverThreads.size(); i++) {
            //    System.out.println(String.format("%-20s",servers.get(0)) + " | #" + (i+1) + "            | " + serverThreads.get(i));
            //}
        } else System.out.println("No other peers listening to messages coming from this Peer");
    }
    
    public BallotBox getBallotBox() {return this.box;}
    public Server getServer() {return this.server;}
    public Session getSession() {return this.session;}
    public ArrayList<PeerThread> getPeerThreads() {return this.peerThreads;}
    public PeerMessageHandler getHandler() {return this.handler;}
}
