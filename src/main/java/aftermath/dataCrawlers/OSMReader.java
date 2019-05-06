package main.java.aftermath.dataCrawlers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.eclipse.jetty.xml.XmlParser;
import org.eclipse.jetty.xml.XmlParser.Attribute;
import org.eclipse.jetty.xml.XmlParser.Node;

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
    private static final String MAP_RESOURCE = "aftermath.map.resource";

    private final AftermathServer     es;
    private final Profiler            profiler;
    private HashMap<Long, MapVertex>  mapData;
    private HashMap<Long, CountMeter> mapDataCounters;
    private HashMap<Long, MapEdge>    edgeData;
    private SpatialIndex<MapVertex>   spatialIndex;
    private SpatialIndex<Depot>       spatialIndexDepot;
    private float                     progress = 0.0f;

    private CountMeter OSMDataVertexCountMeter = new CountMeter();
    private CountMeter OSMDataEdgeCountMeter   = new CountMeter();
    private CountMeter spatialIndexMeter       = new CountMeter();
    private CountMeter spatialIndexDepotMeter  = new CountMeter();

    public static void main(String[] args) throws Exception
    {
        Logger.Log("IDIOT", "You launched the wrong instance, idiot.");
    }

    public OSMReader(HashMap<Long, MapVertex> mapData, HashMap<Long, MapEdge> edgeData,
            SpatialIndex<MapVertex> spatialIndex, SpatialIndex<Depot> spatialIndexDepot) throws Exception
    {
        this.es = AftermathServer.getInstance();
        this.mapData = mapData;
        this.mapDataCounters = new HashMap<Long, CountMeter>();
        this.edgeData = edgeData;
        this.spatialIndex = spatialIndex;
        this.spatialIndexDepot = spatialIndexDepot;
        this.profiler = es.getProfiler();

        es.getCountMeters().put("OSMData.Vertices", OSMDataVertexCountMeter);
        es.getCountMeters().put("OSMData.Edges", OSMDataEdgeCountMeter);

        read();
    }

    public OSMReader() throws Exception
    {
        this.es = null;
        this.mapData = new HashMap<Long, MapVertex>(300000);
        this.mapDataCounters = new HashMap<Long, CountMeter>();
        this.edgeData = new HashMap<Long, MapEdge>(300000);
        this.spatialIndex = new SpatialIndex<MapVertex>(spatialIndexMeter, -180, 180, -90, 90, null);
        this.spatialIndexDepot = new SpatialIndex<Depot>(spatialIndexDepotMeter, -180, 180, -90, 90, null);
        this.profiler = null;

        read();
    }

    private void read() throws Exception
    {
        // TODO: Shouldn't have a null case here.  Should be a mandatory property instead!
        File file = new File(es.getProperty(MAP_RESOURCE, null));

        XmlParser xP = new XmlParser();
        Task xmlParseTask = new Task(this.profiler, null, "Parse into XML", null);
        Node data = xP.parse(file);
        xmlParseTask.end();

        long edgeId = 0L;

        Task osmProcessingTask = new Task(this.profiler, null, "Parse OSM File", null);
        Iterator<Object> nodeIterator = data.iterator();

        // Go through all nodes, and find the vertices and edges to build into the
        // Spatial Index
        HashSet<Long> edgeListForReduction = new HashSet<Long>(300000);
        while (nodeIterator.hasNext())
        {
            Node nD = null;
            Object obj = nodeIterator.next();
            try
            {
                nD = (Node) obj;
            }
            catch (Exception e)
            {
                new Task(this.profiler, "", osmProcessingTask,
                        ((String) obj).replace("\n", "%nl").trim() + ":" + e.getMessage(), e, null).end();
                continue;
            }

            String attributes = processAttributes(nD, osmProcessingTask);
            Task tagTypeTask = new Task(this.profiler, osmProcessingTask,
                    "Encountered Node [" + nD.getTag() + "] with attributes [" + attributes + "]", null);
            switch (nD.getTag())
            {
                case "node":
                    float lat = Float.valueOf(nD.getAttribute("lat"));
                    float lon = Float.valueOf(nD.getAttribute("lon"));
                    long id = Long.valueOf(nD.getAttribute("id"));
                    mapData.put(id, new MapVertex(lon, lat, id));
                    mapDataCounters.put(id, new CountMeter());
                    OSMDataVertexCountMeter.increment();
                    Iterator<Object> ndIterator = nD.iterator();
                    String preName = null;
                    while (ndIterator.hasNext())
                    {
                        Node ndNode = null;
                        Object ndIteratorNext = ndIterator.next();

                        try
                        {
                            ndNode = (Node) ndIteratorNext;
                        }
                        catch (Exception e)
                        {
                            new Task(this.profiler, "", osmProcessingTask,
                                    ((String) ndIteratorNext).replace("\n", "%nl").trim() + ":" + e.getMessage(), e,
                                    null).end();
                            continue;
                        }

                        String ndAttributes = processAttributes(ndNode, osmProcessingTask);
                        Task.entry(this.profiler, tagTypeTask,
                                "Encountered Node [" + nD.getTag() + "] with attributes [" + ndAttributes + "]", null);
                        switch (ndNode.getTag())
                        {
                            case "tag":
                                if (ndNode.getAttribute("k").toLowerCase().indexOf("name") >= 0)
                                {
                                    preName = ndNode.getAttribute("v");
                                }
                                if (ndNode.getAttribute("v").toLowerCase().indexOf("shelter") >= 0
                                        || ndNode.getAttribute("k").toLowerCase().indexOf("shelter") >= 0)
                                {
                                    if (preName == null)
                                    {
                                        preName = ndNode.getAttribute("v");
                                    }
                                    Depot d = new Depot(preName, 100, lon, lat);

                                    es.getAftermathController().getDepotData().put(d.getId(), d);
                                    es.getAftermathController().getSpatialIndexDepot().add(d.getId(), d);
                                }
                                break;
                            default:
                                Task.entry(this.profiler, tagTypeTask, "MISSED CHILDNODETAG: [" + nD.getTag() + "]",
                                        null);
                        }

                        if (Math.random() < 0.02)
                        {
                            Depot d = new Depot("Random Depot", 100, lon, lat);

                            es.getAftermathController().getDepotData().put(d.getId(), d);
                            es.getAftermathController().getSpatialIndexDepot().add(d.getId(), d);
                        }
                    }
                    break;
                case "way":
                    List<Long> edgeRefNodes = new ArrayList<Long>();
                    RoadTypes mode = RoadTypes.unknown;
                    Iterator<Object> wayIterator = nD.iterator();
                    while (wayIterator.hasNext())
                    {
                        Node wayNode = null;

                        Object wayIteratorNext = wayIterator.next();

                        // Cast to Node to parse. If we can't cast, it's probably not an XML node.
                        try
                        {
                            wayNode = (Node) wayIteratorNext;
                        }
                        catch (Exception e)
                        {
                            new Task(this.profiler, "", osmProcessingTask,
                                    ((String) wayIteratorNext).replace("\n", "%nl").trim() + ":" + e.getMessage(), e,
                                    null).end();
                            continue;
                        }

                        String ndAttributes = processAttributes(wayNode, osmProcessingTask);
                        Task.entry(this.profiler, tagTypeTask,
                                "Encountered Node [" + nD.getTag() + "] with attributes [" + ndAttributes + "]", null);
                        switch (wayNode.getTag())
                        {
                            case "nd":
                                edgeRefNodes.add(Long.valueOf(wayNode.getAttribute("ref")));
                                break;
                            case "tag":
                                if (wayNode.getAttribute("k").equals("highway")
                                        || wayNode.getAttribute("k").equals("railway")
                                        || wayNode.getAttribute("k").equals("way"))
                                {
                                    try
                                    {
                                        mode = MapEdge.RoadTypes.valueOf(wayNode.getAttribute("v"));
                                    }
                                    catch (IllegalArgumentException e)
                                    {
                                        Task.entry(this.profiler, tagTypeTask,
                                                "##WARNING## NO ENUM FOR WAYTAG: " + wayNode.getAttribute("v"), null);
                                        continue;
                                    }
                                }
                                else
                                {
                                    Task.entry(this.profiler, tagTypeTask,
                                            "\"k\" Unknown: [" + wayNode.getAttribute("k") + "]", null);
                                }
                                break;
                            default:
                                Task.entry(this.profiler, tagTypeTask, "MISSED WAYTAG: [" + nD.getTag() + "]", null);
                                break;
                        }
                    }

                    long previousNode = 0L;
                    long currentNode = 0L;
                    for (long l : edgeRefNodes)
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

                    break;
                default:
                    Task.entry(this.profiler, tagTypeTask, "MISSED TAG: " + nD.getTag(), null);
                    break;
            }
            tagTypeTask.end();
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
            Task buildSpatialIndexTask = new Task(this.profiler, osmProcessingTask, "Build Spatial Index", null);
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
            buildSpatialIndexTask.end();
        }

        {
            // Prune and reconnect roads
            Task t = new Task(this.profiler, osmProcessingTask, "Parse through road iterations", null);
            try
            {
                while (true)
                {
                    Iterator<Entry<Long, CountMeter>> meter = mapDataCounters.entrySet().iterator();
                    if (meter.hasNext())
                    {
                        Entry<Long, CountMeter> entry = meter.next();
                        long[] list =
                        {
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                        };

                        list[0] = meter.next().getKey();
                        list[1] = meter.next().getKey();
                        list[2] = meter.next().getKey();
                        list[3] = meter.next().getKey();
                        list[4] = meter.next().getKey();
                        list[5] = meter.next().getKey();
                        list[6] = meter.next().getKey();
                        list[7] = meter.next().getKey();
                        list[8] = meter.next().getKey();
                        list[9] = meter.next().getKey();

                        mapDataCounters.remove(list[0]);
                        mapDataCounters.remove(list[1]);
                        mapDataCounters.remove(list[2]);
                        mapDataCounters.remove(list[3]);
                        mapDataCounters.remove(list[4]);
                        mapDataCounters.remove(list[5]);
                        mapDataCounters.remove(list[6]);
                        mapDataCounters.remove(list[7]);
                        mapDataCounters.remove(list[8]);
                        mapDataCounters.remove(list[9]);
                    }
                    else
                    {
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            t.end();
        }
    }

    private String processAttributes(Node nD, Task parent)
    {
        StringBuilder attributeList = new StringBuilder();
        for (Attribute a : nD.getAttributes())
        {
            attributeList.append(", " + a.getName());
        }

        int pos = (attributeList.length() > 2) ? 2 : 0; // Trim the string if we have attributes
        return attributeList.substring(pos);
    }
}
