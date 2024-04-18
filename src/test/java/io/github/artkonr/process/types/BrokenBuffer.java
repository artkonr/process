package io.github.artkonr.process.types;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BrokenBuffer extends BufferedInputStream {

    private final IOException thrown;
    public BrokenBuffer(InputStream in, IOException thrown) {
        super(in);
        this.thrown = thrown;
    }

    @Override
    public synchronized int read() throws IOException {
        throw thrown;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        throw thrown;
    }

    @Override
    public int read(byte[] b) throws IOException {
        throw thrown;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        throw thrown;
    }
}
