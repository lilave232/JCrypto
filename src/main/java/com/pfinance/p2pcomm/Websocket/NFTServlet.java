/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import com.pfinance.p2pcomm.Contracts.NFT;
import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Messaging.Message;
import com.pfinance.p2pcomm.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.Request;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

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
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            request.setAttribute("nfts", session.getWallet().getNFTs());
            request.setAttribute("wallet", session.getWallet());
            request.getRequestDispatcher("/html/nft.jsp").forward(request,response);
        } catch (IllegalStateException ex) {}
    }
    
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
        byte[] file = null;
        if (request.getParameter("pword") != null && request.getParameter("title") != null && request.getParameter("description") != null && request.getParameter("fee") != null) {
            for (Part part : request.getParts()) {
                file = part.getInputStream().readAllBytes();
                if (part.getContentType() != null) {
                    try {
                        session.getWallet().loadKey(request.getParameter("pword"));
                        NFT nft = session.getWallet().createNFT(Float.valueOf(request.getParameter("fee")),part.getContentType(),request.getParameter("title"),request.getParameter("description"),file);
                        String url = "data:" + part.getContentType() + ";base64," + DatatypeConverter.printBase64Binary(file);
                        String object = DatatypeConverter.printBase64Binary(nft.toBytes());
                        JsonObject data = Json.createObjectBuilder().add("data", object).build();
                        session.getPeer().sendMessage(Message.BROADCASTTXN, data);
                    } catch (Exception e) {}
                }
            }
        }
        request.setAttribute("nfts", session.getWallet().getNFTs());
        request.setAttribute("wallet", session.getWallet());
        request.getRequestDispatcher("/html/nft.jsp").forward(request,response);
    }
    
}
