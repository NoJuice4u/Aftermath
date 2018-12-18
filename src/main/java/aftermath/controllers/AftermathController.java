package main.java.aftermath.controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import main.java.encephalon.server.EncephalonThreadPoolExecutor;
import main.java.encephalon.spatialIndex.SpatialIndex;

public class AftermathController
{
    private static AftermathServer es = AftermathServer.getInstance();
    private Runnable engine;
    private HashMap<Long, MapVertex> mapData;
    private HashMap<Long, MapEdge> edgeData;
    private HashMap<Long, Depot> depotData;
    private HashMap<String, SpatialIndex<?>> spatialIndexMap;

    EncephalonThreadPoolExecutor executor = new EncephalonThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(100));

    public final static String SPATIALINDEX_MAPDATA_DEPTH = "SpatialIndex.MapData.Depth";
    public final static String SPATIALINDEX_DEPOT_DEPTH = "SpatialIndex.Depot.Depth";

    static
    {
        es.getCountMeters().put(SPATIALINDEX_MAPDATA_DEPTH, new CountMeter());
        es.getCountMeters().put(SPATIALINDEX_DEPOT_DEPTH, new CountMeter());
    }

    public AftermathController()
    {
        this.mapData = new HashMap<Long, MapVertex>();
        this.edgeData = new HashMap<Long, MapEdge>();
        this.depotData = new HashMap<Long, Depot>();
        this.spatialIndexMap = new HashMap<String, SpatialIndex<?>>();
        this.spatialIndexMap.put("nodesMap",
                new SpatialIndex<MapVertex>(es.getCountMeters().get(SPATIALINDEX_MAPDATA_DEPTH), SpatialIndex.LON_MIN,
                        SpatialIndex.LON_MAX, SpatialIndex.LAT_MIN, SpatialIndex.LAT_MAX, null));
        this.spatialIndexMap.put("depotMap", new SpatialIndex<Depot>(es.getCountMeters().get(SPATIALINDEX_DEPOT_DEPTH),
                SpatialIndex.LON_MIN, SpatialIndex.LON_MAX, SpatialIndex.LAT_MIN, SpatialIndex.LAT_MAX, null));
    }

    public void run()
    {
        try
        {
            engine = new AftermathEngine(this);
            Future<?> future = executor.submit(engine);
        } finally
        {
            executor.shutdown();
        }
    }

    public HashMap<Long, MapVertex> getMapData()
    {
        return mapData;
    }

    public HashMap<Long, MapEdge> getEdgeData()
    {
        return edgeData;
    }

    public HashMap<Long, Depot> getDepotData()
    {
        return depotData;
    }

    public SpatialIndex<?> getSpatialIndex(String s)
    {
        return spatialIndexMap.get(s);
    }

    public Set<String> getSpatialIndexKeys()
    {
        return spatialIndexMap.keySet();
    }

    @SuppressWarnings("unchecked")
    public SpatialIndex<MapVertex> getSpatialIndex()
    {
        return (SpatialIndex<MapVertex>) spatialIndexMap.get("nodesMap");
    }

    @SuppressWarnings("unchecked")
    public SpatialIndex<Depot> getSpatialIndexDepot()
    {
        return (SpatialIndex<Depot>) spatialIndexMap.get("depotMap");
    }

    public Profiler getProfiler()
    {
        return es.getProfiler();
    }

    public List<Transport> getTransporters()
    {
        return ((AftermathEngine) engine).getTransporters();
    }

    public HashSet<Long> getConnectedVertices(MapVertex vertex) throws Exception
    {
        HashSet<Long> vertexSet = new HashSet<Long>();

        for (Long edge : vertex.getEdges())
        {
            vertexSet.add(edgeData.get(edge).getVertices()[0]);
            vertexSet.add(edgeData.get(edge).getVertices()[1]);
        }

        return vertexSet;
    }
}
