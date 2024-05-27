package cloudhubs.analysis.dynamic.jaeger;

import lombok.Data;

@Data
public class Tag {
    private String key;

    private String type;

    private String value;
}
