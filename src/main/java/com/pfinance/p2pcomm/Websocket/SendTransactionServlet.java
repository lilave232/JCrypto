/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Wallet.Wallet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author averypozzobon
 */
public class SendTransactionServlet extends HttpServlet {
    
    Session session;
    
    public SendTransactionServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            if (request.getParameter("receiver") != null && request.getParameter("amount") != null && request.getParameter("fee") != null) {
                ArrayList<TransactionOutput> outputs = new TransactionOutput(request.getParameter("receiver"),Float.valueOf(request.getParameter("amount"))).toList();
                session.getWallet().loadKey(request.getParameter("pword"));
                Transaction txn = session.getWallet().createTransaction(outputs,Float.valueOf(request.getParameter("fee")));
                String object = DatatypeConverter.printBase64Binary(txn.toBytes());
                JsonObject data = Json.createObjectBuilder().add("data", object).build();
                session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            }
            
        } catch (Exception ex) {
            Logger.getLogger(SendTransactionServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        response.sendRedirect("/main");
    }
    
}
