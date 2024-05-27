package edu.baylor;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import javassist.*;
import javassist.bytecode.*;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;

import java.io.File;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.*;
import org.jgrapht.nio.json.JSONExporter;
import org.springframework.asm.Opcodes;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.PrintWriter;

@Slf4j
@SpringBootApplication
public class StaticAnalyzerApplication implements CommandLineRunner {
    //private static final String jarPath = "/Users/ashfak/workspace/tms-testbed/tms-cms/target/cms-0.0.1-SNAPSHOT.jar";
    //private static final String prefix = "edu.baylor.ecs.cms.controller";

    private static String currentJarName;
    private static Map<String, Map<String,List>> finalPaths = new HashMap<>();
    //private static  String radResponse = null;
    private static final Graph<String, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    private static List<String> allPossiblePathWithRestTemplate = new ArrayList<>();
    private static List<String> allPossiblePath = new ArrayList<>();

    public static void main(String[] args) {
        SpringApplication.run(StaticAnalyzerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        //String directoryPath = "C:\\Users\\boyle\\Downloads\\Francis\\jars to run"; // Replace with your directory path
        String directoryPath = "C:\\Users\\boyle\\Downloads\\Francis\\jars"; // Replace with your directory path
        String outputPath = "C:\\Users\\boyle\\Downloads\\Francis\\cil-rad.gv";

        List<String> jarPaths = getJarFilesRecursively(directoryPath);


        String url = "http://localhost:8080"; // Replace with your API endpoint URL

        Map<String, String> jsonDataMap = new HashMap<>();
        jsonDataMap.put("pathToCompiledMicroservices", directoryPath);
        jsonDataMap.put("organizationPath", "edu/fudanselab/trainticket");
        jsonDataMap.put("outputPath", outputPath);

        Gson gson = new Gson();
        String jsonBody = gson.toJson(jsonDataMap);



        // Set up the request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Set up the request entity with headers and JSON body
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Make the POST request and get the response
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        // Extract and print the response body
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
        Map<String,Object> restFlowContext = (Map)responseMap.get("restFlowContext");
        List<Map> restFlows = (List<Map>) restFlowContext.get("restFlows");
        Map<String,List<String>> radPaths = new HashMap<>();
        for(Map m : restFlows){
            List<String> nextEndPointPaths = new ArrayList<>();
            List<Map<String,String>> servers = (List<Map<String,String>>) m.get("servers");
            for (Map<String,String> m1 : servers){
                nextEndPointPaths.add(m1.get("className")+"."+m1.get("methodName"));
            }
            radPaths.put(m.get("className")+"."+m.get("methodName"),nextEndPointPaths);
        }

        for (int i=0; i<jarPaths.size(); i++){
            analyze(jarPaths.get(i), "edu.fudanselab.trainticket");
        }

        for(String s: allPossiblePathWithRestTemplate){
            //System.out.println(s);

            if(s.contains("RestTemplate") || s.contains("restTemplate")){
                String[] splittedPaths = s.split(":");
                String currentPath = String.join(":", Arrays.copyOfRange(splittedPaths, 0, splittedPaths.length-1));
                String currentService = splittedPaths[splittedPaths.length-2];
                List<String> possibleCallToNextMs = radPaths.get(currentService) == null? new ArrayList<>():radPaths.get(currentService);
                List<String> matchingStrings = new ArrayList<>();
                for(String s1 : possibleCallToNextMs ){
                    matchingStrings = allPossiblePathWithRestTemplate.stream()
                            .filter(s3 -> s3.contains(s1))
                            .collect(Collectors.toList());
                }
                for(String s1 : matchingStrings){
                    allPossiblePath.add(currentPath+":"+s1);
                }


            }else{
                allPossiblePath.add(s);
            }

            //System.out.println(s);
        }

        String fileName = "C:\\Users\\boyle\\Downloads\\Francis\\java-execution-path\\input.txt"; // Change this to your desired output file name

        try {
            // Create a FileOutputStream to write to the file
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);

            // Create a PrintWriter to write strings to the output file
            PrintWriter printWriter = new PrintWriter(fileOutputStream);

            // Write strings to the output file
            for(String s : allPossiblePath){
                System.out.println(s);
                printWriter.println(s);
            }

            // Close the PrintWriter and FileOutputStream
            printWriter.close();
            fileOutputStream.close();

            System.out.println("Strings have been written to " + fileName);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }



    }

    public static void analyze(String jarPath, String prefix) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        pool.getClass();
        pool.insertClassPath(jarPath);

        Set<String> classNames = getClassNamesFromJarFile(new File(jarPath), prefix);

        for (String cn : classNames) {
            CtClass cc = null;
            CtMethod[] methods = null;
            try {
                cc = pool.get(cn);
                methods = cc.getDeclaredMethods();
                for (CtMethod method : methods) {
                    analyzeControlFlowPaths(method, prefix);
                }
            } catch (Exception e) {
                //e.printStackTrace();
                //continue;
                //log.error(e.getMessage());
            }
        }

        Set<String> sources = new HashSet<>();
        Set<String> sinks = new HashSet<>();

        for (String node : directedGraph.vertexSet()) {
            if (directedGraph.inDegreeOf(node) == 0) {
                sources.add(node);
            }
            if (directedGraph.outDegreeOf(node) == 0) {
                sinks.add(node);
            }
        }

        List<GraphPath<String, DefaultEdge>> paths = new AllDirectedPaths<>(directedGraph)
                .getAllPaths(sources, sinks, true, 100);

        //System.out.println(paths);



        for (GraphPath<String, DefaultEdge> path : paths) {
            String pathAsString  = path.toString();
            if (path.getLength() > 0 || pathAsString.contains("Controller") /*|| path.toString().contains("RestTemplate") /*&& !pathAsString.contains("Qms")*/  ) {
                //System.out.println(path);
                //finalPaths.put(currentJarName,path.getVertexList());
                /*System.out.println(path);
                if(finalPaths.containsKey(currentJarName)){
                    Map<String,List> pathGraph = finalPaths.get(currentJarName);
                }else{
                    String key = path.getVertexList().get(0);
                    List value = path.getVertexList().subList(1,path.getVertexList().size()-1);
                }*/
                /*String s = "";
                List<String> vertexList = path.getVertexList();*/

                allPossiblePathWithRestTemplate.add(path.getVertexList().stream().collect(Collectors.joining(":")));

                /*for(String vertex:  path.getVertexList()){
                    if(vertex.contains("RestTemplate")){
                        String restVertex = vertexList.get(vertexList.size()-2);
                        s = s+vertex;
                    }else{
                        s = s+vertex;
                    }
                }
                System.out.println(s);*/
            }
        }
        //System.out.println();
        //JSONExporter<String, DefaultEdge> exporter = new JSONExporter<>();
        //String json = exporter.exportGraph(paths);

        paths.clear();
        Set<DefaultEdge> edgesToRemove = new HashSet<>(directedGraph.edgeSet());
        directedGraph.removeAllEdges(edgesToRemove);
        Set verticesToRemove = new HashSet<>(directedGraph.vertexSet());
        // Remove all vertices
        directedGraph.removeAllVertices(verticesToRemove);

    }

    public static Set<String> getClassNamesFromJarFile(File targetJarFile, String prefix) throws Exception {
        Set<String> classNames = new HashSet<>();
        try (JarFile jarFile = new JarFile(targetJarFile)) {
            currentJarName = jarFile.getName();
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    String className = jarEntry.getName().replace("/", ".").replace(".class", "");
                    if (className.startsWith(prefix)) {
                        classNames.add(className);
                    }
                }
            }
            return classNames;
        }
    }

    private static void analyzeControlFlowPaths(CtMethod method, String  prefix) {
        String currMethodName = method.getLongName().split("\\(")[0];
        directedGraph.addVertex(currMethodName);

        try {
            MethodInfo methodInfo = method.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();

            if (codeAttribute == null) {
                return; // skip methods without code (e.g. abstract, native)
            }

            CodeIterator codeIterator = codeAttribute.iterator();

            while (codeIterator.hasNext()) {
                int position = codeIterator.next();
                int opcode = codeIterator.byteAt(position) & 0xFF;

                int flag = 0;


                if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE) {
                    // This opcode represents an "if statement"
                    // You can add your logic here to handle it
                    // For example, you can print the opcode and its position
//                    System.out.println("if");
                    //sb.append("if");
                    flag = 1;
//                    System.out.println("Found if statement opcode: " + Mnemonic.OPCODE[opcode] + " at position " + position);//  branch = "if";
                }

                //System.out.println("Current method OPCODE for: "+ currMethodName +" "+ Mnemonic.OPCODE[opcode]);
                if (Mnemonic.OPCODE[opcode].equals("invokestatic") ||
                        Mnemonic.OPCODE[opcode].equals("invokevirtual") ||
                        Mnemonic.OPCODE[opcode].equals("invokeinterface")) {

                    int index = codeIterator.u16bitAt(position + 1);
                    ConstPool constPool = codeAttribute.getConstPool();
                    String className = constPool.getMethodrefClassName(index);
                    String methodName = constPool.getMethodrefName(index);
                    String executingMethod = className + "." + methodName;

                    if (executingMethod.startsWith(prefix) || executingMethod.contains("RestTemplate")) {
                        directedGraph.addVertex(executingMethod);
                        directedGraph.addEdge(currMethodName, executingMethod);
                    }
                } else if (flag == 1){

                    int index = codeIterator.u16bitAt(position + 1);
                    ConstPool constPool = codeAttribute.getConstPool();
                    String className = constPool.getMethodrefClassName(index);
                    String methodName = constPool.getMethodrefName(index);
                    String executingMethod = className + "." + methodName;

                    if (executingMethod.startsWith(prefix) || executingMethod.contains("RestTemplate")) {
//                        System.out.println("if");
                        executingMethod = "-" + executingMethod;
                        directedGraph.addVertex(executingMethod);
                        directedGraph.addEdge(currMethodName, executingMethod);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static List<String> getJarFilesRecursively(String directoryPath) {
        List<String> jarFiles = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            searchForJarFiles(directory, jarFiles);
        }

        return jarFiles;
    }

    private static void searchForJarFiles(File directory, List<String> jarFiles) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchForJarFiles(file, jarFiles); // Recurse into subdirectories
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    jarFiles.add(file.getAbsolutePath());
                }
            }
        }
    }

}
