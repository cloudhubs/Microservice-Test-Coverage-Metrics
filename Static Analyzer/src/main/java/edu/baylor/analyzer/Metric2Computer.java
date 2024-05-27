package edu.baylor.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static edu.baylor.analyzer.Metric1Computer.combineElementsAfterIndex;
import static edu.baylor.analyzer.Metric1Computer.findIndexOfLayerInComponent;

public class Metric2Computer {

    public static void main(String[] args) {
        String fileName = "output.json"; // Replace with the path to your JSON file

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(fileName));
            JsonNode paths = rootNode.get("paths");
            Set<String> pathSet = new TreeSet<>();
            Map<String, Set<String>> componentNameMap = new TreeMap<>(); // Use TreeMap for sorting by keys
            extractPaths("", paths, pathSet);

            Map<String, Integer> msCountMap = new TreeMap<>(); // Use TreeMap for sorting by keys


            // Display the sorted paths
            for (String path : pathSet) {
//                System.out.println(path);
                String key = null;

                int indexOfFirstPeriod = path.indexOf('.');
                if (indexOfFirstPeriod != -1) { // Check if a period is found
                    key = path.substring(0, indexOfFirstPeriod);
                } else {
                    System.out.println("Bad path.");
                }

                if (!componentNameMap.containsKey(key)) {
                    componentNameMap.put(key, new HashSet<>());
                }
                componentNameMap.get(key).add(path);
                // Print the map entries

                // Metric 3
                String[] components = path.split(":");

                for (String call : components){
                    int msPeriod = call.indexOf('.');
                    String ms = call.substring(0, msPeriod);
                    if (!msCountMap.containsKey(ms)) {
                        msCountMap.put(ms, 0);
                    }
                    msCountMap.put(ms, msCountMap.get(ms) + 1);
                }
            }
            for (Map.Entry<String, Set<String>> entry : componentNameMap.entrySet()) {
                System.out.println("Microservice: " + entry.getKey() + ", # of paths: " + entry.getValue().size());
            }
            System.out.println("Total number of distinct paths: " + pathSet.size());

            System.out.println("-----------------");
            System.out.println("METRIC 3\n");

            for (Map.Entry<String, Integer> entry : msCountMap.entrySet()) {
                System.out.println("Microservice: " + entry.getKey() + ", frequency #: " + entry.getValue());
            }

            // Find the highest and lowest occurrences of microservices
            int highestValue = findEntryWithHighestValue(msCountMap).getValue();
            int lowestValue = findEntryWithLowestValue(msCountMap).getValue();

            // Find microservices with the highest value
            System.out.println("Microservices with the highest value (" + highestValue + "):");
            findKeysWithSpecificValue(msCountMap, highestValue);

            // Find microservices with the lowest value
            System.out.println("Microservices with the lowest value (" + lowestValue + "):");
            findKeysWithSpecificValue(msCountMap, lowestValue);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void extractPaths(String parentPath, JsonNode nodes, Set<String> pathSet) {
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {

                String nodeName = node.get("componentName").asText();
                String[] components = nodeName.split("\\.");
                // Determine name of service
                String[] layers = {"CONTROLLER", "REPOSITORY", "SERVICE", "SERIVCE", "ENTITY", "SECURITY",
                        "UTIL", "DTO", "INIT", "MQ", "EXCEPTION", "CONFIG", "UTILS"};
                int index = findIndexOfLayerInComponent(layers, components);
                if (index != -1) {
                    nodeName = combineElementsAfterIndex(components, index - 1, ".");
                } else {
                    System.out.println("No layer found: " + nodeName);
                }


                String fullPath = parentPath.isEmpty() ? nodeName : parentPath + ":" + nodeName;
                pathSet.add(fullPath);

                JsonNode subNodes = node.get("subNodes");
                if (subNodes != null && subNodes.isArray()) {
                    extractPaths(fullPath, subNodes, pathSet);
                }
            }
        }
    }

    public static Map.Entry<String, Integer> findEntryWithHighestValue(Map<String, Integer> map) {
        Map.Entry<String, Integer> highestEntry = null;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (highestEntry == null || entry.getValue() > highestEntry.getValue()) {
                highestEntry = entry;
            }
        }
        return highestEntry;
    }

    public static Map.Entry<String, Integer> findEntryWithLowestValue(Map<String, Integer> map) {
        Map.Entry<String, Integer> lowestEntry = null;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (lowestEntry == null || entry.getValue() < lowestEntry.getValue()) {
                lowestEntry = entry;
            }
        }
        return lowestEntry;
    }

    public static void findKeysWithSpecificValue(Map<String, Integer> map, int targetValue) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() == targetValue) {
                System.out.println(entry.getKey());
            }
        }
    }
}
