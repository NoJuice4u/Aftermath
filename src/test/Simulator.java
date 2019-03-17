package test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import main.java.aftermath.engine.Depot;
import main.java.encephalon.dto.MapEdge;

class Simulator
{
    public final int DATAENTRIES = 30000;
    public final float DEVIATION = 0.20f;
    public final int DELAY = 5;

    @Test
    @SuppressWarnings("unchecked")
    void simulateEntry() throws IOException, InterruptedException
    {
        URL url = new URL("http://localhost:8080/aftermath/map/node/1093685711/json?depth=22");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int err = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer content = new StringBuffer();

        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
            content.append(inputLine);
        }

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

            url = new URL("http://localhost:8080/aftermath/map/weight/");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(edgePost.toString());

            System.out.println("[" + con.getResponseCode() + "]\t" + edgePost.substring(1));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void markRoads() throws IOException, InterruptedException
    {
        URL url = new URL("http://localhost:8080/aftermath/map/node/1093685711/json?depth=22");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int err = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer content = new StringBuffer();

        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
            content.append(inputLine);
        }

        Map<?, ?> result = new Gson().fromJson(content.toString(), Map.class);

        Map<String, ?> edges = (Map<String, ?>) result.get("mapEdges");

        Set<String> keySet = edges.keySet();

        for (String edg : keySet)
        {
            LinkedTreeMap<?, ?> edgeLinkedTreeMap = (LinkedTreeMap<?, ?>) edges.get(edg);
            String mode = (String) edgeLinkedTreeMap.get("mode");

            if (mode.equals("primary") || mode.equals("secondary"))
            {
                url = new URL("http://localhost:8080/aftermath/map/mark/");
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                out.writeBytes(edg + "=2");

                System.out.println("[" + con.getResponseCode() + "]\t" + edg);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleDepots() throws IOException, InterruptedException
    {
        URL url = new URL("http://localhost:8080/aftermath/map/depots/json");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int err = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer content = new StringBuffer();

        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
            content.append(inputLine);
        }

        // Long, Depot
        Map<String, Depot> result = new Gson().fromJson(content.toString(), Map.class);

        Set<String> keySet = result.keySet();
        
        for (String depot : keySet)
        {
            System.out.println("[" + con.getResponseCode() + "]\t" + depot);
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    void authorativeEntriesTest() throws IOException, InterruptedException
    {
        URL url = new URL("http://localhost:8080/system/session/new");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        String sidA = con.getHeaderField("X-SID");
        
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        String sidB = con.getHeaderField("X-SID");
        
        url = new URL("http://localhost:8080/system/session/promote");
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("X-SID", sidB);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer content = new StringBuffer();

        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
            content.append(inputLine);
        }

        System.out.println(content);

        // Get an Authoritative Session
        // Fill in "5"s for authoritative answers
        // Get a Regular Session
        // Fill in somewhat randomized numbers (only deviating by 1 at most for neighboring nodes
        
        // EXPECTED RESULT:
        // Authoritative numbers show 5, and all other values are normalized to the Authoritative values.
    }
}
