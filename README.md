# Benchmarking JavaScript Engines for Java

This project provides a simple benchmark of three popular JavaScript engines for Java: J2V8, Nashorn, and GraalJS.

## Usage

To run the benchmark on JDK 17:


```bash
./gradlew run
```

On Windows, use:


```bash
./gradlew.bat run
```

To run the benchmark on JDK 17 with Graal JIT compiler:


```bash
./gradlew runWithGraalJIT
```

On Windows, use:


```bash
./gradlew.bat runWithGraalJIT
```

If you prefer not to run the benchmark on your local machine, 
you can fork this repository and run it using GitHub Actions.