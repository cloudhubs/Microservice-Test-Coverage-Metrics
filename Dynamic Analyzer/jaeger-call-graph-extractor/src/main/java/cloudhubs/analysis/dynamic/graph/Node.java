package cloudhubs.analysis.dynamic.graph;

import java.util.ArrayList;
import java.util.List;

import cloudhubs.analysis.dynamic.jaeger.Span;
import lombok.Data;

@Data
public class Node {
    public Node() {
        children = new ArrayList<Node>();
    }

    private String id;

    private List<Node> children;

    private Node parent;

    private Span data;
}
