package io.github.artkonr.process;

import io.github.artkonr.process.types.BrokenBuffer;
import io.github.artkonr.process.types.BrokenReader;
import io.github.artkonr.result.Result;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @Test
    void readByteArray__ok() {
        String data = "abc";
        byte[] result = Util.readByteArray(data.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(
                data.getBytes(StandardCharsets.UTF_8),
                result
        );
    }

    @Test
    void readByteArray__ok__empty_stream() {
        byte[] result = Util.readByteArray(new byte[0]);
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void readByteArray__ok__all_whitespace() {
        InputStream stream = new ByteArrayInputStream(new byte[]{ 10, 10, 10 });
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        byte[] result = Util.readByteArray(new byte[]{ 10, 10, 10 });
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void readByteArray__ok__trims_whitespace_end() {
        String data = "abc\n\t  ";
        byte[] result = Util.readByteArray(data.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(
                "abc".getBytes(StandardCharsets.UTF_8),
                result
        );
    }

    @Test
    void read__err__stream_read() {
        IOException thrown = new IOException("oops");
        InputStream stream = new BrokenBuffer(
                new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)),
                thrown
        );
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        Result<byte[], CmdException> result = Util.read(reader);
        assertTrue(result.isErr());
        assertInstanceOf(CmdException.class, result.getErr());
        assertNotNull(result.getErr().getCause());
        assertSame(thrown, result.getErr().getCause());
    }

    @Test
    void read__err__stream_close() {
        String data = "abc\n\t  ";
        IOException thrown = new IOException("oops");
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BrokenReader(stream, thrown);

        Result<byte[], CmdException> result = Util.read(reader);
        assertTrue(result.isErr());
        assertInstanceOf(CmdException.class, result.getErr());
        assertNotNull(result.getErr().getCause());
        assertInstanceOf(IOException.class, result.getErr().getCause());
    }

    @Test
    void getCmd__single() {
        ProcessBuilder pb = new ProcessBuilder("grep", "-o", "-E", "abc");
        String expected = "grep -o -E abc";
        String actual = Util.getCmd(pb);
        assertEquals(expected, actual);
    }

    @Test
    void getCmd__pipeline() {
        ProcessBuilder p1 = new ProcessBuilder("grep", "-o", "-E", "abc");
        ProcessBuilder p2 = new ProcessBuilder("tr", "-d", "a");
        ProcessBuilder p3 = new ProcessBuilder("wc", "-c");
        String expected = "grep -o -E abc | tr -d a | wc -c";
        String actual = Util.getCmd(List.of(p1, p2, p3));
        assertEquals(expected, actual);
    }

    @Test
    void formulate__single_part() {
        ProcessBuilder expected = new ProcessBuilder("pwd");
        ProcessBuilder actual = Util.formulate("pwd");
        assertEquals(expected.command(), actual.command());
    }

    @Test
    void formulate__many_parts() {
        ProcessBuilder expected = new ProcessBuilder("tr", "-d", "a");
        ProcessBuilder actual = Util.formulate("tr", "-d", "a");
        assertEquals(expected.command(), actual.command());
    }

    @Test
    void formulate__many_parts__null_arr() {
        ProcessBuilder actual = Util.formulate("tr", (String[]) null);
        assertEquals(List.of("tr"), actual.command());
    }

    @Test
    void formulate__many_parts__arr_with_null_or_empty() {
        ProcessBuilder actual = Util.formulate("tr", "-d", "", null, "a");
        assertEquals(List.of("tr", "-d", "a"), actual.command());
    }

}
