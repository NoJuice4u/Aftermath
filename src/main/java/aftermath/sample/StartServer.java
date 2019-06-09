package main.java.aftermath.sample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import main.java.aftermath.handlers.*;
import main.java.aftermath.server.*;
import main.java.encephalon.API.scope.Scope;
import main.java.encephalon.cluster.ClusteringManager;
import main.java.encephalon.logger.Logger;
import main.java.encephalon.server.DefaultHandler;

public class StartServer extends main.java.encephalon.sample.StartServer
{    
    public static void main(String[] args) throws Exception
    {
        try
        {
            AftermathServer aftermath = AftermathServer.getInstance();
            
            String keyStorePath = aftermath.getProperty("ssl.keyStore", "SSL Keystore file.");
            String keyStorePass = aftermath.getProperty("ssl.keyStore.password", "", "SSL Keystore password.");
            String trustStorePath = aftermath.getProperty("ssl.trustStore", "SSL Truststore file.");
            String trustStorePass = aftermath.getProperty("ssl.trustStore.password", "", "SSL Truststore password.");
            Integer publicPort = aftermath.getProperty("server.port.public", 8083, "Public Port.");
            Integer privatePort = aftermath.getProperty("server.port.private", 8082, "Private Port.");
            Integer maintenancePort = aftermath.getProperty("server.port.maintenance", 8081, "Maintenance Port.");
            Integer publicSSLPort = aftermath.getProperty("server.ssl.port.public", 8443, "SSL Public Port.");
            Integer publicPortQueueSize = aftermath.getProperty("server.port.public.queue.size", 10, "Public port queue size.");
            Integer privatePortQueueSize = aftermath.getProperty("server.port.private.queue.size", 5, "Private port queue size.");
            Integer maintenancePortQueueSize = aftermath.getProperty("server.port.maintenance.queue.size", 2, "Maintenance port queue size.");
            Integer publicSSLPortQueueSize = aftermath.getProperty("server.ssl.port.public.queue.size", 1000, "SSL Public port queue size.");

            aftermath.initializeMap();
            
            ClusteringManager.init(aftermath.getAftermathController().getEdgeData());

            GzipHandler gzipHandler = new GzipHandler();
            gzipHandler.setHandler(new DefaultHandler());
            aftermath.setHandler(gzipHandler);

            registerAftermathHandlers(aftermath);
            registerEncephalonHandlers(aftermath);

            List<Connector> connectors = new ArrayList<Connector>();

            ServerConnector publicConnector = new ServerConnector(aftermath);
            publicConnector.setName(Scope.publicPort.name());
            publicConnector.setPort(publicPort);
            publicConnector.setAcceptQueueSize(publicPortQueueSize);
            aftermath.addConnector(publicConnector);

            ServerConnector privateConnector = new ServerConnector(aftermath);
            privateConnector.setName(Scope.privatePort.name());
            privateConnector.setPort(privatePort);
            privateConnector.setAcceptQueueSize(privatePortQueueSize);
            aftermath.addConnector(privateConnector);

            ServerConnector maintenanceConnector = new ServerConnector(aftermath);
            maintenanceConnector.setName(Scope.maintenancePort.name());
            maintenanceConnector.setPort(maintenancePort);
            maintenanceConnector.setAcceptQueueSize(maintenancePortQueueSize);
            aftermath.addConnector(maintenanceConnector);

            if(keyStorePath != null)
            {
                try
                {
                    Logger.Log("SSL", "Trust Store Path: " + trustStorePath);
                    SslContextFactory sslContext = new SslContextFactory(keyStorePath);
                    sslContext.setKeyStorePassword(keyStorePass);
                    sslContext.setTrustStorePath(trustStorePath);
                    sslContext.setTrustStorePassword(trustStorePass);
    
                    ServerConnector sslConnector = new ServerConnector(aftermath, sslContext);
                    sslConnector.setName(Scope.publicSSLPort.name());
                    sslConnector.setPort(publicSSLPort);
                    sslConnector.setAcceptQueueSize(publicSSLPortQueueSize);
                    aftermath.addConnector(sslConnector);
                }
                catch (Exception e)
                {
                    Logger.Log("SSL-FAIL", "Failed to setup SSL");
                    // Log failure to load keystore
                }
            }
            else
            {
                Logger.Log("SSL-ABSENT", "SSL Not Configured.  Skipping");
            }

            aftermath.start();
            aftermath.join();
        }
        catch (Throwable t)
        {
            HashSet<Integer> exceptionHash = new HashSet<Integer>();
            String tabSt = "  ";

            while (t != null && !exceptionHash.contains(t.hashCode()))
            {
                StringBuilder sb = new StringBuilder();
                StackTraceElement[] elements = t.getStackTrace();
                sb.append("  Message: " + t.getMessage() + "\r\n");

                for (StackTraceElement element : elements)
                {
                    sb.append(tabSt + element.getClassName() + "." + element.getMethodName() + "("
                            + element.getFileName() + ":" + element.getLineNumber() + ")\r\n");
                }

                tabSt += "  ";
                exceptionHash.add(t.hashCode());
                System.err.println(sb.toString());
                t = t.getCause();
            }
        }
    }

    public static void registerAftermathHandlers(AftermathServer server) throws Exception
    {
        server.registerUri("/aftermath", "aftermath", new AftermathHandler());
        // server.registerUri("/", "wcencephalon", new
        // WCEncephalonHandler((AftermathServer)server));
    }
}