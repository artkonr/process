package io.github.artkonr.process;

import io.github.artkonr.result.Result;

@FunctionalInterface
public interface Encoder<I> {

    Result<I, EncodingException> encode(byte[] data);

}
