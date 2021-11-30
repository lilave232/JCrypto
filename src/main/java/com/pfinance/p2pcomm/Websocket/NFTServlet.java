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
import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.FileHandler.HashIndex;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.json.Json;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jetty.server.Request;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.web3j.utils.Numeric;
import java.math.BigDecimal;

/**
 *
 * @author averypozzobon
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024,maxFileSize = 1024 * 1024 * 5,maxRequestSize = 1024 * 1024 * 5 * 5)
public class NFTServlet extends HttpServlet {
    
    private static final String UPLOAD_DIRECTORY = "upload";
    private static final int THRESHOLD_SIZE     = 1024 * 1024 * 3;  // 3MB
    private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
    private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB
    
    Session session;
    
    public NFTServlet(Session session) {
        this.session = session;
    }
    
    public ArrayList<NFT> getNFTs(String address) {
        try {
            return this.session.getBlockFileHandler().getNFTs(address);
        } catch (IOException ex) {
            return new ArrayList<NFT>();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            ArrayList<NFT> ownedNFTs = getNFTs(request.getParameter("address"));
            ArrayList<NFT> listedNFTs = session.getBlockFileHandler().getListedNFTs();
            GsonBuilder gsonBuilder = new GsonBuilder();
            JsonSerializer<NFT> serializer = new JsonSerializer<NFT>() {
                    @Override
                    public JsonElement serialize(NFT nft, Type typeOfSrc, JsonSerializationContext context) {
                        JsonObject json = new JsonObject();
                        json.addProperty("inceptionDate", nft.getInceptionDate());
                        json.addProperty("initiatorAddress", nft.getInitiatorAddress());
                        json.addProperty("title", nft.getTitle());
                        json.addProperty("description", nft.getDescription());
                        json.addProperty("type", nft.getFileType());
                        json.addProperty("hash", nft.getHash());
                        json.addProperty("bytes", nft.getBase64());
                        HashIndex index;
                        try {
                            index = (HashIndex) new FileHandler().readObject(session.getPath() + "/contracts/nfts/" + nft.getHash() + "/hashIndex");
                            if (index == null) return null;
                            String previousHash = index.getHashes().get(index.getHashes().size()-1).hash;
                            json.addProperty("previousHash", previousHash);
                            json.addProperty("currentOwner", session.getBlockFileHandler().getNFTOwner(nft.getHash()));
                        } catch (IOException ex) {
                            Logger.getLogger(NFTServlet.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(NFTServlet.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        return json;
                    }
            };
            
            Type nftArrayListType = new TypeToken<NFT>() {}.getType(); 
            gsonBuilder.registerTypeAdapter(nftArrayListType, serializer);
            Gson customGson = gsonBuilder.create();
            JsonArray ownedArray = new JsonArray();
            for (NFT nft : ownedNFTs) {
                String jsonString =  customGson.toJson(nft, NFT.class);
                JsonElement object = new JsonParser().parse(jsonString);
                ownedArray.add(object);
            }
            JsonArray listedArray = new JsonArray();
            for (NFT nft : listedNFTs) {
                String jsonString =  customGson.toJson(nft, NFT.class);
                JsonElement object = new JsonParser().parse(jsonString);
                listedArray.add(object);
            }
            
            JsonObject object = new JsonObject();
            object.add("ownedNFTs", ownedArray);
            object.add("listedNFTs", listedArray);
            
            response.getWriter().print(object);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().close();
        } catch (IllegalStateException ex) {}
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Part part = request.getPart("myFile");
            
            String jsonString = new String(java.util.Base64.getDecoder().decode(request.getParameter("nft")));

            Gson gson = new Gson();
            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();

            String initiatorAddress = jsonObject.get("initiatorAddress").getAsString();

            String inceptionDate = jsonObject.get("inceptionDate").getAsString();

            String fileType = jsonObject.get("fileType").getAsString();

            String title = jsonObject.get("title").getAsString();

            String description = jsonObject.get("description").getAsString();

            BigInteger key = new BigInteger(jsonObject.get("key").getAsString());

            byte[] data = part.getInputStream().readAllBytes();
            
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


            jsonObject = jsonObject.get("mintFee").getAsJsonObject();
            JsonArray inputs = jsonObject.get("inputs").getAsJsonArray();
            JsonArray outputs = jsonObject.get("outputs").getAsJsonArray();
            Transaction mintFee = new Transaction(jsonObject.get("timestamp").getAsString());
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

                mintFee.addInput(input);
            }

            for (int i = 0; i < outputs.size(); i++) {
                JsonObject object = outputs.get(i).getAsJsonObject();
                String address = object.get("address").getAsString();
                BigDecimal value = object.get("value").getAsBigDecimal();
                TransactionOutput output = new TransactionOutput(address,value);
                mintFee.addOutput(output);
            }
            
            NFT nft = new NFT(inceptionDate, initiatorAddress, mintFee, fileType, title, description, data, contractSignature, key);
            
            String object = DatatypeConverter.printBase64Binary(nft.toBytes());
            javax.json.JsonObject objectData = Json.createObjectBuilder().add("data", object).build();
            session.getPeer().sendMessage(Message.BROADCASTTXN, objectData);
            if (session.getValidators().getValidators(session.getBlockValidator().getStakeRequirement()).size() == 1 && session.getValidation()) {
                boolean result = session.getBlockchain().addData(nft);
                if (result) {session.getBlockchain().addPendingTxn(nft);System.out.println("Transaction Accepted");} 
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
