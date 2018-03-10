package main.java.aftermath.sample;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import main.java.aftermath.handlers.*;
import main.java.aftermath.server.*;
import main.java.encephalon.API.scope.Scope;
import main.java.encephalon.server.EncephalonServer;

public class StartServer extends main.java.encephalon.sample.StartServer
{
	public static void main(String[] args) throws Exception
	{
		try
		{
			AftermathServer aftermath = AftermathServer.getInstance();
			aftermath.initializeMap();
			aftermath.setHandler(new DefaultHandler());
			registerAftermathHandlers(aftermath);
			registerEncephalonHandlers(aftermath);
			
			List<Connector> connectors = new ArrayList<Connector>();
			
			ServerConnector publicConnector = new ServerConnector(aftermath);
			publicConnector.setName(Scope.publicPort.name());
			publicConnector.setPort(8080);
			publicConnector.setAcceptQueueSize(50);
			aftermath.addConnector(publicConnector);
			
			ServerConnector privateConnector = new ServerConnector(aftermath);
			privateConnector.setName(Scope.privatePort.name());
			privateConnector.setPort(8081);
			privateConnector.setAcceptQueueSize(50);
			aftermath.addConnector(privateConnector);
			
			ServerConnector debugConnector = new ServerConnector(aftermath);
			debugConnector.setName(Scope.debugPort.name());
			debugConnector.setPort(8082);
			debugConnector.setAcceptQueueSize(20);
			aftermath.addConnector(debugConnector);

			String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
			String keyStorePass = System.getProperty("javax.net.ssl.keyStorePassword");
			String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
			String trustStorePass = System.getProperty("javax.net.ssl.trustStorePassword");
			
			try
			{
				System.out.println("Trust Store Path: " + trustStorePath);
				SslContextFactory sslContext = new SslContextFactory(keyStorePath);
				sslContext.setKeyStorePassword(keyStorePass);
				sslContext.setTrustStorePath(trustStorePath);
				sslContext.setTrustStorePassword(trustStorePass);
				
				ServerConnector sslConnector = new ServerConnector(aftermath, sslContext);
				sslConnector.setName(Scope.publicSSLPort.name());
				sslConnector.setPort(8443);
				sslConnector.setAcceptQueueSize(1000);
				aftermath.addConnector(sslConnector);	
			}
			catch(Exception e)
			{
				System.err.println("Failed to setup SSL");
				// Log failure to load keystore
			}
			
			aftermath.start();
			aftermath.join();
		}
		catch(Throwable t)
		{
			System.err.println(t.getMessage());
			t.printStackTrace();
		}
	}
	
	public static void registerAftermathHandlers(EncephalonServer server) throws Exception
	{
		server.registerUri("/aftermath", "aftermath", new AftermathHandler((AftermathServer)server));
		server.registerUri("/", "wcencephalon", new WCEncephalonHandler((AftermathServer)server));
	}
}