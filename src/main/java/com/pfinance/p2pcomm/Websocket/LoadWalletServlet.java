/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Wallet.Wallet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author averypozzobon
 */
public class LoadWalletServlet  extends HttpServlet {
    
    Session session;
    
    public LoadWalletServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        request.setAttribute("walletFolders", session.getBlockFileHandler().getWallets());
        try {
            request.getRequestDispatcher("/html/loadWallet.jsp").forward(request,response);
        } catch (IllegalStateException ex) {}
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (request.getParameter("wallet") != null) {
            try {
                session.setWallet(new Wallet(session).loadWallet(request.getParameter("wallet")));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(CreateFolderServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
            response.sendRedirect("http://localhost:8080/main");
        }
        
        //request.setAttribute("chainFolders", session.getBlockFileHandler().getBlockchains());
        //request.getRequestDispatcher("/html/loadFolder.jsp").forward(request,response);
    }
}
