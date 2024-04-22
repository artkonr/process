package io.github.artkonr.process;

/**
 * A simple exception that represents CLI program failures
 *  or failures to process their output.
 */
public class CmdException extends RuntimeException {

    /**
     * Creates a new instance.
     * @param expected expected exit code
     * @param cmd invoked command
     * @param actual actual exit code
     * @param output program output
     * @return new instance
     */
    public static CmdException errorExitCode(int expected,
                                             String cmd,
                                             int actual,
                                             String output) {
        String msg = "command failed with unexpected exitcode: cmd='%s' expected=%d actual=%d message='%s'".formatted(
                cmd,
                expected,
                actual,
                output
        );
        return new CmdException(msg);
    }

    /**
     * Wraps the provided exception into a {@link CmdException},
     *  unless it already is a {@link CmdException}.
     * @param ex exception to wrap
     * @return same or wrapped instance
     */
    public static CmdException wrap(Exception ex) {
        if (ex instanceof CmdException cmdEx) {
            return cmdEx;
        } else {
            return new CmdException(null, ex);
        }
    }

    /**
     * Creates new exception instance.
     * @param message message
     */
    public CmdException(String message) {
        super(message);
    }

    /**
     * Creates new exception instance.
     * @param message message
     * @param cause root cause
     */
    public CmdException(String message, Throwable cause) {
        super(message, cause);
    }

}
