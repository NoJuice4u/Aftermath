package main.java.aftermath.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import main.java.aftermath.controllers.AftermathController;
import main.java.aftermath.server.AftermathServer;
import main.java.encephalon.dto.Coordinates;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.dto.MapVertex;
import main.java.encephalon.spatialIndex.SpatialIndex;

public class Transport extends Bin
{
    private static final AftermathServer as = AftermathServer.getInstance();
    private transient AftermathController controller;
    private transient final static Random randomizer = new Random();

    private static long nextId           = 0;
    private final long  id;
    private double      VEHICLE_VELOCITY = as.getProperty("aftermath.vehicle.velocity", 0.5f, "Vehicle speed") / SpatialIndex.COORDINATES_TO_DISTANCE_FACTOR;

    private int  ticks        = 0;
    private int  WeightChange = 1;
    private long lastTick, nextTick, tickAtLastNode;
    private long previousEdge = 0L;

    private MapEdge                          edge;
    private MapVertex                        node, previousNode;
    private transient Map<MapVertex, Double> previousProbabilityList;

    private Coordinates destination = null;

    public Transport(AftermathController controller, long e) throws Exception
    {
        super(500, 0, 0);
        this.controller = controller;
        this.id = Transport.incrementAndGetId();
        this.node = controller.getMapData().get(controller.getEdgeData().get(e).getVertices()[0]);
        this.destination = this.node;
        this.previousNode = controller.getMapData().get(controller.getEdgeData().get(e).getVertices()[1]);
        this.edge = controller.getEdgeData().get(e);
        this.tickAtLastNode = System.currentTimeMillis();
        this.lastTick = System.currentTimeMillis();
        this.nextTick = System.currentTimeMillis();
    }

    public static long incrementAndGetId()
    {
        return ++nextId;
    }

    public boolean traverse() throws Exception
    {
        List<Long> newList = new ArrayList<Long>(node.getEdges());
        if (newList.size() > 1)
        {
            newList.remove(previousEdge);
        }

        edge = controller.getEdgeData().get(newList.get(0));

        for (Long l : newList)
        {
            MapEdge mE = controller.getEdgeData().get(l);
            if (randomizer.nextFloat() > 0.20 && mE.getWeight() < 5)
            {
                edge = mE;
            }
        }
        if (edge.getConfidence() > 0.1)
        {
            // Test Case to add additional weights
            // edge.addWeightInput(true, 0L, System.currentTimeMillis(), edge.getWeight());
        }

        MapVertex newVertex = controller.getMapData().get(edge.getOtherVertex(node.getId()));

        previousEdge = edge.getId();
        previousNode = node;
        node = newVertex;

        lastTick = System.currentTimeMillis();
        nextTick = System.currentTimeMillis() - 20;

        ticks++;

        return true;
    }

    public void setWeightChange(int value)
    {
        WeightChange = value;
    }

    public List<MapEdge> getTraversible()
    {
        List<Long> edges = controller.getMapData().get(node.getId()).getEdges();
        List<MapEdge> edgeList = new ArrayList<MapEdge>();

        for (Long l : edges)
        {
            edgeList.add(controller.getEdgeData().get(l));
        }
        return edgeList;
    }

    public long getId()
    {
        return id;
    }

    public MapVertex getNode()
    {
        return node;
    }

    public MapEdge getEdge()
    {
        return edge;
    }

    public MapVertex getPreviousNode()
    {
        return previousNode;
    }

    public Map<MapVertex, Double> getProbabilityTree()
    {
        return previousProbabilityList;
    }

    public Coordinates getDestination()
    {
        return destination;
    }

    public Coordinates getPosition()
    {
        // double diff = ((double)System.currentTimeMillis() - lastTick) /
        // (double)(nextTick - lastTick);
        double diff = 1.0;
        return Coordinates.GetPositionBetween(previousNode, node, diff);
    }

    public boolean shouldTick()
    {
        return true;
        // return nextTick < System.currentTimeMillis();
    }

    public int getTicks()
    {
        return ticks;
    }

    private float preferenceSwitchCase(MapEdge mapEdge)
    {
        switch (String.valueOf(mapEdge.getMode()))
        {
            case "primary":
            case "primary_link":
                return 0.9f;
            case "secondary":
            case "secondary_link":
                return 0.9f;
            case "tertiary":
            case "tertiary_link":
                return 0.9f;
            case "residential":
            case "living_street":
                return 0.1f;
            case "motorway":
            case "motorway_link":
                return 0.9f;
            case "service":
            default:
                return 0.1f;
        }
    }
}
