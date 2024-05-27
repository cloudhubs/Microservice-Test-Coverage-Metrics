package cloudhubs.analysis.dynamic.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import cloudhubs.analysis.dynamic.jaeger.Span;
import cloudhubs.analysis.dynamic.jaeger.Trace;
import cloudhubs.analysis.dynamic.output.Path;
import lombok.Data;

@Data
public class Graph {
    private Graph() {
        super();
    }

    private Node root;

    private Trace trace;

    private int size;

    public static Graph createGraph(Span[] spans) {
        Graph graph = new Graph();

        var nodes = new Node[spans.length];

        var idToNode = new java.util.HashMap<String, Node>();

        for (int i = 0; i < spans.length; i++) {
            var span = spans[i];
            var node = new Node();
            node.setData(span);
            node.setId(span.getTraceID() + "$" + span.getSpanID());
            nodes[i] = node;
            idToNode.put(node.getId(), node);
        }

        for (int i = 0; i < spans.length; i++) {
            var span = spans[i];
            var node = nodes[i];

            for (var ref : span.getReferences()) {
                if (ref.getRefType().equals("CHILD_OF")) {
                    var parent = idToNode.get(ref.getTraceID() + "$" + ref.getSpanID());
                    node.setParent(parent);
                    parent.getChildren().add(node);
                }
            }
        }

        var fakeRoot = new Node();
        fakeRoot.setId("root");
        fakeRoot.setData(null);
        fakeRoot.setParent(null);

        for (var node : nodes) {
            if (node.getParent() == null) {
                fakeRoot.getChildren().add(node);
                node.setParent(fakeRoot);
            }
        }

        graph.setRoot(fakeRoot);
        graph.setSize(spans.length + 1);

        return graph;
    }

    public void deleteNode(Node n) {
        for (var child : n.getChildren()) {
            child.setParent(n.getParent());
            n.getParent().getChildren().add(child);
        }

        n.getParent().setChildren(
                n.getParent().getChildren().stream().filter(x -> x != n).collect(java.util.stream.Collectors.toList()));

        size--;
    }

    public void deleteNodes(Predicate<Node> predicate) {
        var toDelete = new ArrayList<Node>();
        var queue = new java.util.LinkedList<Node>();
        for (var node : root.getChildren()) {
            queue.add(node);
        }
        while (queue.size() > 0) {
            var current = queue.poll();

            if (predicate.test(current)) {
                toDelete.add(current);
            }

            for (var child : current.getChildren()) {
                queue.add(child);
            }
        }

        for (var node : toDelete) {
            deleteNode(node);
        }
    }

    public void deleteNodes(String[] withTags) {
        var toDelete = new ArrayList<Node>();
        var withTagsSet = new java.util.HashSet<String>(Arrays.asList(withTags));
        var queue = new java.util.LinkedList<Node>();
        for (var node : root.getChildren()) {
            queue.add(node);
        }
        while (queue.size() > 0) {
            var current = queue.poll();

            if (withTagsSet.contains(Arrays.stream(current.getData().getTags())
                    .filter(t -> t.getKey().equals("otel.library.name")).findFirst()
                    .get().getValue())) {
                toDelete.add(current);
            }

            for (var child : current.getChildren()) {
                queue.add(child);
            }
        }

        for (var node : toDelete) {
            deleteNode(node);
        }
    }

    public void keepOnlyNodes(String[] withTags) {
        var toDelete = new ArrayList<Node>();
        var withTagsSet = new java.util.HashSet<String>(Arrays.asList(withTags));
        var queue = new java.util.LinkedList<Node>();
        for (var node : root.getChildren()) {
            queue.add(node);
        }
        while (queue.size() > 0) {
            var current = queue.poll();

            var otelLibraryName = Arrays.stream(current.getData().getTags())
                    .filter(t -> t.getKey().equals("otel.library.name")).findFirst();
            if (otelLibraryName.isEmpty() || !withTagsSet.contains(otelLibraryName.get().getValue())) {
                toDelete.add(current);
            }

            for (var child : current.getChildren()) {
                queue.add(child);
            }
        }

        for (var node : toDelete) {
            deleteNode(node);
        }
    }

    public List<Path> toPaths() {
        var queue = new java.util.LinkedList<PathNode>();

        for (var node : root.getChildren()) {
            var pathNode = new PathNode(node, null);
            queue.add(pathNode);
        }

        var paths = new ArrayList<Path>();

        while (queue.size() > 0) {
            var pathNode = queue.poll();
            var node = pathNode.getNode();
            var parentPath = pathNode.getParentPath();

            var path = new Path();
            var methodName = Arrays.stream(node.getData().getTags()).filter(t -> t.getKey().equals("code.function"))
                    .findFirst().get();
            var componentName = Arrays.stream(node.getData().getTags()).filter(t -> t.getKey().equals("code.namespace"))
                    .findFirst().get();
            var parts = componentName.getValue().split("\\.");

            path.setMethodName(methodName.getValue());
            path.setComponentName(componentName.getValue());
            path.setClassName(parts[parts.length - 1]);
            path.setNode(componentName.getValue() + "." + methodName.getValue());

            if (parentPath != null) {
                parentPath.getSubNodes().add(path);
            } else {
                paths.add(path);
            }

            for (var child : node.getChildren()) {
                var newPathNode = new PathNode(child, path);
                queue.add(newPathNode);
            }
        }

        return paths;
    }
}
