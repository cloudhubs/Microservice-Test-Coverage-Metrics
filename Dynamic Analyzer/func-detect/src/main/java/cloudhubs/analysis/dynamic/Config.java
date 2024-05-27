package cloudhubs.analysis.dynamic;

import lombok.Data;

@Data
public class Config {
    private String rootPath;

    private String[] ignoreDirs;

    private String outputFile;

    private boolean addGetters;

    private boolean addSetters;

    private boolean addConstructors;
}
