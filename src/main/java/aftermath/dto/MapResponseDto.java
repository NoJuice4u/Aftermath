package main.java.aftermath.dto;

import java.util.HashMap;

import main.java.aftermath.engine.Depot;
import main.java.encephalon.cluster.Cluster;
import main.java.encephalon.dto.CoordinateRange;
import main.java.encephalon.dto.Coordinates;
import main.java.encephalon.dto.MapEdge;
import main.java.encephalon.dto.MapVertexLite;

public class MapResponseDto
{
    public Coordinates                  focus;
    public int                          zoom;
    public HashMap<Long, MapVertexLite> mapVertices;
    public HashMap<Long, MapEdge>       mapEdges;
    public HashMap<Long, Depot>         depotData;
    public HashMap<Long, Cluster>       groups;

    public MapResponseDto(double longitude, double latitude, int zoom, HashMap<Long, MapVertexLite> mapVertices,
            HashMap<Long, MapEdge> mapEdges, HashMap<Long, Depot> depotData, HashMap<Long, Cluster> groups)
    {
        this.focus = new Coordinates(longitude, latitude);
        this.zoom = zoom;
        this.mapVertices = mapVertices;
        this.mapEdges = mapEdges;
        this.depotData = depotData;
        this.groups = groups;
    }
}
