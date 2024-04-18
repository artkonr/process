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

public class Data {
    private final byte[] data;

    public boolean isEmpty() {
        return data == null;
    }

    public Optional<byte[]> get() {
        return Optional.ofNullable(data);
    }

    public Optional<String> encode(@NonNull Charset encoding) {
        return get().map(bytes -> new String(bytes, encoding));
    }

    public Optional<String> encode() {
        return encode(StandardCharsets.UTF_8);
    }

    public <I> Optional<Result<I, EncodingException>> encode(@NonNull Encoder<I> encoder) {
        return get().map(encoder::encode);
    }

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

    Data(byte[] data) {
        this.data = data != null && data.length > 0 ? data : null;
    }

}
