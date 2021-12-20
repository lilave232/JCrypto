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
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import com.pfinance.p2pcomm.Wallet.Wallet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.utils.Numeric;
import java.math.BigDecimal;

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
            String jsonString = new String(Base64.getDecoder().decode(request.getParameter("transaction")));
            Gson gson = new Gson();
            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
            JsonArray inputs = jsonObject.get("inputs").getAsJsonArray();
            JsonArray outputs = jsonObject.get("outputs").getAsJsonArray();
            byte[] txnSignature = new byte[65];
            if (jsonObject.get("signature").getAsJsonObject().get("v") == null) {
                txnSignature = Cryptography.deriveSignature(jsonObject.get("signature").getAsJsonObject(), jsonObject.get("signature").getAsJsonObject().get("msg").getAsString().getBytes());
                //System.out.println("Transaction Hash Per Msg: " + jsonObject.get("signature").getAsJsonObject().get("msg").getAsString());
                if (txnSignature == null) {
                    System.out.println("ID Not Found");
                    System.out.println("Transaction Failed");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().close();
                }
            } else {
                txnSignature = Cryptography.generateSignatureRSV(jsonObject.get("signature").getAsJsonObject());
            }
            Transaction txn = new Transaction(jsonObject.get("timestamp").getAsString(), new BigInteger(jsonObject.get("signature").getAsJsonObject().get("public").getAsString()), txnSignature);
            for (int i = 0; i < inputs.size(); i++) {
                JsonObject object = inputs.get(i).getAsJsonObject();
                String previousTxn = object.get("previousTxnHash").getAsString();
                Integer index = object.get("outputIndex").getAsInt();
                int recId = 0;
                byte[] signature = new byte[65];
                
                TransactionInput input = new TransactionInput(previousTxn,index);
                UTXO utxo = session.getBlockFileHandler().loadUTXO(session.getPath() + "/utxos/" + input.previousTxnHash + "|" + String.valueOf(input.outputIndex));
                txn.addInput(input);
            }

            for (int i = 0; i < outputs.size(); i++) {
                JsonObject object = outputs.get(i).getAsJsonObject();
                String address = object.get("address").getAsString();
                BigDecimal value = object.get("value").getAsBigDecimal();

                TransactionOutput output = new TransactionOutput(address,value);
                txn.addOutput(output);
            }
            System.out.println("Transaction Hash Per Txn: " + txn.getMsg());
            String object = DatatypeConverter.printBase64Binary(txn.toBytes());
            javax.json.JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(txn);
                if (result) {session.getBlockchain().addPendingTxn(txn);System.out.println("Transaction Accepted");} 
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
