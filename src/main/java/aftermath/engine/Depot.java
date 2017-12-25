package main.java.aftermath.engine;

import main.java.aftermath.vehicles.Bin;

public class Depot extends Bin{
	private final long edgeLocation;
	
	public Depot(double capacity, long edgeLocation) throws Exception {
		super(capacity);
		
		this.edgeLocation = edgeLocation;
	}
}