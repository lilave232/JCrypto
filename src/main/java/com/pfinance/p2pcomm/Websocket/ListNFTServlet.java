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
import java.math.BigDecimal;

/**
 *
 * @author averypozzobon
 */
public class ListNFTServlet extends HttpServlet {
    
    Session session;
    
    public ListNFTServlet(Session session) {
        this.session = session;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            
            String jsonString = new String(java.util.Base64.getDecoder().decode(request.getParameter("nftList")));

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

            jsonObject = jsonObject.get("feeTransaction").getAsJsonObject();
            JsonArray inputs = jsonObject.get("inputs").getAsJsonArray();
            JsonArray outputs = jsonObject.get("outputs").getAsJsonArray();
            Transaction saleTransaction = new Transaction(jsonObject.get("timestamp").getAsString());
            for (int i = 0; i < inputs.size(); i++) {
                JsonObject object = inputs.get(i).getAsJsonObject();
                String previousTxn = object.get("previousTxnHash").getAsString();
                Integer index = object.get("outputIndex").getAsInt();
                JsonObject sigObject = object.get("outputSignature").getAsJsonObject();

                recId = sigObject.get("v").getAsInt();
                headerByte = recId + 27;
                v = (byte) headerByte;
                r = Numeric.toBytesPadded(new BigInteger(sigObject.get("r").getAsString()), 32);
                s = Numeric.toBytesPadded(new BigInteger(sigObject.get("s").getAsString()), 32);
                byte[] signature = new byte[65];
                System.arraycopy(r, 0, signature, 0, r.length);
                System.arraycopy(s, 0, signature, r.length, s.length);
                signature[64] = v;

                TransactionInput input = new TransactionInput(previousTxn,index,signature,new BigInteger(sigObject.get("public").getAsString()));

                saleTransaction.addInput(input);
            }

            for (int i = 0; i < outputs.size(); i++) {
                JsonObject object = outputs.get(i).getAsJsonObject();
                String address = object.get("address").getAsString();
                BigDecimal value = object.get("value").getAsBigDecimal();
                TransactionOutput output = new TransactionOutput(address,value);
                saleTransaction.addOutput(output);
            }

            //(String timestamp, String nftHash, Transaction validatorCommission, byte[] signature, BigInteger key)
            ListNFT list = new ListNFT(time, nftHash, saleTransaction, contractSignature, key);
            
            String object = DatatypeConverter.printBase64Binary(list.toBytes());
            javax.json.JsonObject objectData = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, objectData);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(list);
                if (result) {session.getBlockchain().addPendingTxn(list);System.out.println("Transaction Accepted");} 
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
