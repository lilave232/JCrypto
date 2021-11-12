/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Websocket;

import static com.pfinance.p2pcomm.Main.session;
import com.pfinance.p2pcomm.Session;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jasper.servlet.TldScanner;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.servlets.CrossOriginFilter;
/**
 *
 * @author averypozzobon
 */
public class WebServer extends Thread {
    private Server server;
    private Session session;
    private static final String WEBROOT_INDEX = "/webroot/";
    
    public WebServer(Session session) {
        this.session = session;
        /*
        try {
            this.session.setPath("First");
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
    }
    
    public void run() {
        try {
            
            server = new Server();
                // Define ServerConnector
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(8080);
            server.addConnector(connector);
            
            URI baseUri = getWebRootResourceUri();

            // Create Servlet context
            ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servletContextHandler.setContextPath("/");
            //servletContextHandler.setResourceBase(baseUri.toASCIIString());
            servletContextHandler.setResourceBase(System.getProperty("user.dir") + "/src/main/resources/webroot/");

            FilterHolder cors = servletContextHandler.addFilter(CrossOriginFilter.class,"/*",EnumSet.of(DispatcherType.REQUEST));
            cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
            cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
            cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
            cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
                
            // Since this is a ServletContextHandler we must manually configure JSP support.
            this.enableEmbeddedJspSupport(servletContextHandler);
            
            IndexServlet servlet = new IndexServlet(this.session);
            ServletHolder servletHolder = new ServletHolder(servlet);
            servletContextHandler.addServlet(servletHolder, "");
            
            CreateFolderServlet createFolderServlet = new CreateFolderServlet(this.session);
            servletHolder = new ServletHolder(createFolderServlet);
            servletContextHandler.addServlet(servletHolder, "/createFolder");
            
            LoadFolderServlet loadFolderServlet = new LoadFolderServlet(this.session);
            servletHolder = new ServletHolder(loadFolderServlet);
            servletContextHandler.addServlet(servletHolder, "/loadFolder");
            
            MainServlet mainServlet = new MainServlet(this.session);
            servletHolder = new ServletHolder(mainServlet);
            servletContextHandler.addServlet(servletHolder, "/main");
            
            SelectWalletServlet selectServlet = new SelectWalletServlet(this.session);
            servletHolder = new ServletHolder(selectServlet);
            servletContextHandler.addServlet(servletHolder, "/selectWallet");
            
            CreateWalletServlet createWalletServlet = new CreateWalletServlet(this.session);
            servletHolder = new ServletHolder(createWalletServlet);
            servletContextHandler.addServlet(servletHolder, "/createWallet");
            
            WalletCreatedServlet walletCreatedServlet = new WalletCreatedServlet(this.session);
            servletHolder = new ServletHolder(walletCreatedServlet);
            servletContextHandler.addServlet(servletHolder, "/walletCreated");
            
            ImportWalletServlet importWalletServlet = new ImportWalletServlet(this.session);
            servletHolder = new ServletHolder(importWalletServlet);
            servletContextHandler.addServlet(servletHolder, "/importWallet");
            
            LoadWalletServlet loadWalletServlet = new LoadWalletServlet(this.session);
            servletHolder = new ServletHolder(loadWalletServlet);
            servletContextHandler.addServlet(servletHolder, "/loadWallet");
            
            SendTransactionServlet sendTxn = new SendTransactionServlet(this.session);
            servletHolder = new ServletHolder(sendTxn);
            servletContextHandler.addServlet(servletHolder, "/sendTxn");
            
            PeerServlet peerServlet = new PeerServlet(this.session);
            servletHolder = new ServletHolder(peerServlet);
            servletContextHandler.addServlet(servletHolder, "/peer");
            
            NFTServlet nftServlet = new NFTServlet(this.session);
            servletHolder = new ServletHolder(nftServlet);
            servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(System.getProperty("user.dir") + "/tmp"));
            servletContextHandler.addServlet(servletHolder, "/nft");
            
            TransferNFTServlet transferNFTServlet = new TransferNFTServlet(this.session);
            servletHolder = new ServletHolder(transferNFTServlet);
            servletContextHandler.addServlet(servletHolder, "/transferNFT");
            
            // Default Servlet (always last, always named "default")
            ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
            //holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString() + "static/");
            holderDefault.setInitParameter("resourceBase", System.getProperty("user.dir") + "/src/main/resources/webroot/static");
            holderDefault.setInitParameter("dirAllowed", "true");
            servletContextHandler.addServlet(new ServletHolder(new DefaultServlet()), "/static/*");
            server.setHandler(servletContextHandler);
            
            //ServletMapping[] mapping = servletContextHandler.getServletHandler().getServletMappings();
            
            //for (int i = 0; i < mapping.length; i++) {
            //    System.out.println(mapping[i].getServletName() + ":" + String.join(",", mapping[i].getPathSpecs()));
            //}

            // Start Server
            // server.setDumpAfterStart(true);
            server.start();
            //if(Desktop.isDesktopSupported()) {
                //String s = "http://localhost:8080";
                //Desktop desktop = Desktop.getDesktop();
                //desktop.browse(URI.create(s));
            //}
            
        
        } catch (Exception ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException
    {
        // Establish Scratch directory for the servlet context (used by JSP compilation)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

        if (!scratchDir.exists())
        {
            if (!scratchDir.mkdirs())
            {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);

        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        servletContextHandler.setClassLoader(jspClassLoader);

        // Manually call JettyJasperInitializer on context startup
        servletContextHandler.addBean(new EmbeddedJspStarter(servletContextHandler));

        // Create / Register JSP Servlet (must be named "jsp" per spec)
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("scratchdir", scratchDir.toString());
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");
        holderJsp.setInitParameter("keepgenerated", "true");
        servletContextHandler.addServlet(holderJsp, "*.jsp");

        servletContextHandler.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
    }
    
    private URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException
    {
        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
        if (indexUri == null)
        {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        // Points to wherever /webroot/ (the resource) is
        return indexUri.toURI();
    }
}
