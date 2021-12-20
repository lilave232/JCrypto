/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import com.google.gson.JsonObject;
import com.pfinance.p2pcomm.Contracts.BorrowContract;
import com.pfinance.p2pcomm.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author averypozzobon
 */
public class GetFeeAmount   extends HttpServlet {
    
    Session session;
    
    public GetFeeAmount(Session session) {
        this.session = session;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        JsonObject json = new JsonObject();
        if (session.getMinFee() == null) {
            json.addProperty("minFee", BigDecimal.ZERO);
        } else {
            json.addProperty("minFee", session.getMinFee());
        }
        response.getWriter().print(json.toString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().close();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
            
    }
}
