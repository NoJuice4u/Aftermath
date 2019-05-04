package test;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import main.java.aftermath.engine.Depot;
import main.java.encephalon.client.HttpApiClient;

class Simulator
{
    public final int DATAENTRIES = 30000;
    public final float DEVIATION = 0.15f;
    public final int DELAY = 5;

    @Test
    @SuppressWarnings("unchecked")
    void simulateEntry() throws Exception
    {
        HttpApiClient client = new HttpApiClient();
        
        client.makeRequestGet("http", null, "localhost:8080", "aftermath/map/node/??/json?depth=??", "1093685732", "22");
        
        int err = client.getResponseCode();
        String content = client.readResponse();
        
        Map<?, ?> result = new Gson().fromJson(content.toString(), Map.class);

        Map<String, ?> vertices = (Map<String, ?>) result.get("mapVertices");

        Set<String> keySet = vertices.keySet();
        HashMap<Integer, Integer> edgeWeightMap = new HashMap<Integer, Integer>();

        Object[] keys = keySet.toArray();

        for (int i = 0; i < DATAENTRIES; i++)
        {
            Thread.sleep(DELAY);
            int rnd = (int) (Math.random() * keys.length);
            String s = (String) keys[rnd];
            Map edgeList = (Map) vertices.get(s);
            ArrayList<Double> edges = (ArrayList<Double>) edgeList.get("edges");
            StringBuilder edgePost = new StringBuilder();
            for (Double d : edges)
            {
                Integer it = (int) Math.floor(d);
                if (!edgeWeightMap.containsKey(it))
                {
                    int weight = (int) Math.round(Math.random() * 10);
                    edgeWeightMap.put(it, weight);
                }

                int edgeWeight = edgeWeightMap.get(it) + (int) (DEVIATION * 10 * (Math.random() - DEVIATION));
                if (edgeWeight < 0)
                    edgeWeight = 0;
                else if (edgeWeight > 10)
                    edgeWeight = 10;
                edgePost.append("&" + it + "=" + edgeWeight);
            }

            client.makeRequestPost("http", null, "localhost:8080", "aftermath/map/weight", edgePost.substring(1));
            
            System.out.println("[" + client.getResponseCode() + "]\t" + edgePost.substring(1));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void markRoads() throws Exception
    {
        HttpApiClient client = new HttpApiClient();
        
        client.makeRequestGet("http", null, "localhost:8080", "aftermath/map/node/??/json?depth=??", "1093685711", "22");

        int err = client.getResponseCode();
        String content = client.readResponse();

        Map<?, ?> result = new Gson().fromJson(content.toString(), Map.class);

        Map<String, ?> edges = (Map<String, ?>) result.get("mapEdges");

        Set<String> keySet = edges.keySet();

        for (String edg : keySet)
        {
            LinkedTreeMap<?, ?> edgeLinkedTreeMap = (LinkedTreeMap<?, ?>) edges.get(edg);
            String mode = (String) edgeLinkedTreeMap.get("mode");

            if (mode.equals("primary") || mode.equals("secondary"))
            {
                client.makeRequestPost("http", null, "localhost:8080", "aftermath/map/mark", edg + "=2");
            }
        }
    }
    
    @Test
    void microTest() throws Exception
    {
        HttpApiClient client = new HttpApiClient();
        
        int[][][] weightGroup = {{{0, 3}, {1, 3}, {3, 3}, {4, 3}, {6, 3}, {7, 3}},
                                {{8, 7}, {9, 7}, {10, 7}, {11, 7}, {12, 7}, {12, 7}, {13, 7}},
                                {{8, 6}, {0, 2}}};
        
        int[] sequence = {0, 0, 0, 0, 0, 1, 1, 1, 2, 1, 2, 0, 2, 2, 2, 2};
        
        for(int z = 0; z < 10; z++)
        {
            for(int i : sequence)
            {
                StringBuilder edgePost = new StringBuilder();
                
                for(int[] item : weightGroup[i])
                {
                    edgePost.append("&" + item[0] + "=" + item[1]);
                }
                
                System.out.println(edgePost.substring(1));
                client.makeRequestPost("http", null, "localhost:8080", "aftermath/map/weight", edgePost.substring(1));
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleDepots() throws Exception
    {
        HttpApiClient client = new HttpApiClient();
        
        client.makeRequestGet("http", null, "localhost:8080", "aftermath/map/depots/json");

        int err = client.getResponseCode();
        String content = client.readResponse();

        // Long, Depot
        Map<String, Depot> result = new Gson().fromJson(content.toString(), Map.class);
        Set<String> keySet = result.keySet();
        
        for (String depot : keySet)
        {
            System.out.println("[" + err + "]\t" + depot);
        }
    }
    
    @Test
    void authorativeEntriesTest() throws Exception
    {
        HttpApiClient client = new HttpApiClient();
        HashMap<String, String> userA = new HashMap<String, String>();
        HashMap<String, String> userB = new HashMap<String, String>();
        
        client.makeRequestGet("http", null, "localhost:8080", "system/session/new");
        String sidA = client.getHeaderField("X-SID");
        userA.put("X-SID", sidA);
        
        client.makeRequestGet("http", null, "localhost:8080", "system/session/new");
        String sidB = client.getHeaderField("X-SID");
        userB.put("X-SID", sidB);
        
        HttpURLConnection connection = client.makeRequestGet("http", userB, "localhost:8080", "aftermath/map");
        System.out.println("EOT");

        // Get an Authoritative Session
        // Fill in "5"s for authoritative answers
        // Get a Regular Session
        // Fill in somewhat randomized numbers (only deviating by 1 at most for neighboring nodes
        
        // EXPECTED RESULT:
        // Authoritative numbers show 5, and all other values are normalized to the Authoritative values.
    }
}
