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
import com.pfinance.p2pcomm.Contracts.NFTTransfer;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.xml.bind.DatatypeConverter;
import org.web3j.utils.Numeric;

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
            
            String jsonString = new String(java.util.Base64.getDecoder().decode(request.getParameter("nftTransfer")));

            Gson gson = new Gson();

            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();

            String time = jsonObject.get("transferDate").getAsString();

            BigInteger key = new BigInteger(jsonObject.get("key").getAsString());

            String previousHash = jsonObject.get("previousHash").getAsString();

            String nftHash = jsonObject.get("nftHash").getAsString();

            String transferToAddress = jsonObject.get("transferToAddress").getAsString();

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

            jsonObject = jsonObject.get("saleTransaction").getAsJsonObject();
            JsonArray inputs = jsonObject.get("inputs").getAsJsonArray();
            JsonArray outputs = jsonObject.get("outputs").getAsJsonArray();
            byte[] txnSignature = new byte[65];
            if (jsonObject.get("signature").getAsJsonObject().get("v") == null) {
                txnSignature = Cryptography.deriveSignature(jsonObject.get("signature").getAsJsonObject(), jsonObject.get("signature").getAsJsonObject().get("msg").getAsString().getBytes());
                if (txnSignature == null) {
                    System.out.println("ID Not Found");
                    System.out.println("Transaction Failed");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().close();
                }
            } else {
                txnSignature = Cryptography.generateSignatureRSV(jsonObject.get("signature").getAsJsonObject());
            }
            Transaction saleTransaction = new Transaction(jsonObject.get("timestamp").getAsString(), new BigInteger(jsonObject.get("signature").getAsJsonObject().get("public").getAsString()),txnSignature);
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

                TransactionInput input = new TransactionInput(previousTxn,index);

                saleTransaction.addInput(input);
            }

            for (int i = 0; i < outputs.size(); i++) {
                JsonObject object = outputs.get(i).getAsJsonObject();
                String address = object.get("address").getAsString();
                BigDecimal value = object.get("value").getAsBigDecimal();
                TransactionOutput output = new TransactionOutput(address,value);
                saleTransaction.addOutput(output);
            }

            //String time, Object saleTransaction, String previousHash, String nftHash, String transferToAddress, byte[] signature, BigInteger key
            NFTTransfer transfer = new NFTTransfer(time, saleTransaction, previousHash, nftHash, transferToAddress, contractSignature, key);
            
            String object = DatatypeConverter.printBase64Binary(transfer.toBytes());
            javax.json.JsonObject objectData = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, objectData);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(transfer);
                if (result) {session.getBlockchain().addPendingTxn(transfer);System.out.println("Transaction Accepted");} 
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
