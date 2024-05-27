package cloudhubs.analysis.dynamic.jaeger;

import lombok.Data;

@Data
public class Span {
    private String traceID;

    private String spanID;

    private String operationName;

    private SpanReference[] references;

    private Long startTime;

    private Long duration;

    private Tag[] tags;

    private String processID;

    private String[] warnings;
}
