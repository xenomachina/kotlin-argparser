# Calculator CLI

This sample is a basic calculator that works via CLI. It shows how to pass parameters of types boolean, string and list.

## Usage

Use the command below in a terminal to compile the project. Make sure you are in the sample folder.

```bash
$ ./gradlew shadowJar
```

It it works, a file named `calculator.jar` will appear in the sample directory.

## Examples

**Sum of 2, 2 and 6**
```text
$ java -jar calculator.jar --sum -n 2 -n 2 -n 6
$ 10
```

**User-friendly multiplication of 3 and 5**
```text
$ java -jar calculator.jar --mul -n 3 -n 5 --pretty
$ The MUL of [3, 5] is equal to 15
```

**Show all options**
```text
$ java -jar calculator.jar --help
```

The complete implementation is available in [this file](src/main/kotlin/Main.kt)
