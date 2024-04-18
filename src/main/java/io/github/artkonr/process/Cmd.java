package io.github.artkonr.process;

import io.github.artkonr.result.Result;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static io.github.artkonr.process.Util.*;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public class Cmd implements Shell {

    final ProcessBuilder handle;

    public static Cmd from(@NonNull ProcessBuilder pb) {
        return new Cmd(pb);
    }

    public static Cmd from(@NonNull String exec, String... arguments) {
        return from(formulate(exec, arguments));
    }

    @Override
    public Chain pipeTo(@NonNull ProcessBuilder pb) {
        return Chain.from(this).pipeTo(pb);
    }

    @Override
    public Chain pipeTo(@NonNull String exec, String... arguments) {
        return Chain.from(this).pipeTo(formulate(exec, arguments));
    }

    @Override
    public Chain pipeTo(Cmd command) {
        return Chain.from(this).pipeTo(command);
    }

    @Override
    public Result<io.github.artkonr.process.Output, CmdException> invoke() {
        return handle(
                Result.wrap(IOException.class, handle::start),
                getCmd(handle)
        );
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Output implements io.github.artkonr.process.Output {

        private final long pid;
        private final String cmd;
        private final int exitcode;
        private final Data stdout;
        private final Data stderr;

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public String command() {
            return cmd;
        }

        @Override
        public int exitcode() {
            return exitcode;
        }

        @Override
        public Cmd.Output devnull() {
            return new Cmd.Output(pid, cmd, exitcode, new Data(null), new Data(null));
        }

        @Override
        public Data stdout() {
            return stdout;
        }

        @Override
        public Data stderr() {
            return stderr;
        }

        static io.github.artkonr.process.Output from(long pid,
                                                     String cmd,
                                                     int exitcode,
                                                     byte[] stdout,
                                                     byte[] stderr) {
            return new Output(pid, cmd, exitcode, new Data(stdout), new Data(stderr));
        }

    }

    static Result<io.github.artkonr.process.Output, CmdException> handle(Result<Process, IOException> result,
                                                                         String cmd) {
        return result
                .mapErr(CmdException::wrap)
                .flatMap(process -> Result
                        .wrap(InterruptedException.class, process::waitFor)
                        .mapErr(CmdException::wrap)
                        .flatMap(exitcode -> read(process.inputReader())
                                .fuse(read(process.errorReader()))
                                .map(fuse -> Output.from(process.pid(), cmd, exitcode, fuse.left(), fuse.right()))
                        )
                )
                .taint(
                        output -> !output.exitedNormally(),
                        output -> CmdException.errorExitCode(
                                0,
                                output.command(),
                                output.exitcode(),
                                output.stderr().encode().orElse("n/a")
                        )
                )
                .mapErr(CmdException::wrap);
    }

}
