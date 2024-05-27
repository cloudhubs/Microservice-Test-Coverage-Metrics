package edu.baylor.analyzer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

class GraphNode {
    String value;
    List<GraphNode> children;

    GraphNode(String value) {
        this.value = value;
        this.children = new ArrayList<>();
    }
}

public class PathToJsonConverter {
    public static void main(String[] args) {
        String inputFile = "allpaths.txt"; // Change this to your input file path
        String outputFile = "output.json"; // Change this to your output file path

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            Map<String, GraphNode> graph = new HashMap<>();
            String line;

            while ((line = reader.readLine()) != null) {
                buildGraph(line, graph);
            }

            List<JsonObject> jsonPaths = new ArrayList<>();
            for (GraphNode root : graph.values()) {
                JsonObject jsonPath = createJsonObject(root);
                jsonPaths.add(jsonPath);
            }

            JsonObject resultObject = new JsonObject();
            resultObject.add("paths", new Gson().toJsonTree(jsonPaths));

            String jsonOutput = new GsonBuilder().setPrettyPrinting().create().toJson(resultObject);
            writer.write(jsonOutput);
            writer.close();
            reader.close();

            System.out.println("Conversion complete. Output written to " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void buildGraph(String line, Map<String, GraphNode> graph) {
        String[] nodes = line.split(":");
        GraphNode parent = null;
        boolean hasSearched = false;
        boolean isPresent;

        for (String nodeName : nodes) {
            if (!hasSearched){
                if(!graph.containsKey(nodeName)) {
                    GraphNode node = new GraphNode(nodeName);
                    graph.put(nodeName, node);
                    parent = node;
                } else {
                    parent = graph.get(nodeName);
                }
                hasSearched = true;
            } else {
                isPresent = false;
                for (int i = 0; i < parent.children.size(); i++){
                    if (nodeName.equals(parent.children.get(i).value)){
                        parent = parent.children.get(i);
                        isPresent = true;
                    }
                }
                if(!isPresent){
                    GraphNode node = new GraphNode(nodeName);
                    parent.children.add(node);
                    parent = node;
                }
            }
        }
    }

    private static String getMethodName(String nodeString) {
        String[] parts = nodeString.split("\\.");

        if (parts.length > 0) {
            // Get the last element (at index parts.length - 1)
            return parts[parts.length - 1];
        }

        System.err.println("No method found.");
        return null;
    }

    private static String getClassName(String nodeString) {
        String[] parts = nodeString.split("\\.");

        if (parts.length > 1) {
            return parts[parts.length - 2];
        }

        System.err.println("No class found.");
        return null;
    }

    private static String getComponentName(String nodeString) {
        String[] parts = nodeString.split("\\.");

        if (parts.length < 2) {
            System.err.println("No component found / bad node.");
            return null;
        }

        // Initialize an empty StringBuilder
        StringBuilder result = new StringBuilder();

        // Iterate through the elements in the array except the last one
        for (int i = 0; i < parts.length - 2; i++) {
            result.append(parts[i]);  // Append the element
            result.append(".");       // Append a period (.) delimiter
        }

        // Append the last class name without a trailing period
        result.append(parts[parts.length - 2]);


        // Convert the StringBuilder to a String
        return result.toString();
    }

    private static String getLayer(String nodeString) {
        enum Layer { CONTROLLER, REPOSITORY, SERVICE, SERIVCE, ENTITY, SECURITY, UTIL, DTO, INIT, MQ, EXCEPTION, CONFIG, UTILS }

        Layer classLayer = null;
        for (Layer layer : Layer.values()) {
            if (nodeString.contains("." + layer.toString().toLowerCase() + ".")) {
                classLayer = layer;
                break;
            }
        }

        if (classLayer != null) {
            return classLayer.toString();
        }
        System.err.println("The layer of this class could not be determined: " + nodeString);
        return null;
    }


    private static JsonObject createJsonObject(GraphNode node) {
        JsonObject jsonObject = new JsonObject();
        if(node.value.contains("-")){
            jsonObject.addProperty("condition", "if");
            node.value = node.value.substring(1);
        }
        jsonObject.addProperty("componentName", getComponentName(node.value));
        jsonObject.addProperty("layer", getLayer(node.value));
        jsonObject.addProperty("className", getClassName(node.value));
        jsonObject.addProperty("methodName", getMethodName(node.value));
        jsonObject.addProperty("node", node.value);


        if (!node.children.isEmpty()) {
            JsonArray subNodes = new JsonArray();
            for (GraphNode child : node.children) {
                subNodes.add(createJsonObject(child));
            }
            jsonObject.add("subNodes", subNodes);
        }else {
            jsonObject.add("subNodes", new JsonArray());
        }

        return jsonObject;
    }
}
