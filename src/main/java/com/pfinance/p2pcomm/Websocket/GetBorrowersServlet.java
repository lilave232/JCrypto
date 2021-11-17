/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import com.pfinance.p2pcomm.Contracts.BorrowContract;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Statistics.TransactionInOut;
import com.pfinance.p2pcomm.Transaction.UTXO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.web3j.utils.Numeric;

/**
 *
 * @author averypozzobon
 */
public class GetBorrowersServlet  extends HttpServlet {
    
    Session session;
    
    public GetBorrowersServlet(Session session) {
        this.session = session;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String responseValue = "{\"borrowers\":[";
        BorrowContract[] contracts;
        try {
            contracts = session.getBlockFileHandler().getBorrowContractObjects();
            for (int i = 0; i < contracts.length; i++) {
                if (contracts[i] != null) {
                    responseValue += "{\"hash\":\"" + contracts[i].getHash() + "\",\"borrowerAddress\":\"" + contracts[i].getBorrowerAddress() + "\"}";
                    if (i < contracts.length-1)
                        responseValue +=",";
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GetBorrowersServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        responseValue += "]}";
        response.getWriter().print(responseValue);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().close();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
            
    }
}
