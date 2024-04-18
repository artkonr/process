package io.github.artkonr.process;

import io.github.artkonr.result.Result;
import io.github.artkonr.result.TakeFrom;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.github.artkonr.process.Cmd.*;
import static io.github.artkonr.process.Util.*;
import static io.github.artkonr.process.Util.formulate;

@Slf4j
public class Chain implements Shell {

    @Getter(value = AccessLevel.PACKAGE)
    private final List<ProcessBuilder> pipeline = new ArrayList<>();

    public static Chain from(@NonNull ProcessBuilder pb) {
        return new Chain(pb);
    }

    public static Chain from(@NonNull String exec, String... arguments) {
        return from(formulate(exec, arguments));
    }

    public static Chain from(@NonNull Cmd command) {
        return from(command.handle);
    }

    @Override
    public Chain pipeTo(@NonNull ProcessBuilder pb) {
        pipeline.add(pb);
        return this;
    }

    @Override
    public Chain pipeTo(@NonNull String exec, String... arguments) {
        return pipeTo(formulate(exec, arguments));
    }

    @Override
    public Chain pipeTo(@NonNull Cmd command) {
        return pipeTo(command.handle);
    }

    @Override
    public Result<io.github.artkonr.process.Output, CmdException> invoke() {
        Result<List<Process>, IOException> invoked = Result
                .wrap(IOException.class, () -> ProcessBuilder.startPipeline(pipeline));

        int endI = pipeline.size() - 1;
        var fin = handle(
                invoked.map(processes -> processes.get(endI)),
                getCmd(pipeline.get(endI))
        );
        var intermediate = invoked
                .map(processes -> processes.subList(0, endI))
                .mapErr(CmdException::wrap)
                .flatMap(processes -> handleIntermediate(processes, endI));
        return fin
                .fuse(intermediate, TakeFrom.TAIL)
                .map(fuse -> Output.from(fuse.left(), fuse.right()));
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static class Output implements io.github.artkonr.process.Output {

        private final List<io.github.artkonr.process.Output> intermediate;
        private final io.github.artkonr.process.Output fin;

        static io.github.artkonr.process.Output from(io.github.artkonr.process.Output fin,
                                                     Collection<io.github.artkonr.process.Output> intermediate) {
            return new Output(new ArrayList<>(intermediate), fin);
        }

        @Override
        public io.github.artkonr.process.Output devnull() {
            var intermediate = this.intermediate.stream()
                    .map(io.github.artkonr.process.Output::devnull)
                    .toList();
            return new Output(intermediate, fin.devnull());
        }

        @Override
        public long pid() {
            return fin.pid();
        }

        @Override
        public String command() {
            String intermediate = this.intermediate.stream()
                    .map(io.github.artkonr.process.Output::command)
                    .collect(Collectors.joining(" | "));
            return intermediate + " | " + fin.command();
        }

        @Override
        public int exitcode() {
            return fin.exitcode();
        }

        @Override
        public boolean isEmpty() {
            return fin.isEmpty();
        }

        @Override
        public Optional<io.github.artkonr.process.Output> error() {
            return Stream.concat(intermediate.stream(), Stream.of(fin))
                    .filter(item -> !item.exitedNormally())
                    .findFirst();
        }

        @Override
        public boolean exitedNormally() {
            return Stream
                    .concat(intermediate.stream(), Stream.of(fin))
                    .allMatch(io.github.artkonr.process.Output::exitedNormally);
        }

        @Override
        public Data stdout() {
            return fin.stdout();
        }

        @Override
        public Data stderr() {
            return fin.stderr();
        }
    }

    private Chain(ProcessBuilder first) {
        this.pipeline.add(first);
    }

    private Result<List<io.github.artkonr.process.Output>, CmdException> handleIntermediate(List<Process> processes,
                                                                                            int toIndex) {
        return IntStream.range(0, toIndex)
                .mapToObj(idx -> handle(
                        Result.ok(processes.get(idx)),
                        getCmd(pipeline.get(idx))
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        Result::join
                ));
    }
}
