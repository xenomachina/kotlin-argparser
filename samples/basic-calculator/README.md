# Calculator CLI

This sample is a basic calculator that works via CLI.

## Usage

**1. Generate the JAR file**

```
$ ./gradlew jar
```

**2. Run the code with the parameters**

```
$ java -jar build/libs/sample-1.0.jar --show-result --sum -n 4 -n 6
```

**Output**
```
$ The SUM of [4, 6] is equal to 10
```

All available parameters can be found in [this file](src/main/kotlin/Main.kt)
