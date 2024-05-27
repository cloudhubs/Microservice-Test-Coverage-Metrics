package edu.baylor.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.File;
import java.io.IOException;
import java.util.*;

public class Metric1Computer {

    public static void extractComponentNames(JsonNode node, Set<String> componentNames) {
        if (node.isArray()) {
            for (JsonNode element : node) {
                extractComponentNames(element, componentNames);
            }
        } else if (node.isObject()) {
            JsonNode componentNameNode = node.get("componentName");
            if (componentNameNode != null && componentNameNode.isTextual()) {
                String componentName = componentNameNode.asText();
                componentNames.add(componentName);

            }
            for (JsonNode subNode : node) {
                extractComponentNames(subNode, componentNames);
            }
        }
    }

    public static int findIndexOfLayerInComponent(String[] layers, String[] components) {
        for (int i = 0; i < layers.length; i++) {
            String valueToFind = layers[i];
            for (int j = 0; j < components.length; j++) {
                if (components[j].equalsIgnoreCase(valueToFind)) {
                    return j; // Return the index where the value is found in the components array
                }
            }
        }
        return -1; // Return -1 if no matching value is found in the components array
    }

    public static String combineElementsAfterIndex(String[] elements, int startIndex, String delimiter) {
        if (startIndex < 0 || startIndex >= elements.length) {
            return ""; // Return an empty string if the startIndex is out of bounds
        }

        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < elements.length; i++) {
            if (i > startIndex) {
                result.append(delimiter); // Add the delimiter between elements
            }
            result.append(elements[i]);
        }

        return result.toString();
    }

    public static void main(String[] args) throws IOException {
        // Replace with the actual JSON file path
        String filePath = "output.json";

        File jsonFile = new File(filePath);
        if (!jsonFile.exists()) {
            System.err.println("JSON file not found.");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonFile);

        // Navigate to the "paths" array
        JsonNode pathsArray = rootNode.get("paths");
        Map<String, Set<String>> componentNameMap = new TreeMap<>(); // Use TreeMap for sorting by keys

        if (pathsArray != null) {
            Set<String> componentNames = new TreeSet<>();
            extractComponentNames(pathsArray, componentNames);

            // Print the unique component names
            for (String componentName : componentNames) {
                String[] components = componentName.split("\\.");
                // Determine name of service
                String[] layers = {"CONTROLLER", "REPOSITORY", "SERVICE", "SERIVCE", "ENTITY", "SECURITY",
                        "UTIL", "DTO", "INIT", "MQ", "EXCEPTION", "CONFIG", "UTILS"};
                int index = findIndexOfLayerInComponent(layers, components);
                if (index != -1) {
                    // Add the relevant part of the component name to the map
                    String value = combineElementsAfterIndex(components, index - 1, ".");
                    // System.out.println(value);
                    // System.out.println("Layer is present at " + index);
                    if (!componentNameMap.containsKey(components[index - 1])) {
                        componentNameMap.put(components[index - 1], new HashSet<>());
                    }
//                    if(Objects.equals(components[index - 1], "trainticket")){
//                        System.out.println(componentName);
//                    }
                    componentNameMap.get(components[index - 1]).add(value);
                } else {
                    System.out.println("No layer found: " + componentName);
                }
            }

            // Print the map entries
            for (Map.Entry<String, Set<String>> entry : componentNameMap.entrySet()) {
                System.out.println("Microservice: " + entry.getKey() + ", # of methods: " + entry.getValue().size());
            }

            // Number of methods
            System.out.println("Total methods covered: " + componentNames.size());

        } else {
            System.err.println("No 'paths' array found in the JSON.");
        }
    }

}
