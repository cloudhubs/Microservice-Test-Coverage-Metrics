package cloudhubs.analysis.dynamic.jaeger;

import lombok.Data;

@Data
public class TraceProcess {
    private String serviceName;

    private Tag[] tags;
}
