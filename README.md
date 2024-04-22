# Process

A simplistic toolkit to invoke command-line applications and handle their output idiomatically.

**License**: Apache License 2.0 - this library is unconditionally open-source.

## Getting started

Library installation is available for [Maven](#maven) and [Gradle](#gradle) and targets Java 17.

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>io.github.artkonr</groupId>
        <artifactId>process</artifactId>
        <version>${versions.process}</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
depencencies {
    implementation 'io.github.artkonr:process:${processVersion}'
}
```

## Cookbook


### Invoking a single program

Working with `process` is a breeze:

```java
import io.github.artkonr.process.*;
import io.github.artkonr.result.Result;
import java.util.Optional;

public class Program {
    public static void main(String[] args) {
        Output invocation = Cmd.from("whoami")
                .invoke().unwrap();
        assert invocation.exitedNormally();
        Optional<String> username = invocation.stdout().encode();
        assert username.isPresent();
        System.out.println("nice to meet you, " + username.get());
    }
}
```

### Checking program errors

`process` by default checks the exit code of a program and wraps into an idiomatic error container if it is non-zero. The details of the failure are taken from stderr.

```java
import io.github.artkonr.process.*;
import io.github.artkonr.result.Result;
import java.util.Optional;

public class Program {
    public static void main(String[] args) {
        // we know for a fact that calling `curl`
        // with no arguments will result in a non-0
        // exitcode + some help info
        Result<Output, CmdException> invocation = Cmd.from("curl").invoke();
        assert invocation.isErr();
        
        CmdException curlErr = invocation.getErr();
        String message = curlErr.getMessage();
        
        // in the exception, we capture the failed
        // command, expected and actual exit codes
        // and there is also stderr output of the
        // program
        String expected =
                """
                command failed with unexpected exitcode: \
                cmd='curl' \
                expected=0 actual=2 \
                message='curl: try 'curl --help' or 'curl --manual' for more information'\
                """;
        assert message.equals(expected);
    }
}
```

### Consuming stderr

Some programs write to stderr in non-error cases, which you might want to capture. Consider this example with `wget`: 

```java
import io.github.artkonr.process.*;
import io.github.artkonr.result.Result;
import java.util.Optional;

public class Program {
    public static void main(String[] args) {
        // we know for a fact that `wget` writes
        // the log of download to stderr
        Output invocation = Cmd.from("wget", "https://www.google.com")
                .invoke()
                .unwrap();
        
        Optional<String> stderr = invocation.stderr().encode();
        assert stderr.isPresent();
        assert stderr.get().contains("Connecting to www.google.com");
    }
}
```

### Chaining commands

Naturally, with CLIs you want to chain commands:

```java
import io.github.artkonr.process.*;
import io.github.artkonr.result.Result;
import java.util.Optional;

public class Program {
    public static void main(String[] args) {
        Output invocation = Chain.from("pwd")
                .pipeTo("tr", "-d", "/")
                .invoke()
                .unwrap();
        
        Optional<String> stdout = invocation.stdout().encode();
        assert stdout.isPresent();
        assert !stdout.get().contains("/"); // using `tr` we removed all slashes
    }
}
```

## Building

The library is built with Maven:

```shell
mvn clean package
```
