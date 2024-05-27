package edu.baylor;

import java.util.ArrayList;
import java.util.List;

public class Node {

    String name;
    List<String> neighbors;

    Node(String name) {
        this.name = name;
        this.neighbors = new ArrayList<>();
    }

    void addNeighbor(String neighborName) {
        neighbors.add(neighborName);
    }
}
