package io.github.artkonr.process;

public class CmdException extends RuntimeException {

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

    public static CmdException wrap(Exception ex) {
        if (ex instanceof CmdException cmdEx) {
            return cmdEx;
        } else {
            return new CmdException(null, ex);
        }
    }

    public CmdException(String message) {
        super(message);
    }

    public CmdException(String message, Throwable cause) {
        super(message, cause);
    }

}
