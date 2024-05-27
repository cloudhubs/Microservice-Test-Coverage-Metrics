package cloudhubs.analysis.dynamic;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

public final class App {
    static boolean isGetMethod(MethodDeclaration method) {
        return method.getParameters().isEmpty() && !Objects.equals(method.getType().asString(), "void")
                && (method.getNameAsString().startsWith("get")
                        || (method.getNameAsString().startsWith("is")
                                && Objects.equals(method.getType().asString(), "boolean")));
    }

    static boolean isSetMethod(MethodDeclaration method) {
        return method.getParameters().size() == 1 && Objects.equals(method.getType().asString(), "void")
                && method.getNameAsString().startsWith("set");
    }

    static String getMicroserviceName(File sourceFile) {
        String microserviceName = null;
        while (sourceFile.getParentFile() != null) {
            var parentFile = sourceFile.getParentFile();
            var pomFile = Arrays.stream(parentFile.listFiles(x -> x.getName().equals("pom.xml"))).findFirst()
                    .orElse(null);
            if (pomFile != null) {
                try {
                    var pomFileContent = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomFile);
                    var possibleNames = pomFileContent.getElementsByTagName("artifactId");
                    for (int i = 0; i < possibleNames.getLength(); i++) {
                        var possibleName = possibleNames.item(i);
                        if (possibleName.getParentNode().getNodeName() == "project" && possibleName.getParentNode()
                                .getParentNode() == possibleName.getParentNode().getOwnerDocument()) {
                            microserviceName = possibleName.getTextContent();
                            break;
                        }
                    }
                } catch (SAXException | IOException | ParserConfigurationException e) {
                    System.err.println("Error parsing pom.xml, microservice name cannot be detected");
                    System.err.println(e.getMessage());
                }
                break;
            }
            sourceFile = sourceFile.getParentFile();
        }

        return microserviceName;
    }

    public static void main(String[] args) throws IOException {
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
        var config = new ObjectMapper().readValue(configFile, Config.class);
        var rootDir = new File(config.getRootPath());
        var ignoreDirs = config.getIgnoreDirs() == null ? new String[0] : config.getIgnoreDirs();

        var javaFiles = FileUtils.listFiles(rootDir, new String[] { "java" }, true);
        javaFiles.removeIf(f -> {
            for (var ignoreDir : ignoreDirs) {
                if (f.getPath().contains(rootDir.getPath() + File.separator + ignoreDir)) {
                    return true;
                }
            }
            return false;
        });

        var outputs = new HashMap<String, StringBuilder>();

        for (var file : javaFiles) {
            var compilationUnit = StaticJavaParser.parse(file);
            var packageDeclaration = compilationUnit.findFirst(PackageDeclaration.class);
            var microserviceName = getMicroserviceName(file);
            if (!outputs.containsKey(microserviceName)) {
                outputs.put(microserviceName, new StringBuilder());
            }
            compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream().forEach(c -> {
                var className = c.getNameAsString();

                var fullClassName = packageDeclaration.get().getNameAsString() + "." + className;

                var methodNames = new HashSet<String>();

                if (config.isAddConstructors())
                    methodNames.add(className);

                for (var method : c.getMethods().stream().filter(
                        m -> (config.isAddGetters() || !isGetMethod(m)) && (config.isAddSetters() || !isSetMethod(m))
                                && (config.isAddConstructors() || !m.isConstructorDeclaration()))
                        .toList()) {
                    methodNames.add(method.getNameAsString());
                }

                var addGetters = c.getAnnotationByClass(lombok.Getter.class).isPresent() || c
                        .getAnnotationByClass(lombok.Data.class).isPresent();
                var addSetters = c.getAnnotationByClass(lombok.Setter.class).isPresent() || c
                        .getAnnotationByClass(lombok.Data.class).isPresent();

                if (addGetters && config.isAddGetters())
                    for (var field : c.getFields()) {
                        var fieldName = field.getVariable(0).getNameAsString();
                        var getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        methodNames.add(getterName);
                    }

                if (addSetters && config.isAddSetters())
                    for (var field : c.getFields()) {
                        var fieldName = field.getVariable(0).getNameAsString();
                        var setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        methodNames.add(setterName);
                    }

                if (!methodNames.isEmpty()) {
                    var classEntry = fullClassName + "[" + String.join(",", methodNames) + "]";
                    outputs.get(microserviceName).append(classEntry + ";");
                }
            });
        }

        // Mapper with default configuration
        ObjectMapper mapper = new YAMLMapper();
        mapper.writeValue(new File(config.getOutputFile()), outputs);
    }
}
