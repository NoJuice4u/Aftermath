package main.java.aftermath.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import main.java.aftermath.controllers.AftermathController;
import main.java.encephalon.dto.Coordinates;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.dto.MapVertex;

public class Transport extends Bin{
	private AftermathController controller;
	private static long nextId = 0;
	private final long id;
	private double VEHICLE_VELOCITY = 0.5/111.0;

	private final static Random randomizer = new Random();

	private int ticks = 0;
	private int WeightChange = 1;
	private long lastTick, nextTick, tickAtLastNode;
	private MapEdge edge;
	private MapVertex node, previousNode;
	private Map<MapVertex, Double> previousProbabilityList;
	private Coordinates destination = null;

	public Transport(AftermathController controller, Long e) throws Exception
	{
		super(500);
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
		MapVertex vertex = controller.getMapData().get(node.getId());
		if(destination == node)
		{
			Long l = controller.getSpatialIndexDepot().getNearestNode(vertex);
			if(l != -1)
			{
				destination = controller.getEdgeData().get(l);
			}
		}

		long vEdge = vertex.getEdges().get(0);
		MapEdge mEd = controller.getEdgeData().get(vertex.getEdges().get(0));
		long vertexId = controller.getEdgeData().get(vertex.getEdges().get(0)).getVertices()[0];
		MapVertex newNode = controller.getMapData().get(vertexId);
		if(node == newNode)
		{
			vertexId = controller.getEdgeData().get(vertex.getEdges().get(0)).getVertices()[0];
			newNode = controller.getMapData().get(vertexId);
		}
		long chosenEdge = -1;
		float totalPref = 0.0f;
		for(Long e : vertex.getEdges())
		{
			if(edge != null && e == edge.getId())
			{
				continue;
			}

			MapEdge mapEdge = controller.getEdgeData().get(e);
			float preference = preferenceSwitchCase(mapEdge);
			totalPref += preference;
		}
		double rnd = randomizer.nextDouble() * totalPref;
		for(Long e : vertex.getEdges())
		{
			if(edge != null && e == edge.getId())
			{
				continue;
			}

			MapEdge mapEdge = controller.getEdgeData().get(e);
			float preference = preferenceSwitchCase(mapEdge);

			for(Long vtxId : controller.getEdgeData().get(e).getVertices())
			{
				MapVertex checkNode = controller.getMapData().get(vtxId);
				if(node.getId().equals(checkNode.getId()) || previousNode.getId().equals(checkNode.getId())) continue;

				newNode = checkNode;
				chosenEdge = e;
				totalPref -= preference;

				/*
				if(this.id == 149 && node.getEdges().size() == 3)
				{
					System.out.println(rnd + " : " + totalPref + " : " + preference);
				}
				*/

				if(rnd > totalPref) // heading < bestHeading
				{
					totalPref = 0.0f;
					break;
				}
			}
			if(totalPref == 0.0f)
			{
				break;
			}
		}

		tickAtLastNode = System.currentTimeMillis();

		long nextTickInterval = (long)(1000*(Coordinates.GetDistance(node, newNode)/VEHICLE_VELOCITY));
		lastTick = System.currentTimeMillis();
		nextTick = lastTick + nextTickInterval;
		ticks++;

		previousNode = node;
		node = newNode;
		edge = controller.getEdgeData().get(chosenEdge);

		if(chosenEdge != -1)
		{
			controller.getEdgeData().get(chosenEdge).addScore();
			controller.getEdgeData().get(chosenEdge).reduceWeight(WeightChange);
		}

		return nextTick > lastTick;
	}
	
	public void setWeightChange(int value)
	{
		WeightChange = value;
	}

	public List<MapEdge> getTraversible()
	{
		List<Long> edges = controller.getMapData().get(node.getId()).getEdges();
		List<MapEdge> edgeList = new ArrayList<MapEdge>();

		for(Long l : edges)
		{
			edgeList.add(controller.getEdgeData().get(l));
		}
		return edgeList;
	}

	public Long getId()
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
		double diff = ((double)System.currentTimeMillis() - lastTick) / (double)(nextTick - lastTick);
		return Coordinates.GetPositionBetween(previousNode, node, diff);
	}

	public boolean shouldTick()
	{
		return nextTick < System.currentTimeMillis();
	}

	public int getTicks()
	{
		return ticks;
	}

	private float preferenceSwitchCase(MapEdge mapEdge)
	{
		switch(String.valueOf(mapEdge.getMode()))
		{
		case "primary":
		case "primary_link":
			return 0.9f;
		case "secondary":
		case "secondary_link":
			return 0.8f;
		case "tertiary":
		case "tertiary_link":
			return 0.75f;
		case "residential":
		case "living_street":
			return 0.1f;
		case "motorway":
		case "motorway_link":
			return 0.95f;
		case "service":
		default:
			return 0.1f;
		}
	}
}
