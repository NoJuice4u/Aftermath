package main.java.aftermath.engine;

import main.java.aftermath.vehicles.Bin;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.profiler.CountMeter;
import main.java.encephalon.server.EncephalonServer;

public class Depot extends Bin
{
    public static final String DEPOT_ACTIVE_COUNT_METER = "Depots.Active";
    public static final String DEPOT_TOTAL_COUNT_METER = "Depots.Total";
    private static long idIncrement;
    private final long id;
    private final long edgeId;
    private boolean active = false;
    private String name;
    
    static
    {
        EncephalonServer.getInstance().getCountMeters().put(DEPOT_ACTIVE_COUNT_METER, new CountMeter());
        EncephalonServer.getInstance().getCountMeters().put(DEPOT_TOTAL_COUNT_METER, new CountMeter());
    }

    public Depot(String name, double capacity, MapEdge edge) throws Exception
    {
        super(capacity, (float) edge.getLongitude(), (float) edge.getLatitude());

        this.id = idIncrement++;
        this.edgeId = edge.getId();
        this.name = name;
        
        EncephalonServer.getInstance().getCountMeters().get(DEPOT_TOTAL_COUNT_METER).increment();
    }

    public Depot(String name, double capacity, float lon, float lat) throws Exception
    {
        super(capacity, lon, lat);

        this.id = idIncrement++;
        this.edgeId = 0;
        this.name = name;
        
        EncephalonServer.getInstance().getCountMeters().get(DEPOT_TOTAL_COUNT_METER).increment();
    }

    public long getId()
    {
        return id;
    }
    
    public long getEdgeId()
    {
        return edgeId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    public boolean isActive()
    {
        return active;
    }
    
    public synchronized void activate()
    {
        if(!this.active) 
        {
            this.active = true;
            EncephalonServer.getInstance().getCountMeters().get(DEPOT_ACTIVE_COUNT_METER).increment();
        }
    }
    
    public synchronized void deactivate()
    {
        if(!this.active) 
        {
            this.active = false;
            EncephalonServer.getInstance().getCountMeters().get(DEPOT_ACTIVE_COUNT_METER).decrement();
        }
    }
}