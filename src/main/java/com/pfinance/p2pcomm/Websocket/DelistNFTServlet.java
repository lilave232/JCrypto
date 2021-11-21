/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pfinance.p2pcomm.Contracts.DelistNFT;
import com.pfinance.p2pcomm.Contracts.ListNFT;
import com.pfinance.p2pcomm.Contracts.NFTTransfer;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import javax.json.Json;
import javax.xml.bind.DatatypeConverter;
import org.web3j.utils.Numeric;

/**
 *
 * @author averypozzobon
 */
public class DelistNFTServlet extends HttpServlet {
    
    Session session;
    
    public DelistNFTServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            
            String jsonString = new String(java.util.Base64.getDecoder().decode(request.getParameter("nftDelist")));

            Gson gson = new Gson();

            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();

            String time = jsonObject.get("timestamp").getAsString();

            BigInteger key = new BigInteger(jsonObject.get("key").getAsString());

            String nftHash = jsonObject.get("nftHash").getAsString();

            JsonObject contractSigObject = jsonObject.get("signature").getAsJsonObject();

            int recId = contractSigObject.get("v").getAsInt();
            int headerByte = recId + 27;
            byte v = (byte) headerByte;
            byte[] r = Numeric.toBytesPadded(new BigInteger(contractSigObject.get("r").getAsString()), 32);
            byte[] s = Numeric.toBytesPadded(new BigInteger(contractSigObject.get("s").getAsString()), 32);
            byte[] contractSignature = new byte[65];
            System.arraycopy(r, 0, contractSignature, 0, r.length);
            System.arraycopy(s, 0, contractSignature, r.length, s.length);
            contractSignature[64] = v;

            //(String timestamp, String nftHash, Transaction validatorCommission, byte[] signature, BigInteger key)
            DelistNFT delist = new DelistNFT(time, nftHash, contractSignature, key);
            
            String object = DatatypeConverter.printBase64Binary(delist.toBytes());
            javax.json.JsonObject objectData = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, objectData);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(delist);
                if (result) {session.getBlockchain().addPendingTxn(delist);System.out.println("Transaction Accepted");} 
                else {
                    System.out.println("Transaction Failed");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().close();
                } 
            }
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().close();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().close();
        }

    }
}
