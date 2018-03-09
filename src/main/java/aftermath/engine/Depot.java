package main.java.aftermath.engine;

import main.java.aftermath.vehicles.Bin;
import main.java.encephalon.dto.MapEdge;

public class Depot extends Bin{
	private static long idIncrement;
	private final long id;
	private final long edgeLocation;
	private final String name;
	
	public Depot(String name, double capacity, MapEdge edge) throws Exception {
		super(capacity, (float)edge.getLongitude(), (float)edge.getLatitude());
		
		this.id = idIncrement++;
		this.edgeLocation = edge.getId();
		this.name = name;
	}
	
	public long getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
}