/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import static com.pfinance.p2pcomm.Blockchain.BlockFiles.recursiveListFiles;
import com.pfinance.p2pcomm.Cryptography.Cryptography;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Statistics.TransactionInOut;
import com.pfinance.p2pcomm.Transaction.UTXO;
import com.pfinance.p2pcomm.Wallet.Wallet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;
import org.web3j.crypto.Sign.SignatureData;

/**
 *
 * @author averypozzobon
 */
@SuppressWarnings("serial")
public class IndexServlet extends HttpServlet {
    
    Session session;
    
    public IndexServlet(Session session) {
        this.session = session;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        //response.getWriter().print("{\"wallets\":[");
        //if (session.getWallets().isEmpty()) {
        //} else {
        //    for (Wallet wallet : session.getWallets()) {
        //        try {
        //            response.getWriter().print("{name:" + wallet.getName() + ";balance:" + wallet.getBalance() + "}");
        //        } catch (FileNotFoundException ex) {
        //            Logger.getLogger(IndexServlet.class.getName()).log(Level.SEVERE, null, ex);
        //        } catch (ClassNotFoundException ex) {
        //            Logger.getLogger(IndexServlet.class.getName()).log(Level.SEVERE, null, ex);
        //        }
        //    }
        //}
        //response.getWriter().print("]}");
        //response.setStatus(HttpServletResponse.SC_OK);
        //response.getWriter().close();
        /*
        String content = "Not Instantiated";
        if (this.session.getWallet() == null) {}
        else {
            content = ("Balance: " + String.valueOf(this.session.getWallet().getUsableBalance()));
        }
        request.setAttribute("walletName",content);
        try {
            request.getRequestDispatcher("/html/index.jsp").forward(request,response);
        } catch (IllegalStateException ex) {}
        */
    }
    
    public AbstractMap.SimpleEntry<ArrayList<UTXO>,Float> getUsableBalance(String address) {
        try {
            float usableBalance = 0;
            ArrayList<UTXO> utxos = new ArrayList<UTXO>();
            File f = new File(session.getPath() + "/utxos/");
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile();
                }
            });
            if (files != null) {
                for (File file : files) {
                    UTXO utxo = session.getBlockFileHandler().loadUTXO(file.getPath());
                    if (utxo.getAddress().equals(address)) {
                        if (!session.getBlockchain().getPendingUTXOs().contains(utxo.getPreviousHash() + "|" + utxo.getIndex())) {
                            usableBalance += utxo.toFloat();
                            utxos.add(utxo);
                        }
                    }   
                }
            }
            
            return new AbstractMap.SimpleEntry<>(utxos, usableBalance);
        } catch (Exception e) {
            return new AbstractMap.SimpleEntry<>(new ArrayList<UTXO>(), (float)0);
        }
        
    }
    
    private void receiveMessage(HttpServletRequest request) {
            int recId = Integer.valueOf(request.getParameter("v"));
            int headerByte = recId + 27;
            byte v = (byte) headerByte;
            byte[] r = Numeric.toBytesPadded(new BigInteger(request.getParameter("r")), 32);
            byte[] s = Numeric.toBytesPadded(new BigInteger(request.getParameter("s")), 32);
            byte[] signature = new byte[65];
            System.arraycopy(r, 0, signature, 0, r.length);
            System.arraycopy(s, 0, signature, r.length, s.length);
            signature[64] = v;
            System.out.println(Cryptography.verify(signature, request.getParameter("message1").getBytes(), new BigInteger(request.getParameter("public"))));
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
            AbstractMap.SimpleEntry<ArrayList<UTXO>,Float> usableBalance = getUsableBalance(request.getParameter("address"));
            float balance = usableBalance.getValue();
            ArrayList<UTXO> utxos = usableBalance.getKey();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            String returnValue = "";
            returnValue += "{\"Usable_Balance\":" + balance + ",";
            ArrayList<TransactionInOut> transactions = session.getStats().getWalletInOuts(request.getParameter("address"));
            returnValue += "\"Transactions\":[";
            
            for (int i = 0; i < transactions.size(); i++) {
                returnValue += "{\"date_string\":\"" + transactions.get(i).getDateToString() + "\""
                                            + ",\"type\":" + transactions.get(i).getType()
                                            + ",\"hash\":\"" + transactions.get(i).getHash() + "\""
                                            + ",\"output\":" + transactions.get(i).getOutput()
                                            + ",\"amount\":" + transactions.get(i).getAmount()
                                            + "}";
                if (i < transactions.size()-1) {
                    returnValue += ",";
                }
            }
            returnValue += "],";
            
            returnValue += "\"UTXOS\":[";
            for (int i = 0; i < utxos.size(); i++) {
                returnValue += "{\"address\":\"" + utxos.get(i).getPreviousHash() + "\""
                                            + ",\"index\":" + utxos.get(i).getIndex()
                                            + ",\"amount\":" + utxos.get(i).toFloat()
                                            + "}";
                if (i < utxos.size()-1) {
                    returnValue += ",";
                }
            }
            
            returnValue += "]}";
            response.getWriter().print(returnValue);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().close();
    }
}
