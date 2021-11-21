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
import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Bid;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Base64;
import javax.json.Json;
import javax.xml.bind.DatatypeConverter;
import org.web3j.utils.Numeric;

/**
 *
 * @author averypozzobon
 */
public class AcceptBidServlet extends HttpServlet {
    
    Session session;
    
    public AcceptBidServlet(Session session) {
        this.session = session;
    }
    
    public Bid getBid(String path) {
        try {
            Bid bid = (Bid) new FileHandler().readObject(path);
            return bid;
        } catch (Exception e) {}
        return null;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            String jsonString = new String(Base64.getDecoder().decode(request.getParameter("acceptBid")));
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
            
            Bid bid = getBid(session.getPath() + "/contracts/listNFTs/" + nftHash + "/bids/" + jsonObject.get("saleTransaction").getAsString());
            
            if (bid==null) {throw new Exception();}
            
            //String time, Object saleTransaction, String previousHash, String nftHash, String transferToAddress, byte[] signature, BigInteger key
            NFTTransfer transfer = new NFTTransfer(time, bid, previousHash, nftHash, transferToAddress, contractSignature, key);
            
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
