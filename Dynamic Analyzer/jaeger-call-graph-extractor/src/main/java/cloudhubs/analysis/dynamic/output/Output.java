package cloudhubs.analysis.dynamic.output;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Output {
    private List<Path> paths;
}
