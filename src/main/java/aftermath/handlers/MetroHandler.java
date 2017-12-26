package main.java.aftermath.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import main.java.encephalon.annotations.HandlerInfo;
import main.java.encephalon.annotations.methods.GET;
import main.java.encephalon.annotations.methods.QueryParam;
import main.java.encephalon.locale.LocaleBase;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.DefaultHandler;
import main.java.encephalon.server.EncephalonServer;
import main.java.encephalon.writers.HtmlWriter;

public class MetroHandler extends DefaultHandler{
	private HashMap<String, List<HashMap<String, String>>> metroCache; 
	private final HashMap<String, Long> metroCacheTime = new HashMap<String, Long>();
	private final int CACHE_TIME = 300000;
	
	private final EncephalonServer es;
	private final String TempDir = System.getProperty("java.io.tmpdir");

	private final String APIKEY;
		
	public MetroHandler() throws Exception {
		super();
		
		this.es = EncephalonServer.getInstance();
		// TODO Auto-generated constructor stub
		
		String key = "APIKEY NOT LOADED";
		BufferedReader fileBuffer = new BufferedReader(new FileReader(new File(TempDir + "/apikey")));;
		try
		{ 
			 key = fileBuffer.readLine();
			 System.out.println("Tokyo Metro API Key: " + key);
		}
		catch(Exception e)
		{
			Task.entry(es.getProfiler(), null, "Failed to load API Key for Tokyo Metro", null);
		}
		finally
		{
			fileBuffer.close();		
		}
		APIKEY = key;
	}
	
	@GET()
	@HandlerInfo(schema="/(dataType)")
	public void checkMetro(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam("dataType") String dataType) throws Exception
	{
		Long l = metroCacheTime.get(dataType);
		if(l == null || l + CACHE_TIME < System.currentTimeMillis())
		{
			metroCache = new HashMap<String, List<HashMap<String, String>>>();
			Task t = new Task(es.getProfiler(), parent, "[API] METROLOAD: " + dataType, baseRequest);
			URL url = new URL("https://api-tokyochallenge.odpt.org/api/v4/" + dataType + "?acl:consumerKey=" + APIKEY);
			URLConnection urlConnection = url.openConnection();

			StringBuffer sb = new StringBuffer();
			BufferedReader bs;
			try
			{
				bs = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			}
			catch(Exception e)
			{
				throw new Exception(e.getMessage().replace(APIKEY, "###"), e);
			}
			finally
			{
				t.end();
			}
			
			String line;
			while((line = bs.readLine()) != null)
			{
				sb.append(line);
			}
			
			String result = sb.toString();
			
			List<String> list = new ArrayList<String>();
			
			JsonArray jArray = new JsonParser().parse(result).getAsJsonArray();
			Iterator<Entry<String, JsonElement>> iterator = jArray.get(0).getAsJsonObject().entrySet().iterator();
			
			while(iterator.hasNext())
			{
				Entry<String, JsonElement> entry = iterator.next();
				list.add(entry.getKey());
			}
			
			metroCache.put(dataType, new ArrayList<HashMap<String, String>>());
			for(JsonElement jE : jArray)
			{
				HashMap<String, String> newMap = new HashMap<String, String>();
				for(String s : list)
				{
					//  metroCache.put(dataType, new ArrayList<HashMap<String, String>>());
					JsonElement jE2;
					String s2;

					jE2 = jE.getAsJsonObject().get(s);
					if(!jE.isJsonNull() && jE2 != null)
					{
						try
						{
							s2 = jE.getAsJsonObject().get(s).getAsString();
						}
						catch(Exception e)
						{
							s2 = "null";
						}
						
					}
					else
					{
						s2 = "null";
					}
					
					newMap.put(s,  s2);
				}
				
				metroCache.get(dataType).add(newMap);
			}
			
			metroCacheTime.put(dataType, System.currentTimeMillis());
		}
		
		HashMap<String, String> matchValues = new HashMap<String, String>();
		
		if(baseRequest.getQueryString() != null)
		{
			for(String query : baseRequest.getQueryString().split("&"))
			{
				String[] qGrp = query.split("=");
				matchValues.put(qGrp[0], qGrp[1]);
			}			
		}

		HtmlWriter writer = new HtmlWriter(2, es);
		writer.table_Start(null, null, "sortable");
		
		List<String> headingList = new ArrayList<String>();
		Iterator<Entry<String, String>> iterator;
		try
		{
			iterator = metroCache.get(dataType).get(0).entrySet().iterator();
		}
		catch(Exception e)
		{
			metroCacheTime.put(dataType, 0L);
			return;
		}
		
		while(iterator.hasNext())
		{
			Entry<String, String> entry = iterator.next();
			headingList.add(entry.getKey());
		}
			
		if(matchValues.isEmpty())
		{
			writer.tr_Start();
			for(String s : headingList)
			{
				writer.th(s);
			}
			writer.tr_End();
		}
		for(HashMap<String, String> componentMap : metroCache.get(dataType))
		{
			if(matchValues.isEmpty())
			{	
				writer.tr_Start();
				for(String s : headingList)
				{
					writer.td(componentMap.get(s));
				}
				writer.tr_End();
			}
			else
			{
				Iterator<String> keys = matchValues.keySet().iterator();
				while(keys.hasNext())
				{
					String key = keys.next();

					if(componentMap.get(key).equals(matchValues.get(key)))
					{
						for(String s : headingList)
						{
							writer.tr_Start();
							writer.th(s);
							writer.td(componentMap.get(s));
							writer.tr_End();
						}
						writer.tr_Start();
						writer.td("&nbsp;", 2);
						writer.tr_End();
						break;
					}
				}
			}
		}

		writer.table_End();

		response.setContentType("text/html; charset=utf-8");
		response.getWriter().print(writer.getString(locale));
	}
}
