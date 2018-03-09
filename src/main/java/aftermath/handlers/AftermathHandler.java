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
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import main.java.aftermath.engine.Depot;
import main.java.aftermath.locale.LocaleBase;
import main.java.aftermath.server.AftermathServer;
import main.java.aftermath.vehicles.Transport;
import main.java.encephalon.annotations.HandlerInfo;
import main.java.encephalon.annotations.methods.GET;
import main.java.encephalon.annotations.methods.POST;
import main.java.encephalon.annotations.methods.QueryParam;
import main.java.encephalon.annotations.methods.QueryString;
import main.java.encephalon.dto.Coordinates;
import main.java.encephalon.dto.DistinctOrderedSet;
import main.java.encephalon.dto.DistinctOrderedSet.OrderType;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.dto.MapEdge.RoadTypes;
import main.java.encephalon.dto.MapResponseDto;
import main.java.encephalon.dto.MapVertex;
import main.java.encephalon.dto.MapVertexLite;
import main.java.encephalon.histogram.HistogramBase;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.DefaultHandler;
import main.java.encephalon.spatialIndex.SpatialIndex;
import main.java.encephalon.writers.HtmlWriter;
import main.java.encephalon.writers.JsonWriter;

public class AftermathHandler extends DefaultHandler{
	private AftermathServer es;
	private long maxScore = 1;
	
	private class EdgeWeightInfo
	{
		public long edge;
		public int weight;
		
		public EdgeWeightInfo(long edge, int weight)
		{
			this.edge = edge;
			this.weight = weight;
		}
	}

	public AftermathHandler(AftermathServer instance) {
		super();

		this.es = instance;
	}

	@GET
	@HandlerInfo(schema="/")
	public void getMap(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		String destination = baseRequest.getRootURL() + "/aftermath/map/coord/\" + position.coords.longitude + \"/\" + position.coords.latitude";
		
		HtmlWriter writer = new HtmlWriter(2, es);
		writer.script_Start();
		writer.text("if(navigator && navigator.geolocation) {");
		writer.text("navigator.geolocation.getCurrentPosition(showPosition);");
		writer.text("} else {");
		writer.text("document.write(\"Beef\")");
		writer.text("}");
		writer.text("function showPosition(position) { document.write(\"Relocating!\"); window.location = \"" + destination + "; }");
		writer.script_End();
		
		response.getWriter().print(writer.getString(locale));
	}
	
	@GET
	@HandlerInfo(schema="/map")
	public void getMapData(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		LocaleBase localizer = es.getLocale(locale);
		HashMap<Long, MapVertex> mapData = es.getAftermathController().getMapData();
		HashMap<Long, MapEdge> mapEdges = es.getAftermathController().getEdgeData();
		Iterator<Entry<Long, MapVertex>> iter = mapData.entrySet().iterator();

		HtmlWriter writer = new HtmlWriter(2, es);

		writer.table_Start(null, null, "sortable");
		writer.tr_Start();
		writer.th(localizer.TH_NODE);
		writer.tr_End();
		int i = 0;
		while(iter.hasNext())
		{
			Map.Entry<Long, MapVertex> kvPair = (Map.Entry<Long, MapVertex>) iter.next();
			String key = String.valueOf(kvPair.getKey());
			MapVertex node = kvPair.getValue();
			List<Long> connections = node.getEdges();
			for(Long l : connections)
			{
				if(mapEdges.get(l).getMode() == RoadTypes.primary || mapEdges.get(l).getMode() == RoadTypes.secondary)
				{
					writer.tr_Start();
					writer.td("<A href=\"/aftermath/map/node/" + key + "\">" + key + "</A>: " + mapEdges.get(l).getMode() + " :: " + node.toString() + " - " + Arrays.toString(connections.toArray()));
					writer.tr_End();
					i++;
					break;
				}	
			}
			if(i > 1000) break;
		}
		writer.table_End();

		response.getWriter().print(writer.getString(locale));
	}

	@GET
	@HandlerInfo(schema="/map/json")
	public void getMapDataJson(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		HashMap<Long, MapVertex> mapData = es.getAftermathController().getMapData();
		
		JsonWriter jw = new JsonWriter(mapData);

		response.getWriter().print(jw.toString());
	}
	
	@GET
	@HandlerInfo(schema="/map/vehicle/(vehicleId)/json")
	public void getVehicleInfo(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="vehicleId") Integer vehicleId) throws Exception
	{
		Transport vehicle = es.getAftermathController().getTransporters().get(vehicleId);
		
		JsonWriter jw = new JsonWriter(vehicle);

		response.setContentType("application/json");
		response.getWriter().print(jw.toString());
	}

	@GET
	@HandlerInfo(schema="/delay")
	public void getDelay(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		Thread.sleep(5000);
		response.getWriter().print("Delayed");
	}
	
	@GET
	@HandlerInfo(schema="/map/node/(uid)")
	public void getMapNode(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="uid") Long uid, 
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth, @QueryString(value="roadType", _default="") String filter,
			@QueryString(value="nodeVertices", _default="false") Boolean drawVertices) throws Exception
	{	
		// @QueryString(value="zoom", _default="18")
		getMapNodeWithDepthAndZoom(target, locale, parent, baseRequest, request, response, uid, depth, zoom, filter, drawVertices);
	}

	@GET
	@HandlerInfo(schema="/map/coord/(longitude)/(latitude)")
	public void getMapNodeWithCoordinates(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="longitude") Double longitude, @QueryParam(value="latitude") Double latitude,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth, @QueryString(value="roadType", _default="") String filter,
			@QueryString(value="nodeVertices", _default="false") Boolean drawVertices) throws Exception
	{
		Coordinates coords = new Coordinates(longitude, latitude);
		
		Long nodeId = es.getAftermathController().getSpatialIndex().getNearestNode(coords);
		getMapNodeWithDepthAndZoom(target, locale, parent, baseRequest, request, response, nodeId, depth, zoom, filter, drawVertices);
	}
	
	@GET
	@HandlerInfo(schema="/map/coord/(longitude)/(latitude)/canvas")
	public void getMapNodeWithCoordinatesCanvas(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="longitude") Double longitude, @QueryParam(value="latitude") Double latitude,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth) throws Exception
	{
		Coordinates coords = new Coordinates(longitude, latitude);

		Long nodeId = es.getAftermathController().getSpatialIndex().getNearestNode(coords);
		getTestCanvas(target, locale, parent, baseRequest, request, response, nodeId, zoom, depth);
	}

	@GET
	@HandlerInfo(schema="/map/vehicle/(vehicleId)")
	public void getMapNodeByVehicle(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="vehicleId") Integer vehicleId,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth, 
			@QueryString(value="roadType", _default="") String filter, @QueryString(value="drawMap", _default="true") Boolean drawMap) throws Exception
	{	
		List<Transport> transporters = es.getAftermathController().getTransporters();
		Transport transport = transporters.get(vehicleId);

		MapVertex initialNode = transport.getNode();
		HtmlWriter writer = new HtmlWriter(2, es);
	
		if(drawMap == true) renderMap(writer, initialNode, zoom);
		writer.canvas("mapCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2);
		writer.script_Start();
		writer.initializeCanvasJS("mapCanvas");

		int zm = (int)(AftermathServer.GOOGLE_MAP_ZOOMSCALE*Math.pow(2, zoom));
		drawSpatialIndex(writer, initialNode, zm);
		drawRoads(writer, initialNode, zm, depth, filter);
		drawDepot(writer, initialNode, zm);
		drawTransport(writer, transport, initialNode, zm, depth);

		writer.script_End();
		writer.table_Start();

		writeSummaryNode(writer, locale, initialNode, zoom, depth);
		writeSummaryNeighboringNodes(writer, locale, initialNode, zoom, depth);
		writeSummaryVehicles(writer, locale, initialNode, zoom, depth);
		writer.table_End();

		response.getWriter().print(writer.getString(locale));
	}

	@GET
	@HandlerInfo(schema="/map/vehicle/(vehicleId)/canvas")
	public void getMapCanvasNodeByVehicle(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="vehicleId") Integer vehicleId,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth) throws Exception
	{
		Transport t = es.getAftermathController().getTransporters().get(vehicleId);
		MapVertex initialNode = t.getNode();

		getTestCanvas(target, locale, parent, baseRequest, request, response, initialNode.getId(), zoom, depth);
	}
	
	@GET
	@HandlerInfo(schema="/map/add/depot/name/(name)/edge/(edgeId)")
	public void getMapAddDepotEdge(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="name") String name, @QueryParam(value="edgeId") Long edgeId) throws Exception
	{
		MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edgeId);
		Depot d = new Depot(name, 100, mapEdge);
		
		es.getAftermathController().getDepotData().put(d.getId(), d);
		es.getAftermathController().getSpatialIndexDepot().add(d.getId(), d);
	}
	
	public void getMapNodeWithDepthAndZoom(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			Long uid, int depth, int zoom, String filter, Boolean drawVertices) throws Exception
	{
		MapVertex initialNode = es.getAftermathController().getMapData().get(uid);
		HtmlWriter writer = new HtmlWriter(2, es);

		renderMap(writer, initialNode, zoom);

		writer.canvas("mapCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2);
		writer.script_Start();
		writer.initializeCanvasJS("mapCanvas");

		int zm = (int)(AftermathServer.GOOGLE_MAP_ZOOMSCALE*Math.pow(2, zoom));
		drawSpatialIndex(writer, initialNode, zm);
		drawRoads(writer, initialNode, zm, depth, filter);
		if(drawVertices) drawVertices(writer, initialNode, zm, depth, filter);
		drawDepot(writer, initialNode, zm);
		drawTransports(writer, initialNode, zm, depth);

		writer.script_End();
		writer.table_Start();

		writeSummaryNode(writer, locale, initialNode, zoom, depth);
		writeSummaryNeighboringNodes(writer, locale, initialNode, zoom, depth);
		writeSummaryVehicles(writer, locale, initialNode, zoom, depth);
		writer.table_End();

		response.getWriter().print(writer.getString(locale));
	}

	@GET
	@HandlerInfo(schema="/map/node/(uid)")
	public void getMapNodeWith(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="uid") Long uid,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth, 
			@QueryString(value="roadType", _default="") String filter, @QueryString(value="nodeVertices", _default="false") Boolean drawVertices) throws Exception
	{
		getMapNodeWithDepthAndZoom(target, locale, parent, baseRequest, request, response, uid, depth, zoom, filter, drawVertices);
	}

	@GET
	@HandlerInfo(schema="/map/node/(uid)/canvas")
	public void getTestCanvas(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="uid") Long uid,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth) throws Exception
	{
		MapVertex initialNode = es.getAftermathController().getMapData().get(uid);
		HtmlWriter writer = new HtmlWriter(2, es);
	
		renderMap(writer, initialNode, zoom);
		writer.table_Start(null, null, "tableContainer", 100, null);
			writer.tr_Start();
				writer.td_Start("canvasContainer");
					writer.table_Start("Map", null, "GlobalMap", MapVertex.WIDTH, MapVertex.HEIGHT, "hidden");
						writer.tr_Start();
							writer.td_Start(1, MapVertex.WIDTH, MapVertex.HEIGHT);
								writer.div_Start("overflow:hidden; height:600px");
									writer.canvas("mapCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2, 0, 25);
									// This one is offset too low.  Overlaying issues.
									writer.canvas("tempLineCanvas", MapVertex.WIDTH, MapVertex.HEIGHT, 2, -MapVertex.HEIGHT, 30, "none");
								writer.div_End();
								writer.canvasInputDiv("canvasInput", "#FF0000");
							writer.td_End();
						writer.tr_End();
					writer.table_End();
					writer.table_Start();
						writer.tr_Start();
							writer.td_Start();
								writeSummaryNode(writer, locale, initialNode, zoom, depth);
								writeSummaryNeighboringNodes(writer, locale, initialNode, zoom, depth);
								writeSummaryVehicles(writer, locale, initialNode, zoom, depth);
							writer.td_End();
						writer.tr_End();
					writer.table_End();
				writer.td_End();
			writer.tr_End();
		writer.table_End();
		
		writer.script_Start("application/javascript");
		writer.text("loadJSON("
				+ "\"" + baseRequest.getRootURL() + "/aftermath/map/node/"
				+ uid + "/json?depth=" 
				+ depth + "&zoom=" 
				+ zoom + "\"," 
				+ zoom + ");");
		writer.script_End();

		response.getWriter().print(writer.getString(locale));
	}

	@GET
	@HandlerInfo(schema="/map/start/(sLat)/(sLon)/end/(eLat)/(eLon)")
	public void getMapNodesWithinBounds(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="sLat") Long sLat, @QueryParam(value="sLon") Long sLon,
			@QueryParam(value="eLat") Long eLat, @QueryParam(value="eLon") Long eLon) throws Exception
	{
		Coordinates[] coordsRange = { new Coordinates(sLon, sLat), new Coordinates(eLon, eLat) };
		List<Long> list = es.getAftermathController().getSpatialIndex().getVerticesWithinBounds(coordsRange);
		
		// TODO: FILL IN FOR EDGES

		HashSet<Long> edgeList = new HashSet<Long>();

		for(Long vertex : list)
		{
			edgeList.addAll(es.getAftermathController().getMapData().get(vertex).getEdges());
		}

		List<MapEdge> edgeData = new ArrayList<MapEdge>();
		for(Long edge : edgeList)
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
	@HandlerInfo(schema="/map/node/(uid)/vehicles/json")
	public void getMapVehiclesJson(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="uid") Long uid,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth) throws Exception
	{
		MapVertex initialNode = es.getAftermathController().getMapData().get(uid);
		
		List<Transport> transports = es.getAftermathController().getTransporters();
		HashMap<Long, MapVertexLite> shortTransportList = new HashMap<Long, MapVertexLite>();
			
		for(Transport t : transports)
		{
			double[] bearing = initialNode.getBearing(t.getNode(), zoom);
			
			// Sort/filter Vehicles based on location!
			MapVertex vertex = new MapVertex((float)t.getNode().getLongitude(), (float)t.getNode().getLatitude(), t.getId());
			shortTransportList.put(t.getId(), vertex);
		}

		MapResponseDto responseDto = new MapResponseDto(initialNode.getLongitude(), initialNode.getLatitude(), zoom, 
				shortTransportList, new HashMap<Long, MapEdge>());
		JsonWriter jw = new JsonWriter(shortTransportList);

		String s = jw.toString();
		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		response.getWriter().print(s);
	}

	@GET
	@HandlerInfo(schema="/map/node/(uid)/json")
	public void getMapNodeJson(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="uid") Long uid,
			@QueryString(value="zoom", _default="18") Integer zoom, @QueryString(value="depth", _default="6") Integer depth, @QueryString(value="roadType", _default="") String filter) throws Exception
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
		DistinctOrderedSet masterEdgeList = buildMasterEdgeListFromDepth(focalPoint, masterDepthList, edges, depth, zoom);
		
		HashMap<Long, MapEdge> edgeData = new HashMap<Long, MapEdge>();
		masterEdgeList.reset(OrderType.FIFO);
		DistinctOrderedSet masterVertexList = new DistinctOrderedSet(200);
		
		while(masterEdgeList.hasNext())
		{
			Long edge = masterEdgeList.next();
			MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edge);
			
			if(filterSet.size() == 0 || filterSet.contains(String.valueOf(mapEdge.getMode().name()))) { } else
			{
				continue;
			}
			
			edgeData.put(edge, mapEdge);
			
			masterVertexList.add(mapEdge.getVertices()[0]);
			masterVertexList.add(mapEdge.getVertices()[1]);
		}
		
		HashMap<Long, MapVertexLite> vertexData = new HashMap<Long, MapVertexLite>();
		
		masterVertexList.reset(OrderType.FIFO);
		while(masterVertexList.hasNext())
		{
			Long vertexId = masterVertexList.next();
			vertexData.put(vertexId, es.getAftermathController().getMapData().get(vertexId));
		}
		
		MapVertex focusVertex = es.getAftermathController().getMapData().get(uid);
		MapResponseDto responseDto = new MapResponseDto(focusVertex.getLongitude(), focusVertex.getLatitude(), zoom, vertexData, edgeData);

		JsonWriter jw = new JsonWriter(responseDto);

		String s = jw.toString();
		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		response.getWriter().print(s);
	}
	
	@GET
	@HandlerInfo(schema="/map/edge/(edgeId)/weight/(weight)")
	public void getMapEdgeSetWeight(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="edgeId") Long edgeId,
			@QueryParam(value="weight") Integer weight) throws Exception
	{
		es.getAftermathController().getEdgeData().get(edgeId).addWeight(weight);
	}
	
	@GET
	@HandlerInfo(schema="/map/vehicles/json")
	public void getVehiclesJson(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		List<Transport> transports = es.getAftermathController().getTransporters();
		
		HashMap<Long, MapVertexLite> shortTransportList = new HashMap<Long, MapVertexLite>();
		for(Transport t : transports)
		{
			MapVertex vertex = new MapVertex((float)t.getNode().getLongitude(), (float)t.getNode().getLatitude(), t.getId());
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
	@HandlerInfo(schema="/map/vehicle/(vehicleId)/capacity/(capacity)")
	public void getVehicleSetWeight(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="vehicleId") Integer vehicleId,
			@QueryParam(value="capacity") Integer capacity) throws Exception
	{
		es.getAftermathController().getTransporters().get(vehicleId).setWeightChange(capacity);;
	}
	
	@GET
	@HandlerInfo(schema="/map/edge/(edgeId)")
	public void getMapEdgeInfo(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response,
			@QueryParam(value="edgeId") Long edgeId) throws Exception
	{
		HistogramBase weights = es.getAftermathController().getEdgeData().get(edgeId).getWeightInputs();
		JsonWriter jw = new JsonWriter(weights);
		
		response.setContentType("application/json");
		response.getWriter().print(jw.toString());
	}

	@POST
	@HandlerInfo(schema="/map/weight")
	public void postMapEdgesSetWeight(String target, String locale, Task parent, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long inId = parent.getTaskId();
		BufferedReader br = request.getReader();
		String inputString = br.readLine();
		
		String[] s = inputString.split("&");
		
		HashMap<Long, Float> weightInput = new HashMap<Long, Float>();
		
		String referer = baseRequest.getHeader("Referer");
		HtmlWriter writer = new HtmlWriter(2, es);
		
		long timeStamp = System.currentTimeMillis();
		
		// First cycle to renormalize

		float normalizationFactor = 0;
		int count = 0;
		ArrayList<EdgeWeightInfo> intArray = new ArrayList<EdgeWeightInfo>();
		float minNormalization = Float.MAX_VALUE;
		float maxNormalization = 0.0f;
		int confidenceCount = 0;
		float confidence = 1.0f;
		for(String item : s)
		{
			String[] s2 = item.split("=");
			if(s2.length != 2)
			{
				continue;
			}
			
			long edge = Long.valueOf(s2[0]);
			int weight = Math.round(Float.valueOf(s2[1]));
			
			intArray.add(new EdgeWeightInfo(edge, weight));
			
			MapEdge mEdge = es.getAftermathController().getEdgeData().get(edge);
			if(mEdge.getWeight() > 0 && weight > 0)
			{
				float normalizeWeight = mEdge.getWeight() / weight;
				
				if(minNormalization > normalizeWeight) minNormalization = normalizeWeight;
				if(maxNormalization < normalizeWeight) maxNormalization = normalizeWeight;
				normalizationFactor += normalizeWeight;
				
				count++;
				
				float normalizationDelta = maxNormalization - minNormalization;
				if(normalizationDelta > 20)
				{
					new Task(es.getAftermathController().getProfiler(), parent, "NormalizationDelta Exceeded 20!", null).end();
					normalizationDelta = 20;
				}
				
				confidence += 1 / (normalizationDelta + 1);
			}
			else
			{
				confidence += 0.25f;
			}
		}
		
		if(confidenceCount < 4) confidence /= 4;
		else confidence /= confidenceCount;
			
		float normalize = 1;
		if(count > 0) normalize = normalizationFactor / count;
		writer.table_Start();
		writer.tr_Start();
		writer.td("edge");
		writer.td("weight");
		writer.td("weightnormalize");
		writer.td("previousWeight");
		writer.td("normalize");
		writer.td("minNormalization");
		writer.td("maxNormalization");
		writer.td("confidence");
		writer.tr_End();
		
		for(EdgeWeightInfo item : intArray)
		{
			long edge = item.edge;
			int weight = item.weight;
			
			int previousWeight = es.getAftermathController().getEdgeData().get(edge).getWeight();
			int newWeight = (previousWeight==0)?(int)(previousWeight+(weight*normalize)):(int)((previousWeight+(weight*normalize))/2) ;

			weightInput.put(edge, Float.valueOf(newWeight));
			
			// TEMP
			es.getAftermathController().getEdgeData().get(edge).addWeightInput(inId, timeStamp, weight);
			HistogramBase weightInputs = es.getAftermathController().getEdgeData().get(edge).getWeightInputs();
			float finalWeight = weightInputs.calculateValue();
			
			es.getAftermathController().getEdgeData().get(edge).setWeight((int)(finalWeight), edge);
			
			writer.tr_Start();
			writer.td(String.valueOf(edge));
			writer.td(String.valueOf(weight));
			writer.td(String.valueOf(newWeight));
			writer.td(String.valueOf(previousWeight));
			writer.td(String.valueOf(normalize));
			writer.td(String.valueOf(minNormalization));
			writer.td(String.valueOf(maxNormalization));
			writer.td(String.valueOf(confidence));
			writer.tr_End();
		}
		writer.table_End();
		
		writer.text("<a href=\"" + referer + "\">Go Back</a><br/>");
		response.getWriter().print(writer.getString(locale));
	}
	
	private void drawVertices(HtmlWriter writer, MapVertex focalPoint, int zoom, int depth, String filter) throws Exception
	{
		SpatialIndex<Coordinates> diveQuadrant = es.getAftermathController().getSpatialIndex().dive(focalPoint);
		Coordinates[] bounds = Coordinates.getBounds(focalPoint, zoom, MapVertex.WIDTH, MapVertex.HEIGHT);
		List<Long> vertices =  es.getAftermathController().getSpatialIndex().getVerticesWithinBounds(bounds);
		
		for(Long l : vertices)
		{
			MapVertex position = es.getAftermathController().getMapData().get(l);
			double[] bearing = position.getBearing(focalPoint, zoom);

			int bearingX = (int)bearing[0]+MapVertex.WIDTH/2;
			int bearingY = (int)bearing[1]+MapVertex.HEIGHT/2;

			String arcColor = "#800000";
			if(position.getEdges().size() > 2)
			{
				arcColor = "#FF0000";
			}
			else if(position.getEdges().size() == 1)
			{
				arcColor = "#808000";
			}
			else if(position.getEdges().size() == 0)
			{
				arcColor = "#000000";
			}
			
			writer.drawArc(bearingX, bearingY, 7, 3, arcColor);
			writer.drawVertex("mapCanvas", 10, 1.0f, bearingX+5, bearingY+15, String.valueOf(position.getId()), arcColor);
		}
	}

	private void drawRoads(HtmlWriter writer, MapVertex focalPoint, int zoom, int depth, String filter) throws Exception
	{
		HashSet<String> filterSet = new HashSet<String>();
		StringTokenizer st = new StringTokenizer(filter, ",");
		while (st.hasMoreTokens())
		{
			filterSet.add(st.nextToken());
		}
		
		Coordinates[] bounds = Coordinates.getBounds(focalPoint, zoom, MapVertex.WIDTH, MapVertex.HEIGHT);
		/*
		DistinctOrderedSet masterEdgeList = new DistinctOrderedSet(200);
		masterEdgeList.add(es.getAftermathController().getSpatialIndex().getEdgesWithinBounds(bounds));
		*/
		
		HashMap<Long, Integer> masterDepthList = new HashMap<Long, Integer>(200);
		
		List<Long> edges = focalPoint.getEdges();
		DistinctOrderedSet masterEdgeList = buildMasterEdgeListFromDepth(focalPoint, masterDepthList, edges, depth, zoom);
		
		masterEdgeList.reset(OrderType.FIFO);

		// The MasterEdgeList doesn't sync with the JavaScript Version.  Why??
		while(masterEdgeList.hasNext())
		{
			Long edge = masterEdgeList.next();
			MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edge);
			long[] verticesList = mapEdge.getVertices();
			
			for(int i = 0; i < verticesList.length - 1; i++)
			{
				double[] focusBearing = es.getAftermathController().getMapData().get(verticesList[i]).getBearing(focalPoint, zoom);
				double[] drawBearing = es.getAftermathController().getMapData().get(verticesList[i+1]).getBearing(focalPoint, zoom);

				int startPointX = (int)focusBearing[0]+MapVertex.WIDTH/2;
				int startPointY = (int)focusBearing[1]+MapVertex.HEIGHT/2;

				int drawPointX = (int)drawBearing[0]+MapVertex.WIDTH/2;
				int drawPointY = (int)drawBearing[1]+MapVertex.HEIGHT/2;

				RoadTypes mode = mapEdge.getMode();
				int width = 1;
				
				if(filterSet.size() == 0 || filterSet.contains(String.valueOf(mode))) { } else
				{
					continue;
				}
				switch(String.valueOf(mode))
				{
				case "secondary":
					width = 4;
					break;
				case "secondary_link":
					width = 3;
					break;
				case "primary":
					width = 5;
					break;
				case "primary_link":
					width = 4;
					break;
				case "tertiary":
					width = 3;
					break;
				case "tertiary_link":
					width = 2;
					break;
				case "residential":
					width = 2;
					break;
				case "living_street":
					break;
				case "service":
					break;
				case "motorway":
					width = 5;
					break;
				case "motorway_link":
					width = 4;
					break;
				case "rail":
				case "subway":
				case "subway_entrance":
				case "station":
					width = 8;
					break;
				case "road":
					width = 1;
					break;
				case "trunk":
				case "trunk_link":
					width = 1;
					break;
				case "pedestrian":
				case "footway":
				case "path":
				case "steps":
					width = 1;
					break;
				case "unclassified":
					width = 1;
					break;
				case "null":
					width = 1;
					break;
				case "platform":
					width = 10;
					break;
				default:
					width = 1;
					break;
				}
				int mx = (int)((mapEdge.getConfidence() * 255));
				if(mx > 255)
				{
					mx = 255;
				}
				else if(mx < 0)
				{
					mx = 0;
				}
				String hx = Integer.toHexString(0x100 | mx).substring(1);
				int mw = (int)(mapEdge.getWeight() * 25);
				if(mw > 255)
				{
					mw = 255;
				}
				String hx2 = Integer.toHexString(0x100 | mw).substring(1);
				int mC = (int)(Math.abs(1-mapEdge.getConfidence())*mw*0.75);
				
				String hx3 = Integer.toHexString(0x100 | mC).substring(1);
				
				String color = "#" + hx2 + hx3 + "00";
				String color2 = "#00" + hx + "00";
				
				if(zoom >= 65535)
				{
					width *= zoom/65536;
				}
				
				// writer.drawCanvasLine("mapCanvas", width, color, startPointX, startPointY, drawPointX, drawPointY);
				writer.drawCanvasLineAsRect("mapCanvas", width, color, color2, startPointX, startPointY, drawPointX, drawPointY);
				// writer.drawVertex("mapCanvas", 2, (startPointX+drawPointX)/2, (startPointY+drawPointY)/2, String.valueOf(mapEdge.getId()), "#606060");
			}
		}
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
		while(diveQuadrant != null)
		{
			Coordinates[] bounds = diveQuadrant.getBounds();
			double[] minPoint = bounds[0].getBearing(focalPoint, zoom);
			double[] maxPoint = bounds[1].getBearing(focalPoint, zoom);

			int xMid = (int) (minPoint[0] + maxPoint[0])/2;
			int yMid = (int) (minPoint[1] + maxPoint[1])/2;

			writer.drawCanvasLine("mapCanvas", 2.0f, "#804080", 0.35f, (int) xMid+MapVertex.WIDTH/2, (int) minPoint[1]+MapVertex.HEIGHT/2,
					(int) xMid+MapVertex.WIDTH/2, (int) maxPoint[1]+MapVertex.HEIGHT/2);
			writer.drawCanvasLine("mapCanvas", 2.0f, "#804080", 0.35f, (int) minPoint[0]+MapVertex.WIDTH/2, (int) yMid+MapVertex.HEIGHT/2,
					(int) maxPoint[0]+MapVertex.WIDTH/2, (int) yMid+MapVertex.HEIGHT/2);
			diveQuadrant = diveQuadrant.getParent();
		}
	}

	private void drawDepot(HtmlWriter writer, Coordinates focalPoint, int zoom) throws Exception
	{
		List<Long> depotIds = es.getAftermathController().getSpatialIndexDepot().getNearestNodeRegion(focalPoint, 2);

		for(Long depotId : depotIds)
		{
			Depot depot = es.getAftermathController().getDepotData().get(depotId);

			double[] point = depot.getBearing(focalPoint, zoom);
			int edgeBearingX = (int)point[0]+MapVertex.WIDTH/2;
			int edgeBearingY = (int)point[1]+MapVertex.HEIGHT/2;
			
			writer.drawArc(edgeBearingX, edgeBearingY, 5, 5, "#00FFFF");
			writer.drawVertex("mapCanvas", 16, edgeBearingX+5, edgeBearingY+5, depot.getName(), "#0080FF");
		}
	}

	public void drawTransport(HtmlWriter writer, Transport transport, Coordinates focalPoint, int zoom, int depth) throws Exception
	{
		Coordinates position = transport.getPosition();
		double[] carBearing = position.getBearing(focalPoint, zoom);

		Coordinates destination = transport.getNode();
		double[] destinationBearing = destination.getBearing(focalPoint, zoom);

		int carBearingX = (int)carBearing[0]+MapVertex.WIDTH/2;
		int carBearingY = (int)carBearing[1]+MapVertex.HEIGHT/2;

		int destBearingX = (int)destinationBearing[0]+MapVertex.WIDTH/2;
		int destBearingY = (int)destinationBearing[1]+MapVertex.HEIGHT/2;

		MapVertex carPrevPosVertex = es.getAftermathController().getMapData().get(transport.getPreviousNode().getId());
		double[] carPrevBearing = carPrevPosVertex.getBearing(focalPoint, zoom);
		int carPrevBearingX = (int)carPrevBearing[0]+MapVertex.WIDTH/2;
		int carPrevBearingY = (int)carPrevBearing[1]+MapVertex.HEIGHT/2;

		writer.drawArc(carBearingX, carBearingY, 5, 3, "#00FF00");
		writer.drawVertex("mapCanvas", 16, 1.0f, carBearingX+10, carBearingY+5, String.valueOf(transport.getId()), "#00AA00");
		writer.drawCanvasLine("mapCanvas", 1, "#0000FF", 1.0f, destBearingX, destBearingY, carPrevBearingX, carPrevBearingY);
	}

	public void writeSummaryNode(HtmlWriter writer, String locale, MapVertex focalPoint, int zoom, int depth) throws Exception
	{
		LocaleBase localizer = es.getLocale(locale);
		
		writer.table_Start(null, null, "sortable");
		writer.tHead_Start();
		writer.tr_Start();
		writer.th(localizer.TH_EDGE_ID);
		writer.th("Vertices");
		writer.th("Mode");
		writer.th("Inputs");
		writer.th("Times");
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
			for(Long vertexId : mapEdge.getVertices())
			{
				writer.text(String.valueOf(vertexId) + "</br>");
			}
			writer.td_End();
			writer.td(mapEdge.getMode().toString());
			writer.td(mapEdge.getHistogramDataString());
			writer.td(mapEdge.getHistogramTimeDeltaString());
			writer.tr_End();
		}
		writer.tBody_End();
		writer.table_End();
	}
	
	public void writeSummaryNeighboringNodes(HtmlWriter writer, String locale, MapVertex focalPoint, int zoom, int depth) throws Exception
	{
		LocaleBase localizer = es.getLocale(locale);
		
		writer.table_Start(null, null, "sortable");
		writer.tHead_Start();
		writer.tr_Start();
		writer.th(localizer.TH_VERTEX_ID_1);
		writer.th(localizer.TH_VERTEX_ID_2);
		writer.th(localizer.TH_EDGE_ID);
		writer.th(localizer.TH_COORDINATES);
		writer.th(localizer.TH_TYPE);
		writer.th(localizer.TH_SCORE);
		writer.th(localizer.TH_WEIGHT);
		writer.th(localizer.TH_CONFIDENCE);
		writer.th(localizer.TH_SET_WEIGHT);
		writer.tr_End();
		writer.tHead_End();
		writer.form_Start("/aftermath/map/weight", "POST");

		HashMap<Long, Coordinates> mapVertices = es.getAftermathController().getSpatialIndex().dive(focalPoint).getIndex();
		Iterator<Entry<Long, Coordinates>> iter = mapVertices.entrySet().iterator();

		HashSet<Long> edgeList = new HashSet<Long>();
		while(iter.hasNext())
		{
			Map.Entry<Long, Coordinates> kvPair = (Map.Entry<Long, Coordinates>) iter.next();
			
			edgeList.addAll(es.getAftermathController().getMapData().get(kvPair.getKey()).getEdges());
		}
		
		writer.tBody_Start();
		for (Long e : edgeList)
		{
			MapEdge mapEdge = es.getAftermathController().getEdgeData().get(e);
			
			int weight = es.getAftermathController().getEdgeData().get(e).getWeight();
			int score = es.getAftermathController().getEdgeData().get(e).getScore();
			float confidence = es.getAftermathController().getEdgeData().get(e).getConfidence();
			
			writer.tr_Start();
			writer.td("<A href=\"/aftermath/map/node/" + mapEdge.getVertices()[0] + "?depth=" + String.valueOf(depth) + "&zoom=" + String.valueOf(zoom) + "\">" + mapEdge.getVertices()[0] + "</A>");
			writer.td("<A href=\"/aftermath/map/node/" + mapEdge.getVertices()[1] + "?depth=" + String.valueOf(depth) + "&zoom=" + String.valueOf(zoom) + "\">" + mapEdge.getVertices()[1] + "</A>");
			writer.td(String.valueOf(mapEdge.getId()));
			writer.td(mapEdge.toString());
			writer.td(mapEdge.getMode().name());
			writer.td(String.valueOf(score));
			writer.td(String.valueOf(weight));
			writer.td(String.valueOf(confidence));
			writer.td("<input type=\"text\" name=\"" + e + "\" value=\"\">");
			writer.tr_End();
		}
		writer.tBody_End();

		writer.tFoot_Start();
		writer.tr_Start();
		writer.td("<input type=\"submit\" value=\"" + localizer.BTN_SEND_WEIGHTS + "\"/>", "", "", "text-align:center", 9);
		writer.tr_End();
		writer.tFoot_End();
		writer.form_End();
		writer.table_End();
	}

	public void writeSummaryVehicles(HtmlWriter writer, String locale, Coordinates focalPoint, int zoom, int depth) throws Exception
	{
		LocaleBase localizer = es.getLocale(locale);
		
		writer.table_Start(null, null, "sortable");
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
			writer.td("<A href=\"/aftermath/map/vehicle/" + i + "?depth=" + depth + "&zoom=" + zoom + "\">" + t.getId() + "</A>");
			writer.td("<A href=\"/aftermath/map/coord/" + coordinates.getLongitude() + "/" + coordinates.getLatitude() + "\">" + coordinates.toString() + "</A>");
			writer.td(String.valueOf(t.getTicks()));
			try
			{
				writer.td(t.getEdge().getMode().name());
				writer.td(String.valueOf(t.getEdge().getScore()));
				writer.td(t.getDestination().toString());
				
			}
			catch(Exception e)
			{
				writer.td("Null");
				writer.td("");
				writer.td("");
			}
			writer.tr_End();
		}
		writer.table_End();
	}

	public void renderMap(HtmlWriter writer, MapVertex node, int zoom)
	{
		// if(true) return;
		writer.tr_Start();
		writer.td_Start();
		writer.text("<div id=\"googleMap\" style=\"width:" + MapVertex.WIDTH +"px;height:" + MapVertex.HEIGHT + "px;position:absolute; z-index:-5\"></div>");
		String scriptstr = "<script>" +
		"function myMap() {" +
		"var mapProp= { center:new google.maps.LatLng(" + node.getLatitude() + "," + node.getLongitude() + "), zoom:" + zoom + " };" +
		"var map=new google.maps.Map(document.getElementById(\"googleMap\"),mapProp);}" +
		"var drawingManager = new google.maps.drawing.DrawingManager();" +
		"drawingManager.setMap(map);" +
		"</script>";
		writer.text(scriptstr);
		writer.text("<script src=\"https://maps.googleapis.com/maps/api/js?key=" + AftermathServer.GOOGLE_MAP_API_KEY + "&callback=myMap&libraries=drawing\"></script>");
		writer.td_End();
		writer.tr_End();
	}
	
	private DistinctOrderedSet buildMasterEdgeListFromBounds(MapVertex focalPoint, int depth, int zoom)
	{
		return null;
	}
	
	private DistinctOrderedSet buildMasterEdgeListFromDepth(MapVertex focalPoint,  HashMap<Long, Integer> masterDepthList, List<Long> edges, int depth, int zoom) throws Exception
	{
		DistinctOrderedSet masterEdgeList = new DistinctOrderedSet(200);
		DistinctOrderedSet masterVertexList = new DistinctOrderedSet(200);
		for(int i = 0; i < depth; i++)
		{
			masterEdgeList.add(edges);
			int vertexListSizeBefore = masterVertexList.size();
			for(Long edge : edges)
			{
				MapEdge mapEdge = es.getAftermathController().getEdgeData().get(edge);
				for(Long vertexId : mapEdge.getVertices())
				{
					MapVertex vertex = es.getAftermathController().getMapData().get(vertexId);
					double[] bearing = focalPoint.getBearing(vertex, zoom);
					if(bearing[0] > -MapVertex.WIDTH && bearing[0] < MapVertex.WIDTH && bearing[1] > -MapVertex.HEIGHT && bearing[1] < MapVertex.HEIGHT)
					{
						while(masterVertexList.add(vertex.getId()) == true && vertex.getEdges().size() == 2)
						{
							masterDepthList.put(vertex.getId(), i);
							for(Long cascadeEdge : vertex.getEdges())
							{
								masterEdgeList.add(cascadeEdge);
								if(es.getAftermathController().getEdgeData().get(cascadeEdge).getScore() > maxScore)
								{
									maxScore = es.getAftermathController().getEdgeData().get(cascadeEdge).getScore();
								}
								if(cascadeEdge == edge) continue;
								for(Long cascadeVertexId : es.getAftermathController().getEdgeData().get(cascadeEdge).getVertices())
								{
									if(cascadeVertexId == vertex.getId()) continue;
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
			while(masterVertexList.hasNext())
			{
				long vertexId = masterVertexList.next();
				edges.addAll(es.getAftermathController().getMapData().get(vertexId).getEdges());
			}
		}
		return masterEdgeList;	
	}
}