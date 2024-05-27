package cloudhubs.analysis.dynamic.output;

import java.util.List;

import lombok.Data;

@Data
public class Path {
    public Path() {
        subNodes = new java.util.ArrayList<>();
    }

    private String componentName;
    private String className;
    private String methodName;
    private String node;
    private List<Path> subNodes;
}
