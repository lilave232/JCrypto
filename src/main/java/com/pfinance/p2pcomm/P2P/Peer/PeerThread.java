/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.P2P.Peer;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.P2P.Server.Server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author averypozzobon
 */
public class PeerThread extends Thread {
    private BufferedReader bufferedReader;
    private int port;
    private int localPort;
    private String localHostAddress = null;
    private String hostAddress = null;
    private PrintWriter printWriter;
    Peer peer;
    
    public PeerThread(Socket socket, Peer peer) throws IOException, Exception {
        this.localHostAddress = socket.getLocalAddress().getHostName();
        this.localPort = socket.getLocalPort();
        this.port = socket.getPort();
        this.hostAddress = socket.getInetAddress().getHostName();
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.printWriter = new PrintWriter(socket.getOutputStream(), true);
        this.peer = peer;
        sendConnectionMessage();
        sendDownloadMessage();
        sendRequestPendingMessage();
    }
    
    public void sendConnectionMessage() {
        JsonObject object = Json.createObjectBuilder()
                                                .add("hostName", Server.hostName)
                                                .add("hostPort", Server.port)
                                                .build();
        this.sendMessage(Message.CONNECTION,object);
    }
    
    public void sendDownloadMessage() {
        JsonObject data = Json.createObjectBuilder().add("data", "Download") .build();
        this.sendMessage(Message.DOWNLOADHASHREQUEST, data);
    }
    
    public void sendRequestPendingMessage() throws Exception {
        this.sendMessage(Message.REQUESTPENDING, Json.createObjectBuilder().add("data", Message.REQUESTPENDING).build());
    }

    public void run() {
        boolean flag = true;
        while (flag) {
            try {
                this.peer.getHandler().handle(bufferedReader,this);
            } catch (Exception e) {
                System.out.println("Incoming Connection Closed");
                this.peer.getPeerThreads().remove(this);
                e.printStackTrace();
                flag = false;
                interrupt();
            }
        }
    }
    
    void sendMessage(Integer type, JsonObject data) {
        try {
            StringWriter stringWriter = new StringWriter();
            Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                                        .add("type", type)
                                        .add("data", data)
                                        .build());
            printWriter.println(stringWriter.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public int getPort() {return port;}
    public String getHostAddress() {return hostAddress;}
    public String toString() {return this.hostAddress + ":" + this.port;}
    
}
