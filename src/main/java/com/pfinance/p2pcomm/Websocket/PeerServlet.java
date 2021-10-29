/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.P2P.Peer.PeerThread;
import com.pfinance.p2pcomm.P2P.Server.Server;
import com.pfinance.p2pcomm.P2P.Server.ServerThread;
import com.pfinance.p2pcomm.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author averypozzobon
 */
public class PeerServlet extends HttpServlet {
    
    Session session;
    
    public PeerServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (session.getPath() == null) {response.sendRedirect("/");}
        else {
            if (session.getWallet() == null) {
                request.setAttribute("walletActive", false);
            } else {
                request.setAttribute("walletActive", true);
            }
            request.setAttribute("peerConnected", session.getPeer() != null);
            if (session.getPeer() != null) {
                request.setAttribute("localAddress", session.getPeer().getServer().hostName);
                request.setAttribute("peerPort", session.getPeer().getServer().port);
            }
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
            request.setAttribute("incomingThreads", serverThreads);
            request.setAttribute("outgoingThreads", peerThreads);
            try {
                request.getRequestDispatcher("/html/peer.jsp").forward(request,response);
            } catch (IllegalStateException ex) {}
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            if (request.getParameter("action").equals("Connect Peer")) {
                session.connectPeer(InetAddress.getLocalHost().getHostName(), request.getParameter("port"));
            }
            if (request.getParameter("action").equals("Disconnect Peer")) {
                session.disconnectPeer();
            }
            request.getRequestDispatcher("/html/index.jsp").forward(request,response);
        } catch (IllegalStateException ex) {} catch (Exception ex) {
            Logger.getLogger(PeerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
