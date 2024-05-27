package cloudhubs.analysis.dynamic.jaeger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

public class JaegerApiService {

    public JaegerApiService(String jaegerBaseUrl, Long startTimestamp, Long endTimestamp) {
        this.jaegerBaseUrl = jaegerBaseUrl.replaceAll("/$", "");
        this.client = HttpClient.newHttpClient();

        var now = System.currentTimeMillis() * 1000;

        this.startTimestamp = startTimestamp == null ? now - 3600000000L : startTimestamp;
        this.endTimestamp = endTimestamp == null ? now : endTimestamp;
    }

    private final String jaegerBaseUrl;

    private final Long startTimestamp;

    private final Long endTimestamp;

    private final HttpClient client;

    public ListServices listServices() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(jaegerBaseUrl + "/api/services")).GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return new Gson().fromJson(response.body(), ListServices.class);
    }

    public ListTraces listTraces(String service) throws IOException,
            InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(jaegerBaseUrl +
                "/api/traces?service=" + service + "&limit=1500&start=" + startTimestamp + "&end=" + endTimestamp))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        var traces = new Gson().fromJson(response.body(), ListTraces.class);

        if (traces.getData().length < traces.getTotal()) {
            System.out.println("Warning: " + traces.getTotal() + " traces found, but only " + traces.getData().length
                    + " were returned. Need to paginate.");
        }

        return traces;
    }
}