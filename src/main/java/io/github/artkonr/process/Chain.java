package io.github.artkonr.process;

import io.github.artkonr.result.Result;
import io.github.artkonr.result.TakeFrom;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.github.artkonr.process.Cmd.handle;
import static io.github.artkonr.process.Util.formulate;
import static io.github.artkonr.process.Util.getCmd;

/**
 * A {@link Shell} implementation that invokes
 *  a pipeline of commands.
 */
public class Chain implements Shell {

    /**
     * Pipeline consisting of {@link ProcessBuilder} instances.
     */
    final List<ProcessBuilder> pipeline = new ArrayList<>();

    /**
     * Factory method. Creates a new {@link Chain}.
     * @param pb java {@link ProcessBuilder}
     * @return new instance
     * @throws IllegalArgumentException if no argument provided
     */
    public static Chain from(@NonNull ProcessBuilder pb) {
        return new Chain(pb);
    }

    /**
     * Factory method. Creates a new {@link Chain}.
     * @param exec program name
     * @param arguments program arguments, nullable
     * @return new instance
     * @throws IllegalArgumentException if no program name provided
     */
    public static Chain from(@NonNull String exec, String... arguments) {
        return from(formulate(exec, arguments));
    }

    /**
     * Factory method. Creates a new {@link Chain}.
     * @param command {@link Cmd}
     * @return new instance
     * @throws IllegalArgumentException if no argument provided
     */
    public static Chain from(@NonNull Cmd command) {
        return from(command.handle);
    }

    /**
     * {@inheritDoc}
     * @param pb program
     * @return this instance
     * @throws IllegalArgumentException if no argument provided
     */
    @Override
    public Chain pipeTo(@NonNull ProcessBuilder pb) {
        pipeline.add(pb);
        return this;
    }

    /**
     * {@inheritDoc}
     * @param exec program
     * @param arguments program arguments, nullable
     * @return this instance
     * @throws IllegalArgumentException if no argument provided
     */
    @Override
    public Chain pipeTo(@NonNull String exec, String... arguments) {
        return pipeTo(formulate(exec, arguments));
    }

    /**
     * {@inheritDoc}
     * @param command program
     * @return this instance
     * @throws IllegalArgumentException if no argument provided
     */
    @Override
    public Chain pipeTo(@NonNull Cmd command) {
        return pipeTo(command.handle);
    }

    /**
     * {@inheritDoc}
     * @return invocation {@link Result}
     */
    @Override
    public Result<io.github.artkonr.process.Output, CmdException> invoke() {
        Result<List<Process>, Exception> invoked = Result
                .wrap(() -> ProcessBuilder.startPipeline(pipeline));

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

    /**
     * {@link Cmd.Output Output} of a {@link Chain pipeline}.
     */
    public static class Output implements io.github.artkonr.process.Output {

        private final List<io.github.artkonr.process.Output> intermediate;
        private final io.github.artkonr.process.Output fin;

        /**
         * {@inheritDoc}
         * @return new output with no stdout or stderr
         */
        @Override
        public io.github.artkonr.process.Output devnull() {
            var intermediate = this.intermediate.stream()
                    .map(io.github.artkonr.process.Output::devnull)
                    .toList();
            return new Output(intermediate, fin.devnull());
        }

        /**
         * {@inheritDoc}
         * @return PID of the last invoked command
         */
        @Override
        public long pid() {
            return fin.pid();
        }

        /**
         * {@inheritDoc}
         * @return invoked command
         */
        @Override
        public String command() {
            String intermediate = this.intermediate.stream()
                    .map(io.github.artkonr.process.Output::command)
                    .collect(Collectors.joining(" | "));
            return intermediate + " | " + fin.command();
        }

        /**
         * Returns exitcode of the last program in the pipeline.
         * @return exitcode of the last invoked program
         */
        @Override
        public int exitcode() {
            return fin.exitcode();
        }

        /**
         * Checks if the last invoked program has written
         *  something into stdout or stderr.
         * @return {@code true} if there is any program output; {@code false} if otherwise
         */
        @Override
        public boolean isEmpty() {
            return fin.isEmpty();
        }

        /**
         * Introspects the output of each program invoked from within
         *  the pipeline and returns the first found program that did
         *  not exit normally.
         * @return optional pipeline {@link io.github.artkonr.process.Output error}
         */
        @Override
        public Optional<io.github.artkonr.process.Output> error() {
            return Stream.concat(intermediate.stream(), Stream.of(fin))
                    .filter(item -> !item.exitedNormally())
                    .findFirst();
        }

        /**
         * Checks if all programs in the pipeline have exited normally.
         * @return {@code true} if all programs in the pipeline returned
         *  exitcode 0; {@code false} if otherwise
         */
        @Override
        public boolean exitedNormally() {
            return Stream
                    .concat(intermediate.stream(), Stream.of(fin))
                    .allMatch(io.github.artkonr.process.Output::exitedNormally);
        }

        /**
         * {@inheritDoc}
         * @return stdout
         */
        @Override
        public Data stdout() {
            return fin.stdout();
        }

        /**
         * {@inheritDoc}
         * @return stderr
         */
        @Override
        public Data stderr() {
            return fin.stderr();
        }

        /**
         * Factory method
         * @param fin output of the last invoked program
         * @param intermediate results of intermediate programs
         * @return new pipeline output
         */
        static io.github.artkonr.process.Output from(io.github.artkonr.process.Output fin,
                                                     Collection<io.github.artkonr.process.Output> intermediate) {
            return new Output(new ArrayList<>(intermediate), fin);
        }

        private Output(List<io.github.artkonr.process.Output> intermediate,
                       io.github.artkonr.process.Output fin) {
            this.intermediate = intermediate;
            this.fin = fin;
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
