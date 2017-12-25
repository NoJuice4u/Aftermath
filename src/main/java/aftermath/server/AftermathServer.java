package main.java.aftermath.server;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import com.google.gson.Gson;

import main.java.aftermath.controllers.AftermathController;
import main.java.aftermath.dataCrawlers.OSMReader;
import main.java.encephalon.annotations.*;
import main.java.encephalon.annotations.methods.*;
import main.java.encephalon.locale.*;
import main.java.encephalon.profiler.Profiler;
import main.java.encephalon.profiler.RequestCircularQueue;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.EncephalonServer;
import main.java.encephalon.writers.HtmlWriter;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AftermathServer extends EncephalonServer
{
	public static final AftermathServer aftermathServer = new AftermathServer();
	public static final String GOOGLE_MAP_API_KEY = "AIzaSyBt9HJImP6x4yiqsxpgVIQtDYGXv8WqKWM";
	public static final double GOOGLE_MAP_ZOOMSCALE = 1.0; // 591657550.5;
	private static AftermathController aftermathController = new AftermathController();

	public static final Gson gsonRequest = new Gson();

	public AftermathServer()
	{
		super();
	}

	public static AftermathServer getInstance()
	{
		return aftermathServer;
	}

	public void initializeMap() throws Exception
	{
		new OSMReader(aftermathController.getMapData(), aftermathController.getEdgeData(), aftermathController.getSpatialIndex(),
				aftermathController.getSpatialIndexDepot());
		aftermathController.run();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void registerUri(String uri, String callbackName, Handler handler) throws NoSuchMethodException
	{
		for(Method method : handler.getClass().getMethods())
		{
			HandlerInfo handlerInfo = method.getAnnotation(HandlerInfo.class);
			if(handlerInfo == null) continue;

			String httpMethod = null;
			if(method.getAnnotation(GET.class) != null) httpMethod = method.getAnnotation(GET.class).method();
			else if(method.getAnnotation(PUT.class) != null) httpMethod = method.getAnnotation(PUT.class).method();
			else if(method.getAnnotation(POST.class) != null) httpMethod = method.getAnnotation(POST.class).method();
			else if(method.getAnnotation(DELETE.class) != null) httpMethod = method.getAnnotation(DELETE.class).method();
			else
			{
				throw new NoSuchMethodException("Handler does not have a registered method defined!");
			}

			String baseSchema = handlerInfo.schema();
			String schema = httpMethod + uri + baseSchema;

			// TEST CODE
		    Class<?> params[] = method.getParameterTypes();
			Method m = handler.getClass().getDeclaredMethod(method.getName(), params);
			String mName = handler.getClass().getName() + "." + m.getName();

			String[] schemaArray = schema.split("/");
			uriSegment = uriCollection;
			for(String s : schemaArray)
			{
				String sMasked = s;
				int isWildcard = s.indexOf('(');
				if(isWildcard > -1)
				{
					sMasked = "??";
				}
				uriSubSegment = uriSegment.get(sMasked);
				if(uriSubSegment == null)
				{
					uriSegment.put(sMasked, new HashMap<String, HashMap>());
					if(isWildcard > -1)
					{
						uriSegment.put(ENCEPHALON_PATHPARAMIDENTIFIER, s.substring(1, s.length()-1));
					}
				}
				uriSegment=(HashMap) uriSegment.get(sMasked);
			}

			if(methodList.get(mName) == null)
			{
				methodList.put(mName, m);
				handlerList.put(mName, handler);
			}
			((HashMap<String, String>) uriSegment).put(ENCEPHALON_CALLBACKIDENTIFIER, mName);

			System.out.println("Added Schema: " + httpMethod + " " + baseSchema);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void invokeMethod(String target, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		String uri = baseRequest.getPathInfo();
		String uriMethod = baseRequest.getMethod();
		List<String> paramMap = new ArrayList<String>();
		String requestBody = null;

		if((uriMethod == POST.methodName || uriMethod == PUT.methodName || uriMethod == DELETE.methodName) && baseRequest.getContentType() == "application/json")
		{
			requestBody = baseRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		}

		// TEST
		String schema = uriMethod + uri;
		String[] schemaArray = schema.split("/");
		HashMap<String, HashMap> uriSegment = uriCollection;
		for(String s : schemaArray)
		{
			uriSubSegment = uriSegment.get(s);
			if(uriSubSegment == null)
			{
				uriSubSegment = uriSegment.get(EncephalonServer.ENCEPHALON_URI_WILDCARD);
				paramMap.add(s);
			}
			uriSegment = (HashMap) uriSubSegment;
		}

		Object callBackIdentifier = uriSegment.get(ENCEPHALON_CALLBACKIDENTIFIER);
		String targetFunction = String.valueOf(callBackIdentifier);
		Method m = methodList.get(targetFunction);
		Handler h = handlerList.get(targetFunction);
		HandlerInfo handlerInfo = m.getAnnotation(HandlerInfo.class);

		Annotation[][] parameterAnnotations = m.getParameterAnnotations();
		Class<?> parameterTypes[] = m.getParameterTypes();
	    Object params[] = new Object[parameterAnnotations.length];
	    
	    Task task = new Task(getProfiler(), parent, uriMethod + " " + handlerInfo.schema(), baseRequest);
	    String localeString = getOptionalQueryParamString(request, "locale", "en_us").toLowerCase();
	    LocaleBase locale = localeList.get(localeString);
	    params[0] = target;
	    params[1] = locale;
	    params[2] = task;
	    params[3] = baseRequest;
	    params[4] = request;
	    params[5] = response;
	    // params may be longer than the above.
	    
	    final int stIndex = 6;
	    try
	    {
		    for(int i = stIndex; i < params.length; i++)
		    {
		    	Class<?> clazz = parameterTypes[i];

		    	if(clazz == String.class)
		    	{
		    		params[i] = paramMap.get(i-stIndex);
		    	}
		    	else if(clazz == Integer.class)
		    	{
		    		params[i] = Integer.valueOf(paramMap.get(i-stIndex));
		    	}
		    	else if(clazz == Long.class)
		    	{
		    		params[i] = Long.valueOf(paramMap.get(i-stIndex));
		    	}
		    	else if(clazz == Double.class)
		    	{
		    		params[i] = Double.valueOf(paramMap.get(i-stIndex));
		    	}
		    	else
		    	{
		    		try
		    		{
		    			// Class<? extends Annotation> a = parameterAnnotations[i][0].getClass().asSubclass(RequestBody.class);
		    	    	int pos = parameterTypes.length-1;
		    			params[pos] = gsonRequest.fromJson(requestBody, parameterTypes[pos]);
		    		}
		    		catch(ClassCastException e)
		    		{
		    			throw(e);
		    		}
		    	}
		    }
	    }
	    catch(Exception e)
	    {
	    	response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    	throw e;
	    }
	    response.setContentType("text/html; charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);

		task.start();
		try
		{
			m.invoke(h, params);
		}
		catch (Throwable t) {
			task.insertException(t.getCause());
			returnError(target, locale, task, baseRequest, request, response, m.getDeclaringClass().getName(), t);
		}
		finally
		{
			task.end();
			task.getRequest().finalize((Request)request);
			System.out.println(">>\t" + uriMethod + " " + handlerInfo.schema());
			lastRequestList.insert(task);
		}
	}

	public void returnError(String target, LocaleBase locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response, String declaringClassName, Throwable t) throws IOException
	{
		Task task = new Task(getProfiler(), parent, "500 INTERNAL SERVER ERROR", baseRequest);
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		baseRequest.setHandled(true);

		HtmlWriter writer = new HtmlWriter(2, locale);
		writer.text("500 Internal Server Error - " + declaringClassName + "</br>");
		writer.text("Message: " + t.getMessage() + "</br>");
		StackTraceElement[] steList =  t.getStackTrace();
		for(StackTraceElement ste : steList)
		{
			writer.text(ste.toString() + "</br>");
		}
		
		response.getWriter().print(writer.getString());
		task.end();
	}

	public AftermathController getAftermathController()
	{
		return aftermathController;
	}

	public HashMap<String, Task> getProfiles()
	{
		return profiles.getProfiles();
	}

	public HashMap<String, Method> getMethodList()
	{
		return methodList;
	}

	public HashMap<String, Handler> getHandlerList()
	{
		return handlerList;
	}

	@SuppressWarnings("rawtypes")
	public HashMap<String, HashMap> getUriCollection()
	{
		return uriCollection;
	}

	public Profiler getProfiler()
	{
		return profiles;
	}
	
	public static String getOptionalQueryParamString(HttpServletRequest request, String key, String _default)
	{
		String queryString = request.getQueryString();
		if(queryString != null)
		{
			String[] list = queryString.split("&");
			for(String s : list)
			{
				String[] set = s.split("=");
				if(set[0].equals(key))
				{
					return set[1];
				}
			}
		}
		return _default;
	}
	
	public static boolean getOptionalQueryParamBoolean(HttpServletRequest request, String key, boolean _default)
	{
		String queryString = request.getQueryString();
		if(queryString != null)
		{
			String[] list = queryString.split("&");
			for(String s : list)
			{
				String[] set = s.split("=");
				if(set[0].equals(key))
				{
					return Boolean.valueOf(set[1]);
				}
			}
		}
		return _default;
	}
	
	public static int getOptionalQueryParamInteger(HttpServletRequest request, String key, int _default)
	{
		String queryString = request.getQueryString();
		if(queryString != null)
		{
			String[] list = queryString.split("&");
			for(String s : list)
			{
				String[] set = s.split("=");
				if(set[0].equals(key))
				{
					return Integer.valueOf(set[1]);
				}
			}
		}
		return _default;
	}
}
