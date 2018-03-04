package main.java.aftermath.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import main.java.aftermath.engine.AftermathEngine;
import main.java.aftermath.engine.Depot;
import main.java.aftermath.server.AftermathServer;
import main.java.aftermath.vehicles.Transport;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.dto.MapVertex;
import main.java.encephalon.profiler.CountMeter;
import main.java.encephalon.profiler.Profiler;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.server.EncephalonThreadPoolExecutor;
import main.java.encephalon.spatialIndex.SpatialIndex;

public class AftermathController {
	private AftermathServer es;
	private Runnable engine;
	private HashMap<Long, MapVertex> mapData;
	private HashMap<Long, MapEdge> edgeData;
	private SpatialIndex<MapVertex> spatialIndex;
	private SpatialIndex<Depot> spatialIndexDepot;

	EncephalonThreadPoolExecutor executor = new EncephalonThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(100));
	
	private CountMeter spatialIndexMeter = new CountMeter();
	private CountMeter spatialIndexDepotMeter = new CountMeter();

	public AftermathController() {
		this.es = AftermathServer.getInstance();

		this.mapData = new HashMap<Long, MapVertex>();
		this.edgeData = new HashMap<Long, MapEdge>();
		this.spatialIndex = new SpatialIndex<MapVertex>(spatialIndexMeter, -180, 180, -90, 90, null);
		this.spatialIndexDepot = new SpatialIndex<Depot>(spatialIndexDepotMeter, -180, 180, -90, 90, null);
		
		es.getCountMeters().put("SpatialIndex.MapData.Depth", spatialIndexMeter);
		es.getCountMeters().put("SpatialIndex.Depot.Depth", spatialIndexDepotMeter);
	}

	public void run() {
		try {
			engine = new AftermathEngine(this);
			Future future = executor.submit(engine);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			executor.shutdown();
		}
	}

	public HashMap<Long, MapVertex> getMapData() {
		return mapData;
	}

	public HashMap<Long, MapEdge> getEdgeData() {
		return edgeData;
	}

	public SpatialIndex<MapVertex> getSpatialIndex() {
		return spatialIndex;
	}

	public SpatialIndex<Depot> getSpatialIndexDepot() {
		return spatialIndexDepot;
	}

	public Profiler getProfiler() {
		return es.getProfiler();
	}

	public List<Transport> getTransporters() {
		return ((AftermathEngine) engine).getTransporters();
	}

	public HashSet<Long> getConnectedVertices(MapVertex vertex) throws Exception {
		HashSet<Long> vertexSet = new HashSet<Long>();

		for (Long edge : vertex.getEdges()) {
			vertexSet.add(edgeData.get(edge).getVertices()[0]);
			vertexSet.add(edgeData.get(edge).getVertices()[1]);
		}

		return vertexSet;
	}
}
