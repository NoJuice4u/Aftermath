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
    public final String SPATIALINDEX_DEPOTMAP_NAME = "depotMap";
    public final String SPATIALINDEX_MAPDATA_NAME  = "nodesMap";

    private AftermathServer                  es;
    private Runnable                         engine;
    private HashMap<Long, MapVertex>         mapData;
    private HashMap<Long, MapEdge>           edgeData;
    private HashMap<String, SpatialIndex<?>> spatialIndexMap;

    EncephalonThreadPoolExecutor executor = new EncephalonThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(100));

    public AftermathController()
    {
        this.es = AftermathServer.getInstance();

        this.mapData = new HashMap<Long, MapVertex>();
        this.edgeData = new HashMap<Long, MapEdge>();
        this.spatialIndexMap = new HashMap<String, SpatialIndex<?>>();

        this.spatialIndexMap.put(SPATIALINDEX_DEPOTMAP_NAME, new SpatialIndex<MapVertex>("SpatialIndex.Vertices",
                SpatialIndex.LON_MIN, SpatialIndex.LON_MAX, SpatialIndex.LAT_MIN, SpatialIndex.LAT_MAX, null));
        this.spatialIndexMap.put(SPATIALINDEX_MAPDATA_NAME, new SpatialIndex<Depot>("SpatialIndex.Depots",
                SpatialIndex.LON_MIN, SpatialIndex.LON_MAX, SpatialIndex.LAT_MIN, SpatialIndex.LAT_MAX, null));
    }

    public void run()
    {
        try
        {
            engine = new AftermathEngine(this);
            executor.submit(engine);
        }
        finally
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
        return Depot.getDepotList();
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
        return (SpatialIndex<MapVertex>) spatialIndexMap.get(SPATIALINDEX_MAPDATA_NAME);
    }

    @SuppressWarnings("unchecked")
    public SpatialIndex<Depot> getSpatialIndexDepot()
    {
        return (SpatialIndex<Depot>) spatialIndexMap.get(SPATIALINDEX_DEPOTMAP_NAME);
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
