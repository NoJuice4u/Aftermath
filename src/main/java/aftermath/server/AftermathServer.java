package main.java.aftermath.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import main.java.aftermath.controllers.AftermathController;
import main.java.aftermath.dataCrawlers.OSMReader;
import main.java.aftermath.locale.*;
import main.java.encephalon.locale.Localizer;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.*;
import main.java.encephalon.utils.ResourceUtils;

public class AftermathServer extends EncephalonServer
{
    public static final AftermathServer as = new AftermathServer();

    public static final String CANVAS_RENDER_JS = ResourceUtils.resourceToString(as, "main/resource/canvasRender.js");

    public static final String GOOGLE_MAP_API_KEY = "AIzaSyBt9HJImP6x4yiqsxpgVIQtDYGXv8WqKWM";
    public static final double GOOGLE_MAP_ZOOMSCALE = 1.0; // 591657550.5;
    private static AftermathController aftermathController = new AftermathController();

    public final Gson gsonRequest = new Gson();

    public Map<String, Localizer<LocaleBase>> localeList = new HashMap<String, Localizer<LocaleBase>>();
    {
        Map<String, Localizer<LocaleBase>> lMap = new HashMap<String, Localizer<LocaleBase>>();
        lMap.put("ja", new Localizer<LocaleBase>(new JP_JP()));
        lMap.put("jp-jp", new Localizer<LocaleBase>(new JP_JP()));
        try
        {
            lMap.put("debug", new Localizer<LocaleBase>(new DEBUG()));
        } catch (IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        lMap.put("en-us", new Localizer<LocaleBase>(new EN_US()));
        lMap.put("undefined", new Localizer<LocaleBase>(new LocaleBase()));
        localeList = Collections.unmodifiableMap(lMap);
    }

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
        new OSMReader(aftermathController.getMapData(), aftermathController.getEdgeData(),
                aftermathController.getSpatialIndex(), aftermathController.getSpatialIndexDepot());
        aftermathController.run();
    }

    public AftermathController getAftermathController()
    {
        return aftermathController;
    }

    public LocaleBase getLocale(String string) throws Exception
    {
        try
        {
            String[] presplit = string.split(",");

            for (int i = 0; i < presplit.length; i++)
            {
                String[] locales = presplit[i].split(";");
                Localizer<LocaleBase> loc = localeList.get(locales[0].toLowerCase());
                if (loc != null)
                {
                    return loc.getLocale();
                }
            }
        } catch (Exception e)
        {

        }
        return localeList.get("undefined").getLocale();
    }

}
