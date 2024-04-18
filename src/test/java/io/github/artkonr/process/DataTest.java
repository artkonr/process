package io.github.artkonr.process;

import io.github.artkonr.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataTest {

    Path home = Paths.get(System.getenv("HOME"));
    Data data = new Data(new byte[]{ 97, 98, 99 }); // stands for "abc"
    Data empty = new Data(null);

    @Test
    void isEmpty() {
        assertFalse(data.isEmpty());
        assertTrue(empty.isEmpty());
        assertTrue(new Data(new byte[0]).isEmpty());
    }

    @Test
    void get_empty() {
        assertTrue(empty.get().isEmpty());
    }

    @Test
    void get() {
        byte[] expected = { 97, 98, 99 };
        assertTrue(data.get().isPresent());
        assertArrayEquals(expected, data.get().get());
    }

    @Test
    void encode() {
        String expected = "abc";

        Optional<String> result = data.encode();
        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
    }

    @Test
    void encode_empty() {
        Optional<String> result = empty.encode();
        assertTrue(result.isEmpty());
    }

    @Test
    void encode_with_charset() {
        String utf8String = "mötorhead";
        byte[] utf8bytes = utf8String.getBytes(StandardCharsets.UTF_8);

        Data data = new Data(utf8bytes);
        Optional<String> result = data.encode(StandardCharsets.US_ASCII);
        assertTrue(result.isPresent());
        assertNotEquals(utf8bytes, result.get().getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void encode_empty_with_charset() {
        Optional<String> result = empty.encode(StandardCharsets.US_ASCII);
        assertFalse(result.isPresent());
    }

    @Test
    void encode_with_charset_null_arg() {
        assertThrows(IllegalArgumentException.class, () -> data.encode((Charset) null));
    }

    @Test
    void encode_custom_empty() {
        var result = empty.encode(new OkEncoder());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void encode_custom_ok() {
        var result = data.encode(new OkEncoder());
        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().isOk());
        Assertions.assertEquals("abc", result.get().get());
    }

    @Test
    void encode_custom_err() {
        EncodingException err = new EncodingException("nay");
        var result = data.encode(bytes -> Result.err(err));
        assertTrue(result.isPresent());
        assertTrue(result.get().isErr());
        assertSame(err, result.get().getErr());
    }

    @Test
    void encode_custom_null_arg() {
        assertThrows(IllegalArgumentException.class, () -> data.encode((Encoder<?>) null));
    }

    @Test
    void dumpTo_null_arg() {
        assertThrows(IllegalArgumentException.class, () -> data.dumpTo(null));
    }

    @Test
    void dumpTo_ok() throws IOException {
        UUID id = UUID.randomUUID();
        Path file = home.resolve(id.toString());
        var result = data.dumpTo(file);
        assertTrue(result.isOk());

        try {
            byte[] read = Files.readAllBytes(file);
            assertArrayEquals(read, data.get().get());
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void dumpTo_existing() throws IOException {
        UUID id = UUID.randomUUID();
        Path file = home.resolve(id.toString());

        Files.createFile(file);

        var result = data.dumpTo(file);
        assertTrue(result.isOk());

        try {
            byte[] read = Files.readAllBytes(file);
            assertArrayEquals(read, data.get().get());
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void dumpTo_truncate() throws IOException {
        UUID id = UUID.randomUUID();
        Path file = home.resolve(id.toString());

        Files.createFile(file);
        Files.writeString(file, "previous text");

        var result = data.dumpTo(file);
        assertTrue(result.isOk());

        try {
            byte[] read = Files.readAllBytes(file);
            assertArrayEquals(read, data.get().get());
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void dumpTo_err() {
        Path nonExistentFile = home.resolve("nonExistentDir/file.txt");
        var result = data.dumpTo(nonExistentFile);
        assertTrue(result.isErr());
    }

    static class OkEncoder implements Encoder<String> {
        @Override
        public Result<String, EncodingException> encode(byte[] data) {
            return Result.ok(new String(data, StandardCharsets.UTF_8));
        }
    }

}