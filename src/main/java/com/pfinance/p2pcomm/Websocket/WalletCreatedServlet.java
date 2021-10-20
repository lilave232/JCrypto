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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author averypozzobon
 */
public class WalletCreatedServlet extends HttpServlet {
    
    Session session;
    
    public WalletCreatedServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            if (request.getParameter("name") != null && request.getParameter("pword") != null) {
                List<Object> returnValue = new Wallet(this.session).createWallet(request.getParameter("name"),request.getParameter("pword"));
                String mnemonic = (String) returnValue.get(1);
                session.setWallet((Wallet) returnValue.get(0));
                request.setAttribute("mnemonic", mnemonic);
                request.getRequestDispatcher("/html/walletCreated.jsp").forward(request,response);
            }
            
        } catch (IllegalStateException ex) {
            Logger.getLogger(WalletCreatedServlet.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (FileNotFoundException ex) {
            Logger.getLogger(WalletCreatedServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(WalletCreatedServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
