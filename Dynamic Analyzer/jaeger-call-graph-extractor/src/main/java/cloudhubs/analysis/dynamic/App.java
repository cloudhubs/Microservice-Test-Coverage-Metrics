package cloudhubs.analysis.dynamic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import cloudhubs.analysis.dynamic.graph.Graph;
import cloudhubs.analysis.dynamic.jaeger.JaegerApiService;
import cloudhubs.analysis.dynamic.output.Output;
import cloudhubs.analysis.dynamic.output.Path;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        var configPath = "config.json";
        if (args.length == 0) {
            System.out.println("Configuration file not provided. Using default `config.json`");
        } else {
            configPath = args[0];
        }
        var configFile = FileUtils.getFile(configPath);
        if (!configFile.exists()) {
            System.out.println("Configuration file `" + configPath
                    + "` not found. See samples/config.json for an example.");
            return;
        }
        var config = new Gson().fromJson(FileUtils.readFileToString(configFile, "UTF-8"), AppConfig.class);

        var jaegerApiService = new JaegerApiService(config.getJaegerApiBaseUrl(), config.getStartTimestamp(),
                config.getEndTimestamp());

        var services = jaegerApiService.listServices();

        var graphs = new HashMap<String, Graph>();

        for (var service : services.getData()) {
            var traces = jaegerApiService.listTraces(service);
            for (var trace : traces.getData()) {
                if (graphs.containsKey(trace.getTraceID())) {
                    continue;
                }

                var graph = Graph.createGraph(trace.getSpans());
                graph.setTrace(trace);
                graph.keepOnlyNodes(config.getSpanKeepTags());

                if (graph.getSize() > 1) {
                    graphs.put(trace.getTraceID(), graph);
                }
            }
        }

        var paths = new ArrayList<Path>();

        for (var graph : graphs.values()) {
            var graphPath = graph.toPaths();
            paths.addAll(graphPath);
        }

        var output = new Output(paths);

        var outputStr = new Gson().toJson(output, Output.class);

        FileUtils.writeStringToFile(FileUtils.getFile(config.getOutputFile()), outputStr, "UTF-8");
    }
}
