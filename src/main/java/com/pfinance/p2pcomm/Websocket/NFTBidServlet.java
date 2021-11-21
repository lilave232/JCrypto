/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.pfinance.p2pcomm.Contracts.NFT;
import com.pfinance.p2pcomm.Contracts.NFTTransfer;
import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.FileHandler.HashIndex;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Bid;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import com.pfinance.p2pcomm.Wallet.Key;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.digest.DigestUtils;
import org.web3j.utils.Numeric;

/**
 *
 * @author averypozzobon
 */
public class NFTBidServlet  extends HttpServlet {
    
    Session session;
    
    public NFTBidServlet(Session session) {
        this.session = session;
    }
    
    public Bid getBid(String path) {
        try {
            Bid bid = (Bid) new FileHandler().readObject(path);
            return bid;
        } catch (Exception e) {}
        return null;
    }
    
    public ArrayList<Bid> getBids(String nftHash) {
        ArrayList<Bid> returnBids = new ArrayList<Bid>();
        File f = new File(session.getPath() + "/contracts/listNFTs/" + nftHash + "/bids");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return new ArrayList<Bid>();
        for (File file : files) {
            returnBids.add(getBid(file.getPath()));
        }
        return returnBids;
    } 
    
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            
            HashMap<String,Integer> returnMap = new HashMap<String,Integer>();
            ArrayList<Bid> bids = getBids(request.getParameter("nft"));
            
            GsonBuilder gsonBuilder = new GsonBuilder();
            JsonSerializer<Bid> serializer = new JsonSerializer<Bid>() {
                    @Override
                    public JsonElement serialize(Bid bid, Type typeOfSrc, JsonSerializationContext context) {
                        JsonObject json = new JsonObject();
                        json.addProperty("hash", bid.getHash());
                        if (bid.getTransaction().getOutputs().size() > 0) {
                            json.addProperty("amount", bid.getTransaction().getOutputs().get(0).value);
                            json.addProperty("address",DigestUtils.sha256Hex(bid.getKey().toByteArray()));
                        }
                        return json;
                    }
            };
            
            Type bidArrayListType = new TypeToken<Bid>() {}.getType(); 
            gsonBuilder.registerTypeAdapter(bidArrayListType, serializer);
            Gson customGson = gsonBuilder.create();
            JsonArray bidArray = new JsonArray();
            
            bids.stream().map(bid -> customGson.toJson(bid, Bid.class)).map(jsonString -> new JsonParser().parse(jsonString)).forEachOrdered(object -> {
                bidArray.add(object);
            });
            
            JsonObject object = new JsonObject();
            object.add("bids", bidArray);
            
            response.getWriter().print(object);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().close();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().close();
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            
            String jsonString = new String(java.util.Base64.getDecoder().decode(request.getParameter("bid")));

            Gson gson = new Gson();

            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
            
            String time = jsonObject.get("timestamp").getAsString();

            BigInteger key = new BigInteger(jsonObject.get("key").getAsString());

            String nftHash = jsonObject.get("contractHash").getAsString();

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

            jsonObject = jsonObject.get("transaction").getAsJsonObject();
            JsonArray inputs = jsonObject.get("inputs").getAsJsonArray();
            JsonArray outputs = jsonObject.get("outputs").getAsJsonArray();
            Transaction bidTransaction = new Transaction(jsonObject.get("timestamp").getAsString());
            for (int i = 0; i < inputs.size(); i++) {
                JsonObject object = inputs.get(i).getAsJsonObject();
                String previousTxn = object.get("previousTxnHash").getAsString();
                Integer index = object.get("outputIndex").getAsInt();

                TransactionInput input = new TransactionInput(previousTxn,index,null,key);

                bidTransaction.addInput(input);
            }

            for (int i = 0; i < outputs.size(); i++) {
                JsonObject object = outputs.get(i).getAsJsonObject();
                String address = object.get("address").getAsString();
                Float value = object.get("value").getAsFloat();
                TransactionOutput output = new TransactionOutput(address,value);
                bidTransaction.addOutput(output);
            }

            //String time, String contractHash, Transaction transaction, byte[] signature, BigInteger key
            Bid bid = new Bid(time, nftHash, bidTransaction, contractSignature, key);
            
            String object = DatatypeConverter.printBase64Binary(bid.toBytes());
            javax.json.JsonObject objectData = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, objectData);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(bid);
                if (result) {session.getBlockchain().addPendingTxn(bid);System.out.println("Transaction Accepted");} 
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

