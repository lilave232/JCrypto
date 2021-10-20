/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Session;
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
@SuppressWarnings("serial")
public class IndexServlet extends HttpServlet {
    
    Session session;
    
    public IndexServlet(Session session) {
        this.session = session;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String content = "Not Instantiated";
        if (this.session.getWallet() == null) {}
        else {
            content = ("Balance: " + String.valueOf(this.session.getWallet().getUsableBalance()));
        }
        request.setAttribute("walletName",content);
        try {
            request.getRequestDispatcher("/html/index.jsp").forward(request,response);
        } catch (IllegalStateException ex) {}
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
 
        String content = "Not Instantiated";
        if (this.session.getWallet() == null) {}
        else {
            content = ("Balance: " + String.valueOf(this.session.getWallet().getUsableBalance()));
        }
        if (request.getParameter("action").equals("Create Folder")) {
            response.sendRedirect("/createFolder");
        } else if (request.getParameter("action").equals("Load Folder")) {
            response.sendRedirect("/loadFolder");
        } 
        try {
            request.getRequestDispatcher("/html/index.jsp").forward(request,response);
        } catch (IllegalStateException ex) {}
    }
}
