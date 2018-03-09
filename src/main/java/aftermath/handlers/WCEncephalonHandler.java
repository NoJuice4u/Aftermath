package main.java.aftermath.handlers;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import main.java.aftermath.server.AftermathServer;
import main.java.encephalon.annotations.HandlerInfo;
import main.java.encephalon.annotations.methods.GET;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.DefaultHandler;
import main.java.encephalon.writers.HtmlWriter;

public class WCEncephalonHandler extends DefaultHandler{
	private AftermathServer es;

	public WCEncephalonHandler(AftermathServer instance) {
		super();

		this.es = instance;
	}

	@GET
	@HandlerInfo(schema="/")
	public void getWCServer(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{	
		HtmlWriter writer = new HtmlWriter(2, es);

		Iterator<Entry<String, Handler>> iter = es.getHandlerBaseList().entrySet().iterator();
		
		writer.table_Start(null, null, "sortable");
		writer.tHead_Start();
		writer.th("URI");
		writer.th("Handler");
		writer.tHead_End();
		while(iter.hasNext())
		{
			Entry<String, Handler> next = iter.next();
			
			writer.tr_Start();
			writer.td(next.getKey());
			writer.td(next.getValue().toString());
			writer.tr_End();
		}
		writer.table_End();
		
		response.getWriter().print(writer.getString(locale));
	}
}