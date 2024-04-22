package io.github.artkonr.process;

import io.github.artkonr.result.Result;

/**
 * An abstract interface to interact with a CLI.
 */
public interface Shell {

    /**
     * Invokes the program with all arguments and collects its output.
     * @return {@link Result} bearing command's output
     */
    Result<Output, CmdException> invoke();

    /**
     * Pipes {@code this} instance into an invocation of another program.
     * @param pb program
     * @return {@link Chain piped} invocation
     */
    Chain pipeTo(ProcessBuilder pb);

    /**
     * Pipes {@code this} instance into an invocation of another program.
     * @param exec program name
     * @param arguments program args
     * @return {@link Chain piped} invocation
     */
    Chain pipeTo(String exec, String... arguments);

    /**
     * Pipes {@code this} instance into an invocation of another program.
     * @param command program
     * @return {@link Chain piped} invocation
     */
    Chain pipeTo(Cmd command);

}
