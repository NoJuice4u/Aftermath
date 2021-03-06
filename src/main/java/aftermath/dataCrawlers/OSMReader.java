package main.java.aftermath.dataCrawlers;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import main.java.aftermath.engine.Depot;
import main.java.aftermath.server.AftermathServer;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.dto.MapEdge.RoadTypes;
import main.java.encephalon.logger.Logger;
import main.java.encephalon.dto.MapVertex;
import main.java.encephalon.profiler.CountMeter;
import main.java.encephalon.profiler.Profiler;
import main.java.encephalon.profiler.Task;
import main.java.encephalon.spatialIndex.SpatialIndex;

public class OSMReader
{
    private static final String MAP_RESOURCE = "aftermath.map.resource.mapfile";
    
    private static final AftermathServer     as = AftermathServer.getInstance();
    
    private static final int MAPDATA_INIT_SIZE = as.getProperty("aftermath.mapdata.initialization.size", 300000, "Hashmap initialization size for map data.");
    private static final int EDGEDATA_INIT_SIZE = as.getProperty("aftermath.edgedata.initialization.size", 300000, "Hashmap initialization size for edge data.");
    private static final int LATLON_MAGIC_NUMBER = -1000;
    
    private final Profiler            profiler;
    private HashMap<Long, MapVertex>  mapData;
    private HashMap<Long, CountMeter> mapDataCounters;
    private HashMap<Long, MapEdge>    edgeData;
    private SpatialIndex<MapVertex>   spatialIndex;
    private SpatialIndex<Depot>       spatialIndexDepot;

    private CountMeter OSMDataVertexCountMeter = new CountMeter();
    private CountMeter OSMDataEdgeCountMeter   = new CountMeter();

    public static void main(String[] args) throws Exception
    {
        Logger.Log("IDIOT", "You launched the wrong instance, idiot.");
    }

    public OSMReader(HashMap<Long, MapVertex> mapData, HashMap<Long, MapEdge> edgeData,
            SpatialIndex<MapVertex> spatialIndex, SpatialIndex<Depot> spatialIndexDepot) throws Exception
    {
        this.mapData = mapData;
        this.mapDataCounters = new HashMap<Long, CountMeter>();
        this.edgeData = edgeData;
        this.spatialIndex = spatialIndex;
        this.spatialIndexDepot = spatialIndexDepot;
        this.profiler = as.getProfiler();

        as.getCountMeters().put("OSMData.Vertices", OSMDataVertexCountMeter);
        as.getCountMeters().put("OSMData.Edges", OSMDataEdgeCountMeter);

        read();
    }

    public OSMReader() throws Exception
    {
        this.mapData = new HashMap<Long, MapVertex>(MAPDATA_INIT_SIZE);
        this.mapDataCounters = new HashMap<Long, CountMeter>();
        this.edgeData = new HashMap<Long, MapEdge>(EDGEDATA_INIT_SIZE);
        this.spatialIndex = new SpatialIndex<MapVertex>("DUMMY", SpatialIndex.LON_MIN, SpatialIndex.LON_MAX, SpatialIndex.LAT_MIN, SpatialIndex.LAT_MAX, null);
        this.spatialIndexDepot = new SpatialIndex<Depot>("DUMMY", SpatialIndex.LON_MIN, SpatialIndex.LON_MAX, SpatialIndex.LAT_MIN, SpatialIndex.LAT_MAX, null);
        this.profiler = null;

        read();
    }

    private void read() throws Exception
    {
        // TODO: Shouldn't have a null case here.  Should be a mandatory property instead!
        File file = new File(as.getProperty(MAP_RESOURCE));
        FileInputStream fileStream = new FileInputStream(file);
        
        // Use Stream object here so we don't load the entire file in memory.
        Task osmProcessingTask = new Task(this.profiler, null, "Parse OSM File", null);
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader xmlStr = inputFactory.createXMLStreamReader(fileStream);
        
        int depth = 0;
        long edgeId = 0;
        List<Long> wayList = new ArrayList<Long>();
        HashSet<Long> edgeListForReduction = new HashSet<Long>(EDGEDATA_INIT_SIZE);
        boolean wayStart = false;
        RoadTypes mode = RoadTypes.unknown;
        String name = null;
        boolean isStation = false;
        float nodeLat = LATLON_MAGIC_NUMBER;
        float nodeLon = LATLON_MAGIC_NUMBER;
        
        while(xmlStr.hasNext())
        {
            xmlStr.next();
            int Etype = xmlStr.getEventType();
            
            switch(Etype)
            {
                case XMLStreamReader.START_ELEMENT:
                    depth++;
                    QName eName = xmlStr.getName();
                    if(eName.getLocalPart() == "node")
                    {
                        int attributes = xmlStr.getAttributeCount();
                        long nodeId = -1;
                        nodeLat = LATLON_MAGIC_NUMBER;
                        nodeLon = LATLON_MAGIC_NUMBER;
                        for(int i = 0; i < attributes; i++)
                        {
                            // Figure out the attribute name/value positions as we don't have a guarantee the order is consistent
                            if(xmlStr.getAttributeName(i).toString() == "id")
                            {
                                nodeId = Long.valueOf(xmlStr.getAttributeValue(i));
                            }
                            else if(xmlStr.getAttributeName(i).toString() == "lat")
                            {
                                nodeLat = Float.valueOf(xmlStr.getAttributeValue(i));
                            }
                            else if(xmlStr.getAttributeName(i).toString() == "lon")
                            {
                                nodeLon = Float.valueOf(xmlStr.getAttributeValue(i));
                            }
                        }
                        if(nodeId == -1 || nodeLat == LATLON_MAGIC_NUMBER || nodeLon == LATLON_MAGIC_NUMBER)
                        {
                            throw new Exception("Incomplete Data? Got: Id: " + nodeId + ", Latitude: " + nodeLat + ", Longitude: " + nodeLon);
                        }
                        mapData.put(nodeId, new MapVertex(nodeLon, nodeLat, nodeId));
                        mapDataCounters.put(nodeId, new CountMeter());
                        OSMDataVertexCountMeter.increment();
                    }
                    else if(eName.getLocalPart() == "way")
                    {
                        wayStart = true;
                    }
                    else if(eName.getLocalPart() == "nd" && wayStart == true)
                    {
                        Long refId = -1L;
                        
                        int attributes = xmlStr.getAttributeCount();
                        for(int i = 0; i < attributes; i++)
                        {
                            // Figure out the attribute name/value positions as we don't have a guarantee the order is consistent
                            if(xmlStr.getAttributeName(i).toString() == "ref")
                            {
                                refId = Long.valueOf(xmlStr.getAttributeValue(i));
                                wayList.add(refId);
                            }
                        }
                    }
                    else if(eName.getLocalPart() == "tag")
                    {
                        int attributes = xmlStr.getAttributeCount();
                        String k = null, v = null;
                        for(int i = 0; i < attributes; i++)
                        {
                            // Figure out the attribute name/value positions as we don't have a guarantee the order is consistent
                            if(xmlStr.getAttributeName(i).toString() == "k")
                            {
                                k = xmlStr.getAttributeValue(i);
                            }
                            else if(xmlStr.getAttributeName(i).toString() == "v")
                            {
                                v = xmlStr.getAttributeValue(i);
                            }
                        }
                        
                        if(k == null || v == null)
                        {
                            throw new Exception("K[" + String.valueOf(k==null) + "] or V[" + String.valueOf(v==null) + "] evaluated to Null!");
                        }
                        
                        if(k.equals("highway") || k.equals("railway") || k.equals("way"))
                        {
                            try
                            {
                                if(v != "switch") // Because I can't put "switch" as enum because it's a java keyword
                                {
                                    mode = MapEdge.RoadTypes.valueOf(v);
                                }
                            }
                            catch (IllegalArgumentException e)
                            {
                                Task.entry(this.profiler, osmProcessingTask, "NO ENUM FOR WAYTAG: " + v, null);
                                continue;
                            }
                        }
                        else if(k.equals("name"))
                        {
                            name = v;
                        }
                        else if(v.equals("station"))
                        {
                            isStation = true;
                        }
                        else
                        {
                            // For Debugging only
                            // Task.entry(this.profiler, osmProcessingTask, "\"k\" Unknown: [" + k + "]", null);
                        }
                        
                        if(name != null && isStation == true)
                        {
                            Depot depot = new Depot(name, 0, nodeLon, nodeLat);
                            spatialIndexDepot.add(depot.getId(), depot);
                            
                            name = null;
                            isStation = false;
                        }
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    depth--;
                    QName eeName = xmlStr.getName();
                    
                    if(eeName.getLocalPart() == "way")
                    {
                        long previousNode = 0L;
                        long currentNode = 0L;
                        for (long l : wayList)
                        {
                            previousNode = currentNode;
                            currentNode = l;
                            if (previousNode != 0L)
                            {
                                MapEdge mapEdge = new MapEdge(edgeId, mapData.get(previousNode), mapData.get(currentNode),
                                        mode);
                                edgeListForReduction.add(edgeId);
                                edgeData.put(edgeId, mapEdge);
                                OSMDataEdgeCountMeter.increment();
                                mapData.get(previousNode).addEdge(edgeId);
                                mapData.get(currentNode).addEdge(edgeId);

                                mapDataCounters.get(previousNode).increment();
                                mapDataCounters.get(currentNode).increment();

                                edgeId++;
                            }
                        }
                        
                        wayStart = false;
                        wayList.clear();
                        // Build the way object.
                    }
                    break;
                default:
                    break;
            }
        }

        osmProcessingTask.end();

        for (long l = 0; l < edgeId; l++)
        {
            MapEdge initialEdge = edgeData.get(l);
            List<Long> edgeList = new ArrayList<Long>();

            if (initialEdge == null) continue;

            edgeList.add(initialEdge.getId());
            RoadTypes type = initialEdge.getMode();
            long[] vertices = initialEdge.getVertices();

            long[] endPoints = new long[]
            {
                    -1, -1
            };

            for (int i = 0; i < endPoints.length; i++)
            {
                MapEdge e = initialEdge;
                for (MapVertex vertex = mapData.get(vertices[i]); true;)
                {
                    if (vertex.getEdges().size() != 2 || (vertex.getEdges().size() == 2 && e.getMode() != type)) // This
                                                                                                                 // breaks
                                                                                                                 // things
                                                                                                                 // for
                                                                                                                 // some
                                                                                                                 // reason...
                    {
                        endPoints[i] = vertex.getId();
                        break;
                    }
                    e = edgeData.get(vertex.getOtherEdge(e.getId()));
                    edgeList.add(e.getId());
                    vertex = mapData.get(e.getOtherVertex(vertex.getId()));
                    if (initialEdge.getId() == e.getId())
                    {
                        break;
                    }
                }
            }

        }

        {
            // Prune and reconnect roads
            Task t = new Task(this.profiler, osmProcessingTask, "Parse through road iterations", null);
            try
            {
                Set<Long> setEntry = new TreeSet<Long>(mapDataCounters.keySet());
                Iterator<Long> meter = setEntry.iterator();
                while (true)
                {
                    if (meter.hasNext())
                    {
                        Long l = meter.next();
                        CountMeter cm = mapDataCounters.get(l);
                        if(cm.getCount() == 0)
                        {
                            new Task(profiler, osmProcessingTask, "Remove vertex with 0 edges", null).end();
                            mapData.remove(l);
                            as.getCountMeters().get("OSMData.Vertices").decrement();
                        }
                        else
                        {
                            String meterName = "Vertex.With." + String.format("%03d", cm.getCount()) + ".Edges";
                            if(as.getCountMeters().get(meterName) == null)
                            {
                                as.getCountMeters().put(meterName, new CountMeter());
                            }
                            else
                            {
                                as.getCountMeters().get(meterName).increment();
                            }
                        }
                    }
                    else
                    {
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                t.insertException(e);
                e.printStackTrace();
            }
            finally
            {
                t.end();
            }
        }
        
        {
            // Build the Spatial Index AFTER cleanup. Otherwise we will get null references.
            Task buildSpatialIndexTask = new Task(this.profiler, osmProcessingTask, "Build Spatial Index", null);
            try
            {
                Iterator<Entry<Long, MapVertex>> iter = mapData.entrySet().iterator();
    
                while (iter.hasNext())
                {
                    Entry<Long, MapVertex> vEntry = iter.next();
                    try
                    {
                        spatialIndex.add(vEntry.getKey(), vEntry.getValue());
                    }
                    catch (StackOverflowError e)
                    {
                        new Task(profiler, buildSpatialIndexTask, "Stack Oveflow on Edge: " + vEntry.getKey(), null).end();
                        System.err.println("Stack Oveflow on Edge: " + vEntry.getKey());
                    }
                }
            }
            finally
            {
                buildSpatialIndexTask.end();
            }
        }
    }
}
