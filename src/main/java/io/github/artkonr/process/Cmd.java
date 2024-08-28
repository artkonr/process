package io.github.artkonr.process;

import io.github.artkonr.result.Result;
import lombok.NonNull;


import static io.github.artkonr.process.Util.*;

/**
 * A {@link Shell} implementation that invokes a single
 *  program with its arguments.
 */
public class Cmd implements Shell {

    /**
     * Wrapped {@link ProcessBuilder}
     */
    final ProcessBuilder handle;

    /**
     * Factory method. Creates a new {@link Cmd command}.
     * @param pb java {@link ProcessBuilder}
     * @return new instance
     * @throws IllegalArgumentException if no argument provided
     */
    public static Cmd from(@NonNull ProcessBuilder pb) {
        return new Cmd(pb);
    }

    /**
     * Factory method. Creates a new {@link Cmd command}.
     * @param exec program name
     * @param arguments program arguments, nullable
     * @return new instance
     * @throws IllegalArgumentException if no program name provided
     */
    public static Cmd from(@NonNull String exec, String... arguments) {
        return from(formulate(exec, arguments));
    }

    /**
     * {@inheritDoc}
     * @param pb program
     * @return {@link Chain piped} program chain
     * @throws IllegalArgumentException if no argument provided
     */
    @Override
    public Chain pipeTo(@NonNull ProcessBuilder pb) {
        return Chain.from(this).pipeTo(pb);
    }

    /**
     * {@inheritDoc}
     * @param exec program
     * @param arguments program arguments, nullable
     * @return {@link Chain piped} program chain
     * @throws IllegalArgumentException if no program name provided
     */
    @Override
    public Chain pipeTo(@NonNull String exec, String... arguments) {
        return Chain.from(this).pipeTo(formulate(exec, arguments));
    }

    /**
     * {@inheritDoc}
     * @param command program
     * @return {@link Chain piped} program chain
     * @throws IllegalArgumentException if no argument provided
     */
    @Override
    public Chain pipeTo(@NonNull Cmd command) {
        return Chain.from(this).pipeTo(command);
    }

    /**
     * {@inheritDoc}
     * @return invocation {@link Result}
     */
    @Override
    public Result<io.github.artkonr.process.Output, CmdException> invoke() {
        return handle(
                Result.wrap(handle::start),
                getCmd(handle)
        );
    }

    /**
     * {@link Output Output} of a single {@link Cmd command}.
     */
    public static class Output implements io.github.artkonr.process.Output {

        private final long pid;
        private final String cmd;
        private final int exitcode;
        private final Data stdout;
        private final Data stderr;

        /**
         * {@inheritDoc}
         * @return PID
         */
        @Override
        public long pid() {
            return pid;
        }

        /**
         * {@inheritDoc}
         * @return invoked command
         */
        @Override
        public String command() {
            return cmd;
        }

        /**
         * {@inheritDoc}
         * @return exit code
         */
        @Override
        public int exitcode() {
            return exitcode;
        }

        /**
         * {@inheritDoc}
         * @return new output with no stdout or stderr
         */
        @Override
        public Cmd.Output devnull() {
            return new Cmd.Output(pid, cmd, exitcode, new Data(null), new Data(null));
        }

        /**
         * {@inheritDoc}
         * @return stdout
         */
        @Override
        public Data stdout() {
            return stdout;
        }

        /**
         * {@inheritDoc}
         * @return stderr
         */
        @Override
        public Data stderr() {
            return stderr;
        }

        /**
         * Factory method.
         * @param pid PID
         * @param cmd command
         * @param exitcode exit code
         * @param stdout stdout
         * @param stderr stderr
         * @return new instance
         */
        static io.github.artkonr.process.Output from(long pid,
                                                     String cmd,
                                                     int exitcode,
                                                     byte[] stdout,
                                                     byte[] stderr) {
            return new Output(pid, cmd, exitcode, new Data(stdout), new Data(stderr));
        }

        private Output(long pid, String cmd, int exitcode, Data stdout, Data stderr) {
            this.pid = pid;
            this.cmd = cmd;
            this.exitcode = exitcode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    /**
     * Handles the command invocation.
     * @param result handled result
     * @param cmd invoked command
     * @return invocation {@link Result}
     */
    static Result<io.github.artkonr.process.Output, CmdException> handle(Result<Process, Exception> result,
                                                                         String cmd) {
        return result
                .flatMap(process -> Result
                        .wrap(InterruptedException.class, process::waitFor)
                        .upcast()
                        .flatMap(exitCode ->
                                 read(process.inputReader())
                                .fuse(read(process.errorReader()))
                                .map(fuse -> Output.from(
                                        process.pid(),
                                        cmd,
                                        exitCode,
                                        fuse.left(),
                                        fuse.right()
                                ))
                        )
                )
                .mapErr(ex -> new CmdException("command failed", ex))
                .fork(
                        io.github.artkonr.process.Output::exitedWithError,
                        output -> CmdException.errorExitCode(
                                0,
                                output.command(),
                                output.exitcode(),
                                output.stderr().encode().orElse("n/a")
                        )
                );
    }

    /**
     * Default constructor.
     * @param handle proccess builder
     */
    Cmd(ProcessBuilder handle) {
        this.handle = handle;
    }
}
