/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import java.io.ByteArrayOutputStream;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author averypozzobon
 */
@ServerEndpoint("/socket")
public class WebSocketEndpoint {
    
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    
    @OnMessage
    public String handleTextMessage(String message) {
        return message;
    }

    @OnMessage
    public byte[] handleBinaryMessage(InputStream is) throws IOException {
        return is.readAllBytes();
    }
    
}
