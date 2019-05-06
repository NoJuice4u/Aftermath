package main.java.aftermath.vehicles;

import java.util.Iterator;
import java.util.List;

import main.java.encephalon.dto.Coordinates;

public class Bin extends Coordinates
{
    private double      binCapacity, finalBinCapacity;
    private double      binCurrentLoad;
    private final float efficiency = 0.8f;

    public Bin(double capacity, float longitude, float latitude) throws Exception
    {
        super(longitude, latitude);
        this.binCapacity = capacity;
        this.finalBinCapacity = capacity * efficiency;
        this.binCurrentLoad = 0;
    }

    public void addLoad(List<Double> load)
    {
        Iterator<Double> iter = load.iterator();
        while (iter.hasNext())
        {
            Double d = iter.next();
            if (d + binCurrentLoad >= finalBinCapacity)
            {
                binCurrentLoad += d;
                iter.remove();
            }
        }
    }
}