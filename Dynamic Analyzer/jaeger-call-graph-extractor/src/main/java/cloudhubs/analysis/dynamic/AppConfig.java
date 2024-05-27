package cloudhubs.analysis.dynamic;

import lombok.Data;

@Data
public class AppConfig {
    private String jaegerApiBaseUrl;

    private Long startTimestamp;

    private Long endTimestamp;

    private String[] spanKeepTags;

    private String outputFile;
}
