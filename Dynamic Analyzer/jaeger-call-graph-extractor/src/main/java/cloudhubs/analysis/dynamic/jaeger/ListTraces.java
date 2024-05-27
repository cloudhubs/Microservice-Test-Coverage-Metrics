package cloudhubs.analysis.dynamic.jaeger;

import lombok.Data;

@Data
public class ListTraces {
    private Trace[] data;

    private int total;

    private int limit;

    private int offset;

    private String[] errors;
}
