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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author averypozzobon
 */
public class ImportWalletServlet extends HttpServlet {
    
    Session session;
    
    public ImportWalletServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            request.getRequestDispatcher("/html/importWallet.jsp").forward(request,response);
        } catch (IllegalStateException ex) {}
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            if (request.getParameter("name") != null && request.getParameter("pword") != null && request.getParameter("mnemonic") != null) {
                session.setWallet(new Wallet(session).importWallet(request.getParameter("name"), request.getParameter("pword"), request.getParameter("mnemonic")));
                response.sendRedirect("/main");
            }
            
        } catch (IllegalStateException ex) {
            Logger.getLogger(WalletCreatedServlet.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (FileNotFoundException ex) {
            Logger.getLogger(WalletCreatedServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ImportWalletServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}