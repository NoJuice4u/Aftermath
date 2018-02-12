package main.java.aftermath.server;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import main.java.aftermath.controllers.AftermathController;
import main.java.aftermath.dataCrawlers.OSMReader;
import main.java.aftermath.locale.*;
import main.java.encephalon.dto.MapVertex;
import main.java.encephalon.locale.Localizer;
import main.java.encephalon.server.*;

public class AftermathServer extends EncephalonServer
{
	public static final AftermathServer as = new AftermathServer();
	public static final String GOOGLE_MAP_API_KEY = "AIzaSyBt9HJImP6x4yiqsxpgVIQtDYGXv8WqKWM";
	public static final double GOOGLE_MAP_ZOOMSCALE = 1.0; // 591657550.5;
	private static AftermathController aftermathController = new AftermathController();
	
	public Map<String, Localizer<LocaleBase>> localeList = new HashMap<String, Localizer<LocaleBase>>();
	{
		Map<String, Localizer<LocaleBase>> lMap = new HashMap<String, Localizer<LocaleBase>>();
		lMap.put("ja", new Localizer<LocaleBase>(new JP_JP()));
		lMap.put("en-us", new Localizer<LocaleBase>(new EN_US()));
		lMap.put("undefined", new Localizer<LocaleBase>(new LocaleBase()));
		localeList = Collections.unmodifiableMap(lMap);
	}

	public final Gson gsonRequest = new Gson();

	protected AftermathServer()
	{
		super();
		EncephalonServer.setInstance(this);
	}

	public static AftermathServer getInstance()
	{
		return as;
	}

	public void initializeMap() throws Exception
	{
		new OSMReader(aftermathController.getMapData(), aftermathController.getEdgeData(), aftermathController.getSpatialIndex(),
				aftermathController.getSpatialIndexDepot());
		aftermathController.run();
	}

	public AftermathController getAftermathController()
	{
		return aftermathController;
	}

	public LocaleBase getLocale(String string) throws Exception {
		String[] presplit = string.split(",");
		
		for(int i = 0; i < presplit.length; i++)
		{
			String[] locales = presplit[i].split(";");
			Localizer<LocaleBase> loc = localeList.get(locales[0].toLowerCase());
			if(loc != null)
			{
				return loc.getLocale();
			}
		}
		return localeList.get("undefined").getLocale();
	}

}
