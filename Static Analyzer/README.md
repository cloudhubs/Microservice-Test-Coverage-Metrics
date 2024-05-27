# Java Execution Paths

## Build the sample application

```
$ cd sample
$ mvn clean package
```

## Run the analyzer

```
$ mvn spring-boot:run

Execution paths:
[(com.example.MainApplication.run : com.example.Hello.m1), (com.example.Hello.m1 : com.example.Hello.m3), (com.example.Hello.m3 : com.example.Hello.m5)]
[(com.example.MainApplication.run : com.example.Hello.m1), (com.example.Hello.m1 : com.example.Hello.m2), (com.example.Hello.m2 : com.example.Hello.m4)]
```
