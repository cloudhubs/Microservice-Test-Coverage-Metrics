package cloudhubs.analysis.dynamic.jaeger;

import lombok.Data;

@Data
public class SpanReference {
    private String refType;

    private String traceID;

    private String spanID;
}
