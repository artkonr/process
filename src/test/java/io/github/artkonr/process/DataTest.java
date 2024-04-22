package io.github.artkonr.process;

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
        assertThrows(IllegalArgumentException.class, () -> data.encode(null));
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

}