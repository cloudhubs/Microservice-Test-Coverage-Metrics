package cloudhubs.analysis.dynamic.graph;

import cloudhubs.analysis.dynamic.output.Path;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PathNode {
    private Node node;

    private Path parentPath;
}
