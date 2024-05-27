package cloudhubs.analysis.dynamic.jaeger;

import java.util.Map;

import lombok.Data;

@Data
public class Trace {
    private String traceID;

    private Span[] spans;

    private Map<String, TraceProcess> processes;

    private String[] warnings;
}
