package io.github.artkonr.process.types;

import java.io.*;

public class BrokenReader extends BufferedReader {

    private final IOException thrown;

    public BrokenReader(InputStream in, IOException thrown) {
        super(new InputStreamReader(in));
        this.thrown = thrown;
    }

    @Override
    public long transferTo(Writer out) throws IOException {
        throw new IOException(thrown);
    }

    @Override
    public void close() throws IOException {
        throw new IOException(thrown);
    }
}
