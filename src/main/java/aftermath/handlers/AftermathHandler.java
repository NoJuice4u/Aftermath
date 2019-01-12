package main.java.aftermath.handlers;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import main.java.aftermath.dto.MapResponseDto;
import main.java.aftermath.engine.Depot;
import main.java.aftermath.locale.LocaleBase;
import main.java.aftermath.server.AftermathServer;
import main.java.aftermath.vehicles.Transport;
import main.java.aftermath.writers.HtmlWriter;

import main.java.encephalon.annotations.HandlerInfo;
import main.java.encephalon.annotations.methods.GET;
import main.java.encephalon.annotations.methods.MenuItem;
import main.java.encephalon.annotations.methods.POST;
import main.java.encephalon.annotations.methods.QueryParam;
import main.java.encephalon.annotations.methods.QueryString;
import main.java.encephalon.cluster.ClusteringManager;
import main.java.encephalon.dto.CoordinateRange;
import main.java.encephalon.dto.Coordinates;
import main.java.encephalon.dto.DistinctOrderedSet;
import main.java.encephalon.dto.DistinctOrderedSet.OrderType;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.dto.MapEdge.RoadTypes;
import main.java.encephalon.dto.MapVertex;
import main.java.encephalon.dto.MapVertexLite;
import main.java.encephalon.exceptions.ResponseException;
import main.java.encephalon.histogram.HistogramBase;
import main.java.encephalon.histogram.LowResolutionHistogram;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.DefaultHandler;
import main.java.encephalon.spatialIndex.SpatialIndex;
import main.java.encephalon.writers.JsonWriter;

public class AftermathHandler extends DefaultHandler
{
    private AftermathServer es;
    private long maxScore = 1;

    public AftermathHandler()
    {
        super();

        this.es = AftermathServer.getInstance();
    }

    @GET
    @MenuItem(name = "Map/Item/Base")
    @HandlerInfo(schema = "/", description = "Details not defined yet because the programmer was lazy.")
    public void getMap(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        String destination = baseRequest.getRootURL()
                + "/aftermath/map/coord/\" + position.coords.longitude + \"/\" + position.coords.latitude + \"/canvas\"";

        HtmlWriter writer = es.getWriter();
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

    @GET
    @MenuItem(name = "Map/Item/Node List")
    @HandlerInfo(schema = "/map", description = "Details not defined yet because the programmer was lazy.")
    public void getMapData(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        LocaleBase localizer = es.getLocale(locale);
        HashMap<Long, MapVertex> mapData = es.getAftermathController().getMapData();
        HashMap<Long, MapEdge> mapEdges = es.getAftermathController().getEdgeData();
        Iterator<Entry<Long, MapVertex>> iter = mapData.entrySet().iterator();

        HtmlWriter writer = es.getWriter();

        writer.table_Start(null, null, "sortable");
        writer.tr_Start();
        writer.td_Start();
        writer.h1(localizer.INTRODUCTION_TITLE);
        writer.text(localizer.INTRODUCTION_MESSAGE);
        writer.td_End();
        writer.tr_End();
        writer.tr_Start();
        writer.th(localizer.TH_NODE);
        writer.tr_End();
        int i = 0;
        while (iter.hasNext())
        {
            Map.Entry<Long, MapVertex> kvPair = (Map.Entry<Long, MapVertex>) iter.next();
            String key = String.valueOf(kvPair.getKey());
            MapVertex node = kvPair.getValue();
            List<Long> connections = node.getEdges();
            for (Long l : connections)
            {
                if (mapEdges.get(l).getMode() == RoadTypes.primary || mapEdges.get(l).getMode() == RoadTypes.secondary)
                {
                    writer.tr_Start();
                    writer.td(
                            "<A href=\"/aftermath/map/node/" + key + "\">" + key + "</A>: " + mapEdges.get(l).getMode()
                                    + " :: " + node.toString() + " - " + Arrays.toString(connections.toArray()));
                    writer.tr_End();
                    i++;
                    break;
                }
            }
            if (i > 1000)
                break;
        }
        writer.table_End();

        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @MenuItem(name = "Map/Json/Base")
    @HandlerInfo(schema = "/map/json", description = "Details not defined yet because the programmer was lazy.")
    public void getMapDataJson(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        HashMap<Long, MapVertex> mapData = es.getAftermathController().getMapData();

        JsonWriter jw = new JsonWriter(mapData);

        response.getWriter().print(jw.toString());
    }

    @GET
    @HandlerInfo(schema = "/map/vehicle/(vehicleId)/json", description = "Details not defined yet because the programmer was lazy.")
    public void getVehicleInfo(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response,
            @QueryParam(value = "vehicleId") Integer vehicleId) throws Exception
    {
        Transport vehicle = es.getAftermathController().getTransporters().get(vehicleId);

        JsonWriter jw = new JsonWriter(vehicle);

        response.setContentType("application/json");
        response.getWriter().print(jw.toString());
    }

    @GET
    @HandlerInfo(schema = "/delay", description = "Details not defined yet because the programmer was lazy.")
    public void getDelay(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Thread.sleep(5000);
        response.getWriter().print("Delayed");
    }
    
    @GET
    @HandlerInfo(schema = "/map/node/(uid)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapNode(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response, @QueryParam(value = "uid") Long uid,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "roadType", _default = "") String filter,
            @QueryString(value = "transports", _default = "false") Boolean drawTransports,
            @QueryString(value = "nodeVertices", _default = "false") Boolean drawVertices,
            @QueryString(value = "drawSpatialGrid", _default = "false") Boolean drawSpatialGrid,
            @QueryString(value = "drawGroups", _default = "false") Boolean drawGroups,
            @QueryString(value = "authorative", _default = "false") Boolean authorative) throws Exception
    {
        getMapNodeWithDepthAndZoom(target, locale, parent, baseRequest, request, response, uid, depth, zoom, filter,
                drawVertices, drawTransports, drawSpatialGrid, drawGroups, authorative);
    }

    private long findNearestMajorRoad(Double longitude, Double latitude, int diveOutDepth) throws Exception
    {
        Coordinates coords = new Coordinates(longitude, latitude);

        SpatialIndex<Coordinates> index = es.getAftermathController().getSpatialIndex()
                .getNearestNodeRegionIndex(coords);
        for (int i = 0; i < diveOutDepth; i++)
        {
            index = index.getParent();
        }

        List<Long> nodeIds = index.getVerticesWithinBounds();

        if (nodeIds.size() > 0)
        {
            MapVertex nearestNode = null;
            for (Long nId : nodeIds)
            {
                MapVertex vtx = es.getAftermathController().getMapData().get(nId);
                for (Long eId : vtx.getEdges())
                {
                    MapEdge edge = es.getAftermathController().getEdgeData().get(eId);
                    switch (String.valueOf(edge.getMode()))
                    {
                    case "secondary":
                    case "primary":
                    case "residential":
                        if (nearestNode == null || nearestNode.getDistance(coords) > vtx.getDistance(coords))
                        {
                            nearestNode = vtx;
                        }
                    default:
                        break;
                    }
                }
            }
            if (nearestNode != null)
            {
                return nearestNode.getId();
            }
        }

        return es.getAftermathController().getSpatialIndex().getNearestNode(coords);
    }

    @GET
    @HandlerInfo(schema = "/map/coord/(longitude)/(latitude)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapNodeWithCoordinates(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "longitude") Double longitude,
            @QueryParam(value = "latitude") Double latitude, @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "roadType", _default = "") String filter,
            @QueryString(value = "transports", _default = "false") Boolean drawTransports,
            @QueryString(value = "nodeVertices", _default = "false") Boolean drawVertices,
            @QueryString(value = "drawSpatialGrid", _default = "false") Boolean drawSpatialGrid,
            @QueryString(value = "drawGroups", _default = "false") Boolean drawGroups,
            @QueryString(value = "diveOutDepth", _default = "5") Integer diveOutDepth,
            @QueryString(value = "authorative", _default = "false") Boolean authorative) throws Exception
    {
        Long nodeId = findNearestMajorRoad(longitude, latitude, diveOutDepth);
        getMapNodeWithDepthAndZoom(target, locale, parent, baseRequest, request, response, nodeId, depth, zoom, filter,
                drawVertices, drawTransports, drawSpatialGrid, drawGroups, authorative);
    }

    @GET
    @HandlerInfo(schema = "/map/coord/(longitude)/(latitude)/canvas", description = "Details not defined yet because the programmer was lazy.")
    public void getMapNodeWithCoordinatesCanvas(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "longitude") Double longitude,
            @QueryParam(value = "latitude") Double latitude, @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "diveOutDepth", _default = "5") Integer diveOutDepth,
            @QueryString(value = "authorative", _default = "false") Boolean authorative,
            @QueryString(value = "roadType", _default = "") String roadType) throws Exception
    {
        Long nodeId = findNearestMajorRoad(longitude, latitude, diveOutDepth);
        getTestCanvas(target, locale, parent, baseRequest, request, response, nodeId, zoom, depth, authorative, roadType);
    }

    @GET
    @HandlerInfo(schema = "/map/vehicle/(vehicleId)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapNodeByVehicle(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response,
            @QueryParam(value = "vehicleId") Integer vehicleId,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "roadType", _default = "") String filter,
            @QueryString(value = "drawMap", _default = "true") Boolean drawMap,
            @QueryString(value = "drawSpatialGrid", _default = "false") Boolean drawSpatialGrid,
            @QueryString(value = "drawGroups", _default = "false") Boolean drawGroups,
            @QueryString(value = "authorative", _default = "false") Boolean authorative) throws Exception
    {
        List<Transport> transporters = es.getAftermathController().getTransporters();
        Transport transport = transporters.get(vehicleId);

        MapVertex initialNode = transport.getNode();
        HtmlWriter writer = es.getWriter();

        if (drawMap == true)
            renderMap(writer, initialNode, zoom);
        writer.canvasIcons();
        writer.canvas("mapCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2);
        writer.script_Start();
        writer.initializeCanvasJS("mapCanvas");

        int zm = (int) (AftermathServer.GOOGLE_MAP_ZOOMSCALE * Math.pow(2, zoom));
        if (drawSpatialGrid)
            drawSpatialIndex(writer, initialNode, zm);
        drawRoads(writer, initialNode, zm, depth, filter, authorative);
        drawDepot(writer, initialNode, zm);
        drawTransport(writer, transport, initialNode, zm, depth);
        if (drawGroups)
            drawGroups(writer, initialNode, zm);

        writer.addImageData();
        writer.script_End();
        writer.table_Start();

        writeSummaryNode(writer, locale, initialNode, zoom, depth, authorative);
        writeSummaryNeighboringNodes(writer, locale, initialNode, zoom, depth);
        writeSummaryVehicles(writer, locale, zoom, depth);
        writer.table_End();

        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @HandlerInfo(schema = "/map/vehicle/(vehicleId)/canvas", description = "Details not defined yet because the programmer was lazy.")
    public void getMapCanvasNodeByVehicle(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response,
            @QueryParam(value = "vehicleId") Integer vehicleId,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "authorative", _default = "false") Boolean authorative,
            @QueryString(value = "roadType", _default = "all") String roadType) throws Exception
    {
        Transport t = es.getAftermathController().getTransporters().get(vehicleId);
        MapVertex initialNode = t.getNode();

        getTestCanvas(target, locale, parent, baseRequest, request, response, initialNode.getId(), zoom, depth,
                authorative, roadType);
    }

    @GET
    @HandlerInfo(schema = "/map/add/depot/name/(name)/edge/(edgeId)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapAddDepotEdge(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "name") String name,
            @QueryParam(value = "edgeId") Long edgeId) throws Exception
    {
        HtmlWriter writer = es.getWriter();
        MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edgeId);
        Depot d = new Depot(name, 100, mapEdge);

        es.getAftermathController().getDepotData().put(d.getId(), d);
        es.getAftermathController().getSpatialIndexDepot().add(d.getId(), d);
        writer.text(String.valueOf(d.getId()));
        response.getWriter().print(writer.getString(locale));
    }
    
    @GET
    @HandlerInfo(schema = "/map/depot/(depotId)/activate", description = "Details not defined yet because the programmer was lazy.")
    public void getDepotActivate(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "depotId") Long depotId) throws Exception
    {
        HtmlWriter writer = es.getWriter();
        Depot d = es.getAftermathController().getDepotData().get(depotId);
        d.activate();

        JsonWriter jw = new JsonWriter(d);
        
        String s = jw.toString();
        response.setContentType("application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    @GET
    @HandlerInfo(schema = "/map/depot/(depotId)/deactivate", description = "Details not defined yet because the programmer was lazy.")
    public void getDepotDeactivate(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "depotId") Long depotId) throws Exception
    {
        HtmlWriter writer = es.getWriter();
        Depot d = es.getAftermathController().getDepotData().get(depotId);
        d.deactivate();

        JsonWriter jw = new JsonWriter(d);
        
        String s = jw.toString();
        response.setContentType("application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    @GET
    @HandlerInfo(schema = "/map/remove/depot/(depotId)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapRemoveDepotEdge(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "depotId") Long depotId)
            throws Exception
    {
        HtmlWriter writer = es.getWriter();

        Depot d = es.getAftermathController().getDepotData().remove(depotId);

        if (d != null)
        {
            writer.text(String.valueOf(d.getId()));
        }
        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @HandlerInfo(schema = "/map/rename/depot/(depotId)/name/(name)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapRenameDepotEdge(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "depotId") Long depotId,
            @QueryParam(value = "name") String name) throws Exception
    {
        HtmlWriter writer = es.getWriter();

        Depot d = es.getAftermathController().getDepotData().get(depotId);
        d.setName(name);

        if (d != null)
        {
            writer.text(String.valueOf(d.getId()) + " -&gt; " + name);
        }
        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @HandlerInfo(schema = "/map/depot/(depotId)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapDepot(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response, @QueryParam(value = "depotId") Long depotId,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "roadType", _default = "") String filter,
            @QueryString(value = "transports", _default = "false") Boolean drawTransports,
            @QueryString(value = "nodeVertices", _default = "false") Boolean drawVertices,
            @QueryString(value = "diveOutDepth", _default = "5") Integer diveOutDepth,
            @QueryString(value = "drawSpatialGrid", _default = "false") Boolean drawSpatialGrid,
            @QueryString(value = "drawGroups", _default = "false") Boolean drawGroups,
            @QueryString(value = "authorative", _default = "false") Boolean authorative) throws Exception
    {
        HtmlWriter writer = es.getWriter();

        Depot depot = es.getAftermathController().getDepotData().get(depotId);
        Long nodeId = findNearestMajorRoad(depot.getLongitude(), depot.getLatitude(), diveOutDepth);

        getMapNodeWithDepthAndZoom(target, locale, parent, baseRequest, request, response, nodeId, depth, zoom, filter,
                drawVertices, drawTransports, drawSpatialGrid, drawGroups, authorative);

        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @MenuItem(name = "Map/Depot List")
    @HandlerInfo(schema = "/map/depots", description = "Details not defined yet because the programmer was lazy.")
    public void getMapDepotList(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        HtmlWriter writer = es.getWriter();

        Iterator<Entry<Long, Depot>> iter = es.getAftermathController().getDepotData().entrySet().iterator();
        writer.table_Start();
        writer.tHead_Start();
        writer.tr_Start();
        writer.th("ID");
        writer.th("Link");
        writer.th("Active?");
        writer.tr_End();
        writer.tHead_End();
        while (iter.hasNext())
        {
            Entry<Long, Depot> entry = iter.next();
            writer.tr_Start();
            writer.td("<A href=\"/aftermath/map/depot/" + entry.getKey().toString() + "\">" + entry.getKey().toString()
                    + "</A>");
            writer.td("<A href=\"/aftermath/map/coord/" + String.valueOf(entry.getValue().getLongitude()) + "/"
                    + String.valueOf(entry.getValue().getLatitude()) + "/canvas\">" + entry.getValue().getName()
                    + "</A>");
            writer.td(String.valueOf(entry.getValue().isActive()));
            writer.tr_End();
        }
        writer.table_End();
        response.getWriter().print(writer.getString(locale));
    }
    
    @GET
    @MenuItem(name = "Map/Depot List/Json")
    @HandlerInfo(schema = "/map/depots/json", description = "Details not defined yet because the programmer was lazy.")
    public void getMapDepotListJson(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        HashMap<Long, Depot> depotData = es.getAftermathController().getDepotData();
        
        JsonWriter jw = new JsonWriter(depotData);

        String s = jw.toString();
        response.setContentType("application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    public void getMapNodeWithDepthAndZoom(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, Long uid, int depth, int zoom, String filter,
            Boolean drawVertices, Boolean drawTransports, Boolean drawSpatialGrid, Boolean drawGroups,
            Boolean authorative) throws Exception
    {
        MapVertex initialNode = es.getAftermathController().getMapData().get(uid);
        if (initialNode == null)
        {
            throw new ResponseException(404, "Node ID: [" + uid + "] not found!");
        }
        HtmlWriter writer = es.getWriter();

        renderMap(writer, initialNode, zoom);

        writer.canvasIcons();
        writer.canvas("mapCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2);
        writer.script_Start();
        writer.initializeCanvasJS("mapCanvas");

        int zm = (int) (AftermathServer.GOOGLE_MAP_ZOOMSCALE * Math.pow(2, zoom));
        if (drawSpatialGrid)
            drawSpatialIndex(writer, initialNode, zm);
        drawRoads(writer, initialNode, zm, depth, filter, authorative);
        if (drawVertices)
            drawVertices(writer, initialNode, zm, depth, filter);
        drawDepot(writer, initialNode, zm);
        if (drawTransports)
            drawTransports(writer, initialNode, zm, depth);
        if (drawGroups)
            drawGroups(writer, initialNode, zm);
        drawDepot(writer, initialNode, zm);
        
        writer.addImageData();
        writer.script_End();
        writer.table_Start();

        writeSummaryNode(writer, locale, initialNode, zoom, depth, authorative);
        writeSummaryNeighboringNodes(writer, locale, initialNode, zoom, depth);
        writeSummaryVehicles(writer, locale, zoom, depth);
        writer.table_End();

        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @HandlerInfo(schema = "/map/node/(uid)/canvas", description = "Details not defined yet because the programmer was lazy.")
    public void getTestCanvas(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "uid") Long uid,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "authorative", _default = "false") Boolean authorative,
            @QueryString(value = "roadType", _default = "") String roadType) throws Exception
    {
        MapVertex initialNode = es.getAftermathController().getMapData().get(uid);
        if (initialNode == null)
        {
            throw new ResponseException(404, "Node ID: [" + uid + "] not found!");
        }

        HtmlWriter writer = es.getWriter();
        writer.importScript(es.CANVAS_RENDER_JS + "function refresh()" + "{loadJSON(" + "\""
                + baseRequest.getRootURL() + "/aftermath/map/node/" + uid + "/json?depth=" + depth + "&zoom=" + zoom + "&roadType=" + roadType
                + "\"," + zoom + ");}refresh();");

        renderMap(writer, initialNode, zoom);
        writer.table_Start(null, null, "tableContainer", 100, null);
        writer.tr_Start();
        writer.td_Start("canvasContainer");
        writer.table_Start("Map", null, "GlobalMap", MapVertex.WIDTH, MapVertex.HEIGHT, "hidden");
        writer.tr_Start();
        writer.td_Start(1, MapVertex.WIDTH, MapVertex.HEIGHT);
        writer.div_Start("overflow:hidden; height:" + MapVertex.HEIGHT + "px");
        writer.canvasIcons();
        writer.canvas("mapCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2, 0, 25);
        // This one is offset too low. Overlaying issues.
        writer.canvas("tempLineCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2, -MapVertex.HEIGHT, 30, "none");
        writer.div_End();
        String submitDataForm = "submitDataForm('" + baseRequest.getRootURL() + "/aftermath/map/weight');";
        writer.canvasInputDiv("canvasInputBox", "#FF0000", submitDataForm);
        writer.td_End();
        writer.tr_End();
        writer.table_End();
        writer.table_Start();
        writer.tr_Start();
        writer.td_Start();
        writeSummaryNode(writer, locale, initialNode, zoom, depth, authorative);
        writeSummaryNeighboringNodes(writer, locale, initialNode, zoom, depth);
        writeSummaryVehicles(writer, locale, zoom, depth);
        writer.td_End();
        writer.tr_End();
        writer.table_End();
        writer.td_End();
        writer.tr_End();
        writer.table_End();

        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @HandlerInfo(schema = "/map/start/(sLat)/(sLon)/end/(eLat)/(eLon)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapNodesWithinBounds(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "sLat") Long sLat,
            @QueryParam(value = "sLon") Long sLon, @QueryParam(value = "eLat") Long eLat,
            @QueryParam(value = "eLon") Long eLon) throws Exception
    {
        Coordinates[] coordsRange =
        { new Coordinates(sLon, sLat), new Coordinates(eLon, eLat) };
        List<Long> list = es.getAftermathController().getSpatialIndex().getVerticesWithinBounds(coordsRange);

        // TODO: FILL IN FOR EDGES

        HashSet<Long> edgeList = new HashSet<Long>();

        for (Long vertex : list)
        {
            edgeList.addAll(es.getAftermathController().getMapData().get(vertex).getEdges());
        }

        List<MapEdge> edgeData = new ArrayList<MapEdge>();
        for (Long edge : edgeList)
        {
            edgeData.add(es.getAftermathController().getEdgeData().get(edge));
        }

        JsonWriter jw = new JsonWriter(edgeData);

        String s = jw.toString();
        response.setContentType("application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    @GET
    @HandlerInfo(schema = "/map/node/(uid)/vehicles/json", description = "Details not defined yet because the programmer was lazy.")
    public void getMapVehiclesJson(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "uid") Long uid,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth) throws Exception
    {
        MapVertex initialNode = es.getAftermathController().getMapData().get(uid);

        List<Transport> transports = es.getAftermathController().getTransporters();
        HashMap<Long, MapVertexLite> shortTransportList = new HashMap<Long, MapVertexLite>();

        for (Transport t : transports)
        {
            double[] bearing = initialNode.getBearing(t.getNode(), zoom);

            // Sort/filter Vehicles based on location!
            MapVertex vertex = new MapVertex((float) t.getNode().getLongitude(), (float) t.getNode().getLatitude(),
                    t.getId());
            shortTransportList.put(t.getId(), vertex);
        }

        JsonWriter jw = new JsonWriter(shortTransportList);

        String s = jw.toString();
        response.setContentType("application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    @GET
    @HandlerInfo(schema = "/map/vehicles", description = "Details not defined yet because the programmer was lazy.")
    public void getMapVehicles(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {

        HtmlWriter writer = es.getWriter();
        writeSummaryVehicles(writer, locale, 16, 5);

        String s = writer.getString(locale);
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    @GET
    @HandlerInfo(schema = "/map/node/(uid)/clear", description = "Details not defined yet because the programmer was lazy.")
    public void getMapNodeClearWeights(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "uid") Long uid,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "roadType", _default = "") String filter) throws Exception
    {
        HashSet<String> filterSet = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(filter, ",");
        while (st.hasMoreTokens())
        {
            filterSet.add(st.nextToken());
        }

        MapVertex initialNode = es.getAftermathController().getMapData().get(uid);
        HashMap<Long, Integer> masterDepthList = new HashMap<Long, Integer>(200);

        List<Long> edges = initialNode.getEdges();

        MapVertex focalPoint = es.getAftermathController().getMapData().get(uid);
        DistinctOrderedSet masterEdgeList = buildMasterEdgeListFromDepth(focalPoint, masterDepthList, edges, depth,
                zoom);

        masterEdgeList.reset(OrderType.FIFO);

        while (masterEdgeList.hasNext())
        {
            Long edge = masterEdgeList.next();
            MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edge);

            mapEdge.resetWeights();
        }
    }

    @GET
    @HandlerInfo(schema = "/map/node/(uid)/json", description = "Details not defined yet because the programmer was lazy.")
    public void getMapNodeJson(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "uid") Long uid,
            @QueryString(value = "zoom", _default = "18", min = 1, max = 20) Integer zoom,
            @QueryString(value = "depth", _default = "6") Integer depth,
            @QueryString(value = "roadType", _default = "") String filter) throws Exception
    {
        HashSet<String> filterSet = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(filter, ",");
        while (st.hasMoreTokens())
        {
            filterSet.add(st.nextToken());
        }

        MapVertex initialNode = es.getAftermathController().getMapData().get(uid);
        HashMap<Long, Integer> masterDepthList = new HashMap<Long, Integer>(200);

        List<Long> edges = initialNode.getEdges();

        MapVertex focalPoint = es.getAftermathController().getMapData().get(uid);
        DistinctOrderedSet masterEdgeList = buildMasterEdgeListFromDepth(focalPoint, masterDepthList, edges, depth,
                zoom);

        HashMap<Long, MapEdge> edgeData = new HashMap<Long, MapEdge>();
        masterEdgeList.reset(OrderType.FIFO);
        DistinctOrderedSet masterVertexList = new DistinctOrderedSet(200);

        while (masterEdgeList.hasNext())
        {
            Long edge = masterEdgeList.next();
            MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edge);
            mapEdge.update();

            if (filterSet.size() == 0 || filterSet.contains(String.valueOf(mapEdge.getMode().name())))
            {
            } else
            {
                continue;
            }

            edgeData.put(edge, mapEdge);

            masterVertexList.add(mapEdge.getVertices()[0]);
            masterVertexList.add(mapEdge.getVertices()[1]);
        }

        HashMap<Long, MapVertexLite> vertexData = new HashMap<Long, MapVertexLite>();

        masterVertexList.reset(OrderType.FIFO);
        while (masterVertexList.hasNext())
        {
            Long vertexId = masterVertexList.next();
            vertexData.put(vertexId, es.getAftermathController().getMapData().get(vertexId));
        }
        
        
        HashMap<Long, Depot> depotData = new HashMap<Long, Depot>();
        List<Long> depotIds = es.getAftermathController().getSpatialIndexDepot().getNearestNodeRegion(focalPoint);

        for (Long depotId : depotIds)
        {
            Depot depot = es.getAftermathController().getDepotData().get(depotId);
            depotData.put(depotId, depot);
        }
        
        MapVertex focusVertex = es.getAftermathController().getMapData().get(uid);
        MapResponseDto responseDto = new MapResponseDto(focusVertex.getLongitude(), focusVertex.getLatitude(), zoom,
                vertexData, edgeData, depotData, ClusteringManager.getRoots());

        JsonWriter jw = new JsonWriter(responseDto);

        String s = jw.toString();
        response.setContentType("application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    @GET
    @MenuItem(name = "Map/Json/Vehicles")
    @HandlerInfo(schema = "/map/vehicles/json", description = "Details not defined yet because the programmer was lazy.")
    public void getVehiclesJson(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        List<Transport> transports = es.getAftermathController().getTransporters();

        HashMap<Long, MapVertexLite> shortTransportList = new HashMap<Long, MapVertexLite>();
        for (Transport t : transports)
        {
            MapVertex vertex = new MapVertex((float) t.getNode().getLongitude(), (float) t.getNode().getLatitude(),
                    t.getId());
            shortTransportList.put(t.getId(), vertex);
        }

        JsonWriter jw = new JsonWriter(shortTransportList);

        String s = jw.toString();
        response.setContentType("application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        response.getWriter().print(s);
    }

    @GET
    @HandlerInfo(schema = "/map/vehicle/(vehicleId)/capacity/(capacity)", description = "Details not defined yet because the programmer was lazy.")
    public void getVehicleSetWeight(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response,
            @QueryParam(value = "vehicleId") Integer vehicleId, @QueryParam(value = "capacity") Integer capacity)
            throws Exception
    {
        es.getAftermathController().getTransporters().get(vehicleId).setWeightChange(capacity);
        ;
    }

    @GET
    @HandlerInfo(schema = "/map/edge/(edgeId)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapEdgeInfo(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "edgeId") Long edgeId)
            throws Exception
    {
        HistogramBase weights = es.getAftermathController().getEdgeData().get(edgeId).getWeightInputs();
        JsonWriter jw = new JsonWriter(weights);

        response.setContentType("application/json");
        response.getWriter().print(jw.toString());
    }

    @GET
    @MenuItem(name = "Debug/Map/Roots/Json")
    @HandlerInfo(schema = "/map/roots/json", description = "Details not defined yet because the programmer was lazy.")
    public void getMapRootsJson(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        HashMap<Long, CoordinateRange> rootSet = ClusteringManager.getRoots();

        JsonWriter jw = new JsonWriter(rootSet);

        response.setContentType("application/json");
        response.getWriter().print(jw.toString());
    }

    @GET
    @MenuItem(name = "Debug/Map/Roots")
    @HandlerInfo(schema = "/map/roots", description = "Details not defined yet because the programmer was lazy.")
    public void getMapRoots(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        HashMap<Long, CoordinateRange> rootSet = ClusteringManager.getRoots();
        Iterator<Entry<Long, CoordinateRange>> iter = rootSet.entrySet().iterator();

        HtmlWriter writer = es.getWriter();
        writer.table_Start();
        while(iter.hasNext())
        {
            Entry<Long, CoordinateRange> entry = iter.next();
            writer.tr_Start();
            writer.td(entry.getKey().toString());
            writer.td(entry.getValue().getLabel());
            writer.tr_End();
        }
        writer.table_End();

        response.getWriter().print(writer.getString(locale));
    }

    @GET
    @HandlerInfo(schema = "/map/root/(rootId)/label/(label)", description = "Details not defined yet because the programmer was lazy.")
    public void getMapRootSetLabel(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response,
            @QueryParam(value = "rootId") Long rootId,
            @QueryParam(value = "label") String label) throws Exception
    {
        HashMap<Long, CoordinateRange> roots = ClusteringManager.getRoots();
        JsonWriter jw;
        
        synchronized(roots)
        {
            Long root = ClusteringManager.getRoot(rootId);
            roots.get(root).setLabel(label);
            
            jw = new JsonWriter(roots.get(root));
        }

        response.setContentType("application/json");
        response.getWriter().print(jw.toString());
    }

    @GET
    @MenuItem(name = "Debug/Map/Roots/Clear")
    @HandlerInfo(schema = "/map/roots/clear", description = "Details not defined yet because the programmer was lazy.")
    public void getMapRootsClear(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        HashMap<Long, CoordinateRange> rootSet = ClusteringManager.clearRoots();

        JsonWriter jw = new JsonWriter(rootSet);

        response.setContentType("application/json");
        response.getWriter().print(jw.toString());
    }

    @GET
    @MenuItem(name = "Debug/Map/SpatialIndexes")
    @HandlerInfo(schema = "/map/spatialindexes", description = "Details not defined yet because the programmer was lazy.")
    public void getSpatialIndexes(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        Set<String> spatialIndexKeys = es.getAftermathController().getSpatialIndexKeys();

        // SpatialIndex<?> spatialIndex =
        // es.getAftermathController().getSpatialIndex(index);

        JsonWriter jw = new JsonWriter(spatialIndexKeys);
        response.setContentType("application/json");
        response.getWriter().print(jw.toString());
    }

    @GET
    @HandlerInfo(schema = "/map/spatialindex/(index)/dive/(diveparams)", description = "Details not defined yet because the programmer was lazy.")
    public void getSpatialIndexDive(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response, @QueryParam(value = "index") String index,
            @QueryParam(value = "diveparams") String diveparams) throws Exception
    {
        SpatialIndex<?> spatialIndex = es.getAftermathController().getSpatialIndex(index);

        HtmlWriter writer = es.getWriter();

        String[] divisors = diveparams.split("(?!^)");

        for (String s : divisors)
        {
            spatialIndex = spatialIndex.getQuadrantByIndex(Integer.valueOf(s));
        }

        if (spatialIndex.getIndex() == null)
        {
            writer.table_Start();
            for (int i = 0; i < 4; i++)
            {
                if (spatialIndex.getQuadrantByIndex(i).getIndex() == null)
                {
                    writer.tr_Start();
                    writer.td("<a href=\"/aftermath/map/spatialindex/" + index + "/dive/" + diveparams + i + "\">"
                            + spatialIndex.getQuadrantByIndex(i).toString() + "</a>");
                    writer.tr_End();
                } else
                {
                    writer.tr_Start();
                    Set<Long> setLong = spatialIndex.getQuadrantByIndex(i).getIndex().keySet();
                    if (setLong.size() > 0)
                    {
                        StringBuilder sb = new StringBuilder();
                        for (Long l : setLong)
                        {
                            sb.append(",<a href=\"/aftermath/map/node/" + l + "\">" + l + "</a>");
                        }
                        writer.td(sb.substring(1));
                    } else
                    {
                        writer.td("&nbsp;");
                    }

                    writer.tr_End();
                }
            }
            writer.table_End();
        } else
        {
            writer.table_Start();
            writer.tr_Start();
            writer.text("EMPTY");
            writer.tr_End();
            writer.table_End();
        }

        response.getWriter().print(writer.getString(locale));
    }

    @POST
    @HandlerInfo(schema = "/map/weight", description = "Details not defined yet because the programmer was lazy.")
    public void postMapEdgesSetWeight(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        Boolean authorative = parent.getSession().isAuthorative();
        long inId = parent.getTaskId();
        BufferedReader br = request.getReader();
        String inputString = br.readLine();

        String[] s = inputString.split("&");

        String referer = baseRequest.getHeader("Referer");
        HtmlWriter writer = es.getWriter();

        long timeStamp = System.currentTimeMillis();

        // First cycle to renormalize

        float normalizationFactor = 0;
        int count = 0;
        HashMap<Long, Integer> edgeWeightMap = new HashMap<Long, Integer>();
        float minNormalization = Float.MAX_VALUE;
        float maxNormalization = 0.0f;
        CoordinateRange coordRange = new CoordinateRange();

        for (String item : s)
        {
            String[] s2 = item.split("=");
            if (s2.length != 2)
            {
                continue;
            }

            long edge = Long.valueOf(s2[0]);
            int weight = Math.round(Float.valueOf(s2[1]));

            edgeWeightMap.put(edge, weight);

            MapEdge mEdge = es.getAftermathController().getEdgeData().get(edge);
            coordRange.add(mEdge.getLongitude(), mEdge.getLatitude());

            if (mEdge.getWeight() > 0 && weight > 0)
            {
                float normalizeWeight = mEdge.getWeight() / weight;

                if (minNormalization > normalizeWeight)
                    minNormalization = normalizeWeight;
                if (maxNormalization < normalizeWeight)
                    maxNormalization = normalizeWeight;
                normalizationFactor += normalizeWeight;

                count++;

                float normalizationDelta = maxNormalization - minNormalization;
                if (normalizationDelta > 20)
                {
                    new Task(es.getAftermathController().getProfiler(), parent, "NormalizationDelta Exceeded 20!", null)
                            .end();
                    normalizationDelta = 20;
                }
            }
        }

        ClusteringManager.tryMerge(edgeWeightMap.keySet(), coordRange);

        writer.table_Start();
        writer.tr_Start();
        writer.td("edge");
        writer.td("previousWeight");
        writer.td("newWeight");
        writer.td("finalWeight");
        writer.td("minNormalization");
        writer.td("maxNormalization");
        writer.td("confidence");
        writer.tr_End();

        boolean lowRes = edgeWeightMap.size() <= 1;
        for (long edge : edgeWeightMap.keySet())
        {
            int previousWeight = es.getAftermathController().getEdgeData().get(edge).getWeight();
            int weight = edgeWeightMap.get(edge);
            int finalWeight = -1;
            float confidence = -1;

            if (lowRes)
            {
                weight = (int) Math.floor(weight / (float) 3.35);
                LowResolutionHistogram weightInputs = es.getAftermathController().getEdgeData().get(edge)
                        .addWeightInputLowRes(inId, timeStamp, weight);

                confidence = es.getAftermathController().getEdgeData().get(edge).getConfidence();
                finalWeight = weightInputs.getWeight();
            } else
            {
                HistogramBase weightInputs = es.getAftermathController().getEdgeData().get(edge)
                        .addWeightInput(authorative, inId, timeStamp, weight);

                confidence = es.getAftermathController().getEdgeData().get(edge).getConfidence();
                finalWeight = weightInputs.getWeight();
            }

            writer.tr_Start();
            writer.td(String.valueOf(edge));
            writer.td(String.valueOf(previousWeight));
            writer.td(String.valueOf(weight));
            writer.td(String.valueOf(finalWeight));
            writer.td(String.valueOf(minNormalization));
            writer.td(String.valueOf(maxNormalization));
            writer.td(String.valueOf(confidence));
            writer.tr_End();
        }
        writer.table_End();

        writer.text("<a href=\"" + referer + "\">Go Back</a><br/>");
        response.getWriter().print(writer.getString(locale));
    }

    @POST
    @HandlerInfo(schema = "/map/mark", description = "Details not defined yet because the programmer was lazy.")
    public void postMapEdgesSetMark(String target, String locale, Task parent, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        BufferedReader br = request.getReader();
        String inputString = br.readLine();

        String[] s = inputString.split("&");

        for (String item : s)
        {
            String[] s2 = item.split("=");
            if (s2.length != 2)
            {
                continue;
            }

            long edge = Long.valueOf(s2[0]);
            int weight = Math.round(Float.valueOf(s2[1]));

            MapEdge mEdge = es.getAftermathController().getEdgeData().get(edge);
            mEdge.setMarked(true);
        }
    }

    private void drawVertices(HtmlWriter writer, MapVertex focalPoint, int zoom, int depth, String filter)
            throws Exception
    {
        SpatialIndex<Coordinates> diveQuadrant = es.getAftermathController().getSpatialIndex().dive(focalPoint);
        Coordinates[] bounds = Coordinates.getBounds(focalPoint, zoom, MapVertex.WIDTH, MapVertex.HEIGHT);
        List<Long> vertices = es.getAftermathController().getSpatialIndex().getVerticesWithinBounds(bounds);

        for (Long l : vertices)
        {
            MapVertex position = es.getAftermathController().getMapData().get(l);
            double[] bearing = position.getBearing(focalPoint, zoom);

            int bearingX = (int) bearing[0] + MapVertex.WIDTH / 2;
            int bearingY = (int) bearing[1] + MapVertex.HEIGHT / 2;

            String arcColor = "#800000";
            if (position.getEdges().size() > 2)
            {
                arcColor = "#FF0000";
            } else if (position.getEdges().size() == 1)
            {
                arcColor = "#808000";
            } else if (position.getEdges().size() == 0)
            {
                arcColor = "#000000";
            }

            writer.drawArc(bearingX, bearingY, 7, 3, arcColor);
            writer.drawVertex("mapCanvas", 10, 1.0f, bearingX + 5, bearingY + 15, String.valueOf(position.getId()),
                    arcColor);
        }
    }

    private void drawRoads(HtmlWriter writer, MapVertex focalPoint, int zoom, int depth, String filter,
            boolean authorative) throws Exception
    {
        HashSet<String> filterSet = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(filter, ",");
        while (st.hasMoreTokens())
        {
            filterSet.add(st.nextToken());
        }

        Coordinates[] bounds = Coordinates.getBounds(focalPoint, zoom, MapVertex.WIDTH, MapVertex.HEIGHT);
        HashMap<Long, Integer> masterDepthList = new HashMap<Long, Integer>(200);

        List<Long> edges = focalPoint.getEdges();
        DistinctOrderedSet masterEdgeList = buildMasterEdgeListFromDepth(focalPoint, masterDepthList, edges, depth,
                zoom);

        masterEdgeList.reset(OrderType.FIFO);

        // The MasterEdgeList doesn't sync with the JavaScript Version. Why??
        while (masterEdgeList.hasNext())
        {
            Long edge = masterEdgeList.next();
            MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edge);
            long[] verticesList = mapEdge.getVertices();

            for (int i = 0; i < verticesList.length - 1; i++)
            {
                double[] focusBearing = es.getAftermathController().getMapData().get(verticesList[i])
                        .getBearing(focalPoint, zoom);
                double[] drawBearing = es.getAftermathController().getMapData().get(verticesList[i + 1])
                        .getBearing(focalPoint, zoom);

                int startPointX = (int) focusBearing[0] + MapVertex.WIDTH / 2;
                int startPointY = (int) focusBearing[1] + MapVertex.HEIGHT / 2;

                int drawPointX = (int) drawBearing[0] + MapVertex.WIDTH / 2;
                int drawPointY = (int) drawBearing[1] + MapVertex.HEIGHT / 2;

                RoadTypes mode = mapEdge.getMode();
                int width = 1;

                if (filterSet.size() == 0 || filterSet.contains(String.valueOf(mode)))
                {
                } else
                {
                    continue;
                }
                switch (String.valueOf(mode))
                {
                case "secondary":
                    width = 10;
                    break;
                case "secondary_link":
                    width = 9;
                    break;
                case "primary":
                    width = 10;
                    break;
                case "primary_link":
                    width = 9;
                    break;
                case "tertiary":
                    width = 8;
                    break;
                case "tertiary_link":
                    width = 7;
                    break;
                case "residential":
                    width = 8;
                    break;
                case "living_street":
                    break;
                case "service":
                    break;
                case "motorway":
                    width = 10;
                    break;
                case "motorway_link":
                    width = 9;
                    break;
                case "rail":
                case "subway":
                case "subway_entrance":
                case "station":
                    width = 12;
                    break;
                case "road":
                    width = 4;
                    break;
                case "trunk":
                case "trunk_link":
                    width = 4;
                    break;
                case "pedestrian":
                case "footway":
                case "path":
                case "steps":
                    width = 2;
                    break;
                case "unclassified":
                    width = 1;
                    break;
                case "null":
                    width = 1;
                    break;
                case "platform":
                    width = 14;
                    break;
                default:
                    width = 1;
                    break;
                }

                if (zoom >= 65535)
                {
                    width *= zoom / 65536;
                }
                if (width < 4)
                {
                    width = 4;
                }

                float weight = (float) mapEdge.getWeight();
                float weightCeiling = mapEdge.getWeightRangeCeiling(authorative) - 1.0f;
                float weightRange = weight / weightCeiling;
                int weightScore = Math.round(weightRange * 255);

                int confidenceScore = Math.round(Math.abs(mapEdge.getConfidence() - 1) * 255);
                if (weightScore < confidenceScore)
                {
                    weightScore = confidenceScore;
                }

                int rr = (confidenceScore > weightScore) ? confidenceScore : weightScore;
                int gg = confidenceScore - Math.round((weightRange * confidenceScore) / 2);

                String rrHex = Integer.toHexString(0x100 | rr).substring(1);
                String ggHex = Integer.toHexString(0x100 | gg).substring(1);
                String hx3 = (mapEdge.getMarked() ? "FF" : "00");
                String lineAlpha = (width > 8) ? "FF" : "80";

                String color = "#" + rrHex + ggHex + "00";
                String color2 = "#" + hx3 + hx3 + "00" + lineAlpha;
                
                int strokeWidth = (mapEdge.getMarked())?2:1;

                writer.drawCanvasLineAsRect("mapCanvas", width, color, color2, startPointX, startPointY, drawPointX,
                        drawPointY, strokeWidth);
                if (mapEdge.getConfidence() < 0.5)
                {
                    writer.drawImage("mapCanvas", 16, 16, "lowConfidence", ((startPointX + drawPointX) / 2) - 8,
                            ((startPointY + drawPointY) / 2) - 8);
                }
            }
        }
        writer.addRoadLineData();
    }

    private void drawTransports(HtmlWriter writer, Coordinates focalPoint, int zoom, int depth) throws Exception
    {
        List<Transport> transporters = es.getAftermathController().getTransporters();
        for (Transport t : transporters)
        {
            drawTransport(writer, t, focalPoint, zoom, depth);
        }
    }

    private void drawSpatialIndex(HtmlWriter writer, Coordinates focalPoint, int zoom) throws Exception
    {
        SpatialIndex<Coordinates> diveQuadrant = es.getAftermathController().getSpatialIndex().dive(focalPoint);
        while (diveQuadrant != null)
        {
            Coordinates[] bounds = diveQuadrant.getBounds();
            double[] minPoint = bounds[0].getBearing(focalPoint, zoom);
            double[] maxPoint = bounds[1].getBearing(focalPoint, zoom);

            int xMid = (int) (minPoint[0] + maxPoint[0]) / 2;
            int yMid = (int) (minPoint[1] + maxPoint[1]) / 2;

            writer.drawCanvasLine("mapCanvas", 2.0f, "#804080", 0.35f, (int) xMid + MapVertex.WIDTH / 2,
                    (int) minPoint[1] + MapVertex.HEIGHT / 2, (int) xMid + MapVertex.WIDTH / 2,
                    (int) maxPoint[1] + MapVertex.HEIGHT / 2);
            writer.drawCanvasLine("mapCanvas", 2.0f, "#804080", 0.35f, (int) minPoint[0] + MapVertex.WIDTH / 2,
                    (int) yMid + MapVertex.HEIGHT / 2, (int) maxPoint[0] + MapVertex.WIDTH / 2,
                    (int) yMid + MapVertex.HEIGHT / 2);
            diveQuadrant = diveQuadrant.getParent();
        }
    }

    private void drawGroups(HtmlWriter writer, Coordinates focalPoint, int zoom) throws Exception
    {
        HashMap<Long, CoordinateRange> groups = ClusteringManager.getRoots();
        Iterator<Entry<Long, CoordinateRange>> entry = groups.entrySet().iterator();

        while (entry.hasNext())
        {
            Coordinates[] bounds = entry.next().getValue().getMinMaxCoords();
            double[] minPoint = bounds[0].getBearing(focalPoint, zoom);
            double[] maxPoint = bounds[1].getBearing(focalPoint, zoom);

            int xMin = ((int) (minPoint[0])) + (MapVertex.WIDTH / 2);
            int yMin = ((int) (minPoint[1])) + (MapVertex.HEIGHT / 2);
            int xMax = ((int) (maxPoint[0])) + (MapVertex.WIDTH / 2);
            int yMax = ((int) (maxPoint[1])) + (MapVertex.HEIGHT / 2);

            writer.drawRect("mapCanvas", 2.0f, "#00FFFF40", "#000000FF", 1.0f, xMin, yMin, xMax, yMax);
        }
    }

    private void drawDepot(HtmlWriter writer, Coordinates focalPoint, int zoom) throws Exception
    {
        List<Long> depotIds = es.getAftermathController().getSpatialIndexDepot().getNearestNodeRegion(focalPoint);

        for (Long depotId : depotIds)
        {
            Depot depot = es.getAftermathController().getDepotData().get(depotId);

            double[] point = depot.getBearing(focalPoint, zoom);
            int edgeBearingX = (int) point[0] + MapVertex.WIDTH / 2;
            int edgeBearingY = (int) point[1] + MapVertex.HEIGHT / 2;

            String color = (depot.isActive())?"#0080FF":"#808080";
            writer.drawVertex("mapCanvas", 16, edgeBearingX + 5, edgeBearingY + 5, depot.getName(), color);
            writer.drawImage("mapCanvas", 32, 32, "depot", edgeBearingX - 16, edgeBearingY - 16);
        }
    }

    public void drawTransport(HtmlWriter writer, Transport transport, Coordinates focalPoint, int zoom, int depth)
            throws Exception
    {
        Coordinates position = transport.getPosition();
        double[] carBearing = position.getBearing(focalPoint, zoom);

        Coordinates destination = transport.getNode();
        double[] destinationBearing = destination.getBearing(focalPoint, zoom);

        int carBearingX = (int) carBearing[0] + MapVertex.WIDTH / 2;
        int carBearingY = (int) carBearing[1] + MapVertex.HEIGHT / 2;

        int destBearingX = (int) destinationBearing[0] + MapVertex.WIDTH / 2;
        int destBearingY = (int) destinationBearing[1] + MapVertex.HEIGHT / 2;

        MapVertex carPrevPosVertex = es.getAftermathController().getMapData().get(transport.getPreviousNode().getId());
        double[] carPrevBearing = carPrevPosVertex.getBearing(focalPoint, zoom);
        int carPrevBearingX = (int) carPrevBearing[0] + MapVertex.WIDTH / 2;
        int carPrevBearingY = (int) carPrevBearing[1] + MapVertex.HEIGHT / 2;

        writer.drawArc(carBearingX, carBearingY, 5, 3, "#00FF00");
        writer.drawVertex("mapCanvas", 16, 1.0f, carBearingX + 10, carBearingY + 5, String.valueOf(transport.getId()),
                "#00AA00");
        writer.drawCanvasLine("mapCanvas", 1, "#0000FF", 1.0f, destBearingX, destBearingY, carPrevBearingX,
                carPrevBearingY);
    }

    public void writeSummaryNode(HtmlWriter writer, String locale, MapVertex focalPoint, int zoom, int depth,
            boolean authorative) throws Exception
    {
        LocaleBase localizer = es.getLocale(locale);

        writer.h1(localizer.H1_SUMMARYNODES);
        writer.table_Start(null, null, "sortable", "100%", "1");
        writer.tHead_Start();
        writer.tr_Start();
        writer.th(localizer.TH_EDGE_ID);
        writer.th(localizer.TH_VERTICES);
        writer.th(localizer.TH_MODE);
        writer.th(localizer.TH_NORMAL_HISTOGRAM);
        writer.th(localizer.TH_HISTOGRAMTIMES);
        writer.tr_End();
        writer.tHead_End();

        List<Long> edges = focalPoint.getEdges();

        writer.tBody_Start();
        for (Long e : edges)
        {
            MapEdge mapEdge = es.getAftermathController().getEdgeData().get(e);

            writer.tr_Start();
            writer.td(String.valueOf(mapEdge.getId()));
            writer.td_Start();
            for (Long vertexId : mapEdge.getVertices())
            {
                writer.text(String.valueOf(vertexId) + "</br>");
            }
            writer.td_End();
            writer.td(mapEdge.getMode().toString());
            writer.td(mapEdge.getHistogramDataString(authorative));
            writer.td(mapEdge.getHistogramTimeDeltaString(authorative));
            writer.tr_End();
        }
        writer.tBody_End();
        writer.table_End();
    }

    public void writeSummaryNeighboringNodes(HtmlWriter writer, String locale, MapVertex focalPoint, int zoom,
            int depth) throws Exception
    {
        LocaleBase localizer = es.getLocale(locale);

        writer.h1(localizer.H1_NEIGHBORNODES);
        writer.table_Start(null, null, "sortable", "100%", "1");
        writer.tHead_Start();
        writer.tr_Start();
        writer.th(localizer.TH_VERTEX_ID_1);
        writer.th(localizer.TH_VERTEX_ID_2);
        writer.th(localizer.TH_EDGE_ID);
        writer.th(localizer.TH_COORDINATES);
        writer.th(localizer.TH_TYPE);
        writer.th(localizer.TH_SCORE);
        writer.th(localizer.TH_WEIGHT);
        writer.th(localizer.TH_GROUP);
        writer.th(localizer.TH_CONFIDENCE);
        writer.th(localizer.TH_SET_WEIGHT);
        writer.th(localizer.TH_AUTHORATIVE_HISTOGRAM);
        writer.th(localizer.TH_NORMAL_HISTOGRAM);
        writer.th(localizer.TH_LOW_HISTOGRAM);
        writer.tr_End();
        writer.tHead_End();
        writer.form_Start("/aftermath/map/weight", "POST");

        HashMap<Long, Integer> masterDepthList = new HashMap<Long, Integer>(200);
        List<Long> edges = focalPoint.getEdges();
        DistinctOrderedSet masterEdgeList = buildMasterEdgeListFromDepth(focalPoint, masterDepthList, edges, 5, zoom);
        masterEdgeList.reset(OrderType.FIFO);

        writer.tBody_Start();
        while (masterEdgeList.hasNext())
        {
            long e = masterEdgeList.next();
            MapEdge mapEdge = es.getAftermathController().getEdgeData().get(e);

            int weight = es.getAftermathController().getEdgeData().get(e).getWeight();
            int score = es.getAftermathController().getEdgeData().get(e).getScore();
            float confidence = es.getAftermathController().getEdgeData().get(e).getConfidence();
            Long group = es.getAftermathController().getEdgeData().get(e).getGroup();

            writer.tr_Start();
            writer.td("<A href=\"/aftermath/map/node/" + mapEdge.getVertices()[0] + "/canvas?depth="
                    + String.valueOf(depth) + "&zoom=" + String.valueOf(zoom) + "\">" + mapEdge.getVertices()[0]
                    + "</A>");
            writer.td("<A href=\"/aftermath/map/node/" + mapEdge.getVertices()[1] + "/canvas?depth="
                    + String.valueOf(depth) + "&zoom=" + String.valueOf(zoom) + "\">" + mapEdge.getVertices()[1]
                    + "</A>");
            writer.td(String.valueOf(mapEdge.getId()));
            writer.td("<A href=\"/aftermath/map/coord/" + mapEdge.getLongitude() + "/" + mapEdge.getLatitude() + "/canvas?depth="
                    + String.valueOf(depth) + "&zoom=" + String.valueOf(zoom) + "\">" + mapEdge.toString()
                    + "</A>");
            writer.td(mapEdge.getMode().name());
            writer.td(String.valueOf(score));
            writer.td(String.valueOf(weight));
            if (group == null)
            {
                writer.td("0");
            } else
            {
                writer.td(String.valueOf(group));
            }
            writer.td(String.valueOf(confidence));
            writer.td("<input type=\"text\" name=\"" + e + "\" value=\"\">");
            writer.td(mapEdge.getHistogramDataString(true));
            writer.td(mapEdge.getHistogramDataString(false));
            writer.td(mapEdge.getLowResHistogramDataString());
            writer.tr_End();
        }
        writer.tBody_End();

        writer.tFoot_Start();
        writer.tr_Start();
        writer.td("<input type=\"submit\" value=\"" + localizer.BTN_SEND_WEIGHTS + "\"/>", "", "", "text-align:center",
                13);
        writer.tr_End();
        writer.tFoot_End();
        writer.form_End();
        writer.table_End();
    }

    public void writeSummaryVehicles(HtmlWriter writer, String locale, int zoom, int depth) throws Exception
    {
        LocaleBase localizer = es.getLocale(locale);

        writer.h1(localizer.H1_VEHICLES);
        writer.table_Start(null, null, "sortable", "100%", "1");
        writer.tr_Start();
        writer.th(localizer.TH_ID_GENERIC);
        writer.th(localizer.TH_COORDINATES);
        writer.th(localizer.TH_TICKS);
        writer.th(localizer.TH_TYPE);
        writer.th(localizer.TH_SCORE);
        writer.th(localizer.TH_DESTINATION);
        writer.tr_End();

        List<Transport> transporters = es.getAftermathController().getTransporters();
        for (int i = 0; i < transporters.size(); i++)
        {
            Transport t = transporters.get(i);
            Long vertexId = t.getNode().getId();
            writer.tr_Start();
            Coordinates coordinates = es.getAftermathController().getMapData().get(vertexId);
            writer.td("<A href=\"/aftermath/map/vehicle/" + i + "?depth=" + depth + "&zoom=" + zoom + "\">" + t.getId()
                    + "</A>");
            writer.td("<A href=\"/aftermath/map/coord/" + coordinates.getLongitude() + "/" + coordinates.getLatitude()
                    + "/canvas\">" + coordinates.toString() + "</A>");
            writer.td(String.valueOf(t.getTicks()));
            try
            {
                writer.td(t.getEdge().getMode().name());
                writer.td(String.valueOf(t.getEdge().getScore()));
                writer.td(t.getDestination().toString());

            } catch (Exception e)
            {
                writer.td("Null");
                writer.td("");
                writer.td("");
            }
            writer.tr_End();
        }
        writer.table_End();
    }

    public void renderMap(HtmlWriter writer, MapVertex node, int zoom) throws ResponseException
    {
        writer.tr_Start();
        writer.td_Start();
        writer.text("<div id=\"googleMap\" style=\"width:" + MapVertex.WIDTH + "px;height:" + MapVertex.HEIGHT
                + "px;position:absolute; z-index:-5\"></div>");
        String scriptstr = "<script>" + "function myMap() {" + "var mapProp= { center:new google.maps.LatLng("
                + node.getLatitude() + "," + node.getLongitude() + "), zoom:" + zoom + " };"
                + "var map=new google.maps.Map(document.getElementById(\"googleMap\"),mapProp);}"
                + "var drawingManager = new google.maps.drawing.DrawingManager();" + "drawingManager.setMap(map);"
                + "</script>";
        writer.text(scriptstr);
        writer.text("<script src=\"https://maps.googleapis.com/maps/api/js?key=" + AftermathServer.GOOGLE_MAP_API_KEY
                + "&callback=myMap&libraries=drawing\"></script>");
        writer.td_End();
        writer.tr_End();
    }

    private DistinctOrderedSet buildMasterEdgeListFromBounds(MapVertex focalPoint, int depth, int zoom)
    {
        return null;
    }

    private DistinctOrderedSet buildMasterEdgeListFromDepth(MapVertex focalPoint,
            HashMap<Long, Integer> masterDepthList, List<Long> edges, int depth, int zoom) throws Exception
    {
        DistinctOrderedSet masterEdgeList = new DistinctOrderedSet(200);
        DistinctOrderedSet masterVertexList = new DistinctOrderedSet(200);
        for (int i = 0; i < depth; i++)
        {
            masterEdgeList.add(edges);
            int vertexListSizeBefore = masterVertexList.size();
            for (Long edge : edges)
            {
                MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edge);
                for (Long vertexId : mapEdge.getVertices())
                {
                    MapVertex vertex = es.getAftermathController().getMapData().get(vertexId);
                    double[] bearing = focalPoint.getBearing(vertex, zoom);
                    if (bearing[0] > -MapVertex.WIDTH && bearing[0] < MapVertex.WIDTH && bearing[1] > -MapVertex.HEIGHT
                            && bearing[1] < MapVertex.HEIGHT)
                    {
                        while (masterVertexList.add(vertex.getId()) == true && vertex.getEdges().size() == 2)
                        {
                            masterDepthList.put(vertex.getId(), i);
                            for (Long cascadeEdge : vertex.getEdges())
                            {
                                masterEdgeList.add(cascadeEdge);
                                if (es.getAftermathController().getEdgeData().get(cascadeEdge).getScore() > maxScore)
                                {
                                    maxScore = es.getAftermathController().getEdgeData().get(cascadeEdge).getScore();
                                }
                                if (cascadeEdge == edge)
                                    continue;
                                for (Long cascadeVertexId : es.getAftermathController().getEdgeData().get(cascadeEdge)
                                        .getVertices())
                                {
                                    if (cascadeVertexId == vertex.getId())
                                        continue;
                                    vertex = es.getAftermathController().getMapData().get(cascadeVertexId);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            masterVertexList.resetAndJump(OrderType.FIFO, vertexListSizeBefore);
            edges = new ArrayList<Long>();
            while (masterVertexList.hasNext())
            {
                long vertexId = masterVertexList.next();
                edges.addAll(es.getAftermathController().getMapData().get(vertexId).getEdges());
            }
        }
        return masterEdgeList;
    }
}