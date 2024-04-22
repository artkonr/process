package io.github.artkonr.process;

import io.github.artkonr.result.FlagResult;
import io.github.artkonr.result.Result;
import lombok.NonNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Output data container with a handy API.
 * <p>Internally, stores the output as a simple byte array.
 */
public class Data {
    private final byte[] data;

    /**
     * Checks if {@code this} instance bears any data.
     * @return {@code true} if there are data present; {@code false} otherwise
     */
    public boolean isEmpty() {
        return data == null;
    }

    /**
     * Safely reads the data.
     * @return internal byte array, wrapped in {@link Optional}
     */
    public Optional<byte[]> get() {
        return Optional.ofNullable(data);
    }

    /**
     * Safely returns the encoded data.
     * @param encoding applied encoding
     * @return internal byte array, encoded accordingly and wrapped in {@link Optional}
     * @throws IllegalArgumentException if no argument provided
     */
    public Optional<String> encode(@NonNull Charset encoding) {
        return get().map(bytes -> new String(bytes, encoding));
    }

    /**
     * Safely returns the encoded data. Implies {@link StandardCharsets#UTF_8 UTF-8}.
     * @return internal byte array, encoded accordingly and wrapped in {@link Optional}
     * @throws IllegalArgumentException if no argument provided
     */
    public Optional<String> encode() {
        return encode(StandardCharsets.UTF_8);
    }

    /**
     * Dumps the data into a file at {@link Path location}.
     * <p>If the file is not present, it is created. Intermediate
     *  directories are not created.
     * @param location target file
     * @return write result as {@link FlagResult}
     */
    public FlagResult<IOException> dumpTo(@NonNull Path location) {
        return Result
                .wrap(
                        IOException.class,
                        () -> Files.write(
                                location,
                                data,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                        ))
                .drop();
    }

    /**
     * Default constructor.
     * @param data byte array
     */
    Data(byte[] data) {
        this.data = data != null && data.length > 0 ? data : null;
    }

}
