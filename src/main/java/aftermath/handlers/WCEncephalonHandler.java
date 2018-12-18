package main.java.aftermath.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import main.java.aftermath.server.AftermathServer;
import main.java.encephalon.annotations.HandlerInfo;
import main.java.encephalon.annotations.methods.GET;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.DefaultHandler;
import main.java.encephalon.writers.HtmlWriter;

public class WCEncephalonHandler extends DefaultHandler
{
    private AftermathServer es;

    public WCEncephalonHandler(AftermathServer instance)
    {
        super();

        this.es = instance;
    }

    @GET
    @HandlerInfo(schema = "/", description = "Details not defined yet because the programmer was lazy.")
    public void getWCServer(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        String destination = baseRequest.getRootURL()
                + "/aftermath/map/coord/\" + position.coords.longitude + \"/\" + position.coords.latitude + \"/canvas\"";

        HtmlWriter writer = new HtmlWriter(2, es);
        writer.script_Start();
        writer.text("if(navigator && navigator.geolocation) {");
        writer.text("navigator.geolocation.getCurrentPosition(showPosition);");
        writer.text("} else {");
        writer.text("document.write(\"Beef\")");
        writer.text("}");
        writer.text("function showPosition(position) { document.write(\"Relocating!\"); window.location = \""
                + destination + "; }");
        writer.script_End();
        response.getWriter().print(writer.getString(locale));
    }
}