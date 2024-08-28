package io.github.artkonr.process;

import java.util.Optional;

/**
 * Abstract program output.
 */
public interface Output {

    /**
     * Converts {@code this} instance into another
     *  {@link Output instance} with all actual
     *  output stripped.
     * @return new instance with no output
     */
    Output devnull();

    /**
     * PID of the completed process.
     * @return PID
     */
    long pid();

    /**
     * Invoked command.
     * @return invoked command
     */
    String command();

    /**
     * Process exit code
     * @return exit code
     */
    int exitcode();

    /**
     * Checks if the status code corresponds to what convention
     *  states to be an "ok" status code.
     * @return {@code true} if the exitcode is zero; {@code false} otherwise
     */
    default boolean exitedNormally() {
        return exitcode() == 0;
    }

    default boolean exitedWithError() {
        return !exitedNormally();
    }

    /**
     * Checks if {@code this} instance has any output.
     * @return {@code true} if {@code this} instance does not contain
     *  any stdout or stderr
     */
    default boolean isEmpty() {
        return stdout().isEmpty() && stderr().isEmpty();
    }

    /**
     * Program stdout {@link Data output}.
     * @return stdout
     */
    Data stdout();

    /**
     * Program stderr {@link Data output}.
     * @return stderr
     */
    Data stderr();

    /**
     * Introspects {@code this} instance's state to
     *  see if it is an error.
     * @return an empty {@link Optional} if {@link Output#exitedNormally() exited with zero};
     *  an optional bearing {@code this} instance otherwise
     */
    default Optional<Output> error() {
        if (exitedNormally()) {
            return Optional.empty();
        }

        return Optional.of(this);
    }

}
