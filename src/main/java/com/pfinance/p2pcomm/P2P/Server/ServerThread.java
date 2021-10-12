package com.pfinance.p2pcomm.P2P.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
public class ServerThread extends Thread {
    private Server server;
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    
    public ServerThread(Socket socket, Server server) throws UnknownHostException,IOException {
        this.server = server;
        this.socket = socket;
    }
    
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.printWriter = new PrintWriter(socket.getOutputStream(), true);
            while(true) {this.server.getHandler().handle(bufferedReader,this);}
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Outgoing Connection Closed");
            server.getServerThreads().remove(this);
            interrupt();
        }
    }
    
    public void sendMessage(Integer type, JsonObject data) {
        try {
            if (this.getPrintWriter() != null) {
                StringWriter stringWriter = new StringWriter();
                Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                                            .add("type", type)
                                            .add("data", data)
                                            .build());
                this.getPrintWriter().println(stringWriter.toString());
            }
        } catch (Exception e) {}
    }
    
    public Server getServer() {return this.server;}
    public PrintWriter getPrintWriter() {return printWriter;}
    @Override public String toString() {return socket.getInetAddress().getHostName() + ":" + socket.getPort();}
}
