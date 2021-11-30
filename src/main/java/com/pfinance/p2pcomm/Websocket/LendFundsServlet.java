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
import com.pfinance.p2pcomm.Contracts.LendContract;
import static com.pfinance.p2pcomm.Main.session;
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
import java.util.Base64;
import javax.json.Json;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;
import java.math.BigDecimal;

/**
 *
 * @author averypozzobon
 */
public class LendFundsServlet extends HttpServlet {
    
    Session session;
    
    public LendFundsServlet(Session session) {
        this.session = session;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            String jsonString = new String(Base64.getDecoder().decode(request.getParameter("lendContract")));
            Gson gson = new Gson();
            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
            String lenderAddress = jsonObject.get("lenderAddress").getAsString();
            String borrowContractHash = jsonObject.get("borrowContractHash").getAsString();
            
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
            
            String timestamp = jsonObject.get("inceptionDate").getAsString();
            
            BigInteger key = new BigInteger(jsonObject.get("key").getAsString());
            
            
            jsonObject = jsonObject.get("lendTransaction").getAsJsonObject();
            JsonArray inputs = jsonObject.get("inputs").getAsJsonArray();
            JsonArray outputs = jsonObject.get("outputs").getAsJsonArray();
            Transaction txn = new Transaction(jsonObject.get("timestamp").getAsString());
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
                
                txn.addInput(input);
            }

            for (int i = 0; i < outputs.size(); i++) {
                JsonObject object = outputs.get(i).getAsJsonObject();
                String address = object.get("address").getAsString();
                BigDecimal value = object.get("value").getAsBigDecimal();
                TransactionOutput output = new TransactionOutput(address,value);
                txn.addOutput(output);
            }
            
            LendContract lcontract = new LendContract(timestamp, lenderAddress, borrowContractHash, txn, contractSignature, key);
            
            String object = DatatypeConverter.printBase64Binary(lcontract.toBytes());
            javax.json.JsonObject data = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, data);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(lcontract);
                if (result) {session.getBlockchain().addPendingTxn(lcontract);System.out.println("Transaction Accepted");} 
                else {
                    System.out.println("Transaction Failed");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().close();
                } 
            }
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().close();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().close();
        }
    }
}
