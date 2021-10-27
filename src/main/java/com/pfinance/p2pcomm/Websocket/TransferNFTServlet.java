/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import com.pfinance.p2pcomm.Contracts.NFTTransfer;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class TransferNFTServlet extends HttpServlet {
    
    Session session;
    
    public TransferNFTServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            if (request.getParameter("nft-contract") != null && request.getParameter("transfer-address") != null && request.getParameter("pword") != null) {
                session.getWallet().loadKey(request.getParameter("pword"));
                NFTTransfer transfer = session.getWallet().transferNFT(new Transaction(), request.getParameter("nft-contract") , request.getParameter("transfer-address"));
                String object = DatatypeConverter.printBase64Binary(transfer.toBytes());
                JsonObject data = Json.createObjectBuilder().add("data", object).build();
                session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            }
            
        } catch (Exception ex) {
            Logger.getLogger(SendTransactionServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        response.sendRedirect("/nft");
    }
}
