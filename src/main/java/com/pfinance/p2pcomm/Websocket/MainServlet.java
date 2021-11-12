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
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author averypozzobon
 */
public class MainServlet extends HttpServlet {
    
    Session session;
    
    public MainServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (session.getPath() == null) {response.sendRedirect("/");}
        else if (session.getWallet() == null) {response.sendRedirect("/selectWallet");}
        else {
            try {
                if (session.getPeer() == null) {
                    session.connectPeer(InetAddress.getLocalHost().getHostName(), "7777");
                }
            } catch (Exception ex) {
                Logger.getLogger(MainServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
            //request.setAttribute("pathName", session.getPath());
            //request.setAttribute("wallet", session.getWallet());
            //request.setAttribute("transactions", session.getStats().getWalletInOuts());
            //try {
            //    request.getRequestDispatcher("/html/main.jsp").forward(request,response);
            //} catch (IllegalStateException ex) {}
            //response.setContentType("application/json");
            //response.setCharacterEncoding("UTF-8");
            //response.getWriter().print("{ \"my_data\": \"Hello from Java!\" }");
            //response.setStatus(HttpServletResponse.SC_OK);
            //response.getWriter().close();
        }
        
    }
}
