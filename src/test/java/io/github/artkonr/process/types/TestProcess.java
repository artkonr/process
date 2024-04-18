package io.github.artkonr.process.types;

import lombok.Builder;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Builder
public class TestProcess extends Process {

    @Builder.Default
    private int exitcode = 0;
    private String stdout;
    private String stderr;
    private Failure failure;

    public enum Failure {
        WAIT_FOR_COMPLETION_INTERRUPTED,
        STREAM_READ_ERR,
        READER_CLOSE_ERR,
        GENERIC_ERROR_INJECTED
    }


    @Override
    public OutputStream getOutputStream() {
        return null;
    }

    private InputStream getStream(boolean stdout) {
        String content = stdout ? this.stdout : this.stderr;
        byte[] arr = content != null
                ? content.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        InputStream stream = new ByteArrayInputStream(arr);
        boolean readerErr = failure != null
                && (failure == Failure.STREAM_READ_ERR || failure == Failure.READER_CLOSE_ERR);
        if (readerErr) {
            return new BrokenBuffer(
                    stream,
                    new IOException("failed to read " + (stdout ? "stdout" : "stderr"))
            );
        } else {
            return new BufferedInputStream(stream);
        }
    }

    @Override
    public InputStream getInputStream() {
        return getStream(true);
    }

    @Override
    public InputStream getErrorStream() {
        return getStream(false);
    }

    @Override
    public int waitFor() throws InterruptedException {
        if (failure != null) {
            if (failure == Failure.WAIT_FOR_COMPLETION_INTERRUPTED) {
                throw new InterruptedException("imma done waiting");
            } else if (failure == Failure.GENERIC_ERROR_INJECTED) {
                throw new RuntimeException("generic");
            }
        }
        return exitcode;
    }

    @Override
    public int exitValue() {
        return exitcode;
    }

    @Override
    public long pid() {
        return 914;
    }

    @Override
    public void destroy() {
    }
}
