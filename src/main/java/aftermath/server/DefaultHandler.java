package main.java.aftermath.server;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import main.java.encephalon.profiler.Task;
import main.java.encephalon.writers.HtmlWriter;

public class DefaultHandler extends AbstractHandler {
	private AftermathServer as;
	private volatile Handler[] _handlers;
	String pathInfo;
	String resource;

	public DefaultHandler() {
		this.as = AftermathServer.getInstance();
	}

	public Handler[] getHandlers() {
		return _handlers;
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		Task task = new Task(as.getProfiler(), null, "HttpServer.Handle", null);
		try {
			as.invokeMethod(target, task, baseRequest, request, response);
		} catch (Exception e) {
			task.insertException(e.getCause());
			try {
				returnNotFound(target, task, baseRequest, request, response, e);
			} catch (Exception e2) {
				Timestamp time = new Timestamp(System.currentTimeMillis());
				System.err.println(time.toString().replace(" ", "T") + " [EXCEPTION] -- Second Exception!");
				e2.printStackTrace();
			}
		} finally {
			task.end();
		}

		return;
	}

	public void returnNotFound(String target, Task parent, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response, Exception e) throws Exception {
		Task task = new Task(as.getProfiler(), null, "404 NOT FOUND", baseRequest);
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		baseRequest.setHandled(true);

		HtmlWriter writer = new HtmlWriter(2, as);

		writer.writeln("404 NOT FOUND", 0);
		writer.writeln("The requested URI: " + baseRequest.getRequestURI() + " was not found.", 0);
		writer.writeln("", 0);

		writer.writeln(e.toString() + ":" + e.getMessage(), 0);
		for (StackTraceElement stackElement : e.getStackTrace()) {
			writer.writeln(stackElement.toString(), 0);
		}

		response.getWriter().print(writer.getString("en-us"));
		task.end();
	}
}