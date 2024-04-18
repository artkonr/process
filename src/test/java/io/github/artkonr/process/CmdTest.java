package io.github.artkonr.process;

import io.github.artkonr.process.types.TestProcess;
import io.github.artkonr.result.Result;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CmdTest {

    @Test
    void factory__process_builder__ok() {
        ProcessBuilder pb = new ProcessBuilder("pwd");
        Cmd shell = Cmd.from(pb);
        assertEquals(pb, shell.handle);
    }

    @Test
    void factory__process_builder__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Cmd.from(null));
    }

    @Test
    void factory__text_command__ok() {
        Cmd shell = Cmd.from("tr", "-d", "a");
        assertEquals(
                List.of("tr", "-d", "a"),
                shell.handle.command()
        );
    }

    @Test
    void factory__text_command__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Cmd.from((String) null));
        assertThrows(IllegalArgumentException.class, () -> Cmd.from(null, (String[]) null));
    }

    @Test
    void pipeTo__process_builder__ok() {
        Cmd shell = Cmd.from("pwd");
        Chain chain = shell.pipeTo(new ProcessBuilder("tr", "-d", "/"));

        assertEquals(
                List.of(List.of("pwd"), List.of("tr", "-d", "/")),
                chain.getPipeline().stream().map(ProcessBuilder::command).toList()
        );
    }

    @Test
    void pipeTo__process_builder__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Cmd.from("pwd").pipeTo((ProcessBuilder) null));
    }

    @Test
    void pipeTo__text_command__ok() {
        Cmd shell = Cmd.from("pwd");
        Chain chain = shell.pipeTo("tr", "-d", "/");
        assertEquals(
                List.of(List.of("pwd"), List.of("tr", "-d", "/")),
                chain.getPipeline().stream().map(ProcessBuilder::command).toList()
        );
    }

    @Test
    void pipeTo__text_command__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Cmd.from("pwd").pipeTo(null, null));
    }

    @Test
    void pipeTo__command__ok() {
        Cmd first = Cmd.from("pwd");
        Cmd then = Cmd.from("tr", "-d", "/");
        Chain chain = first.pipeTo(then);
        assertEquals(
                List.of(List.of("pwd"), List.of("tr", "-d", "/")),
                chain.getPipeline().stream().map(ProcessBuilder::command).toList()
        );
    }

    @Test
    void pipeTo__command__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Cmd.from("pwd").pipeTo((Cmd) null));
    }

    @Test
    void handle__ok() {
        Process process = TestProcess.builder()
                .stdout("abc")
                .build();
        Result<Process, IOException> invoke = Result.ok(process);
        Result<Output, CmdException> result = Cmd.handle(invoke, "pwd");
        assertTrue(result.isOk());
        assertEquals(0, result.get().exitcode());
        assertTrue(result.get().stdout().encode().isPresent());
        assertEquals("abc", result.get().stdout().encode().orElseThrow());
    }

    @Test
    void handle__err__not_exited_normally() {
        Process process = TestProcess.builder()
                .exitcode(31)
                .stdout("abc")
                .stderr("fail")
                .build();
        Result<Process, IOException> invoke = Result.ok(process);
        Result<Output, CmdException> result = Cmd.handle(invoke, "pwd");
        assertTrue(result.isErr());
        String msg = result.getErr().getMessage();
        assertTrue(msg.contains("'fail'"));
        assertTrue(msg.contains("actual=31"));
        assertTrue(msg.contains("'pwd'"));
    }

    @Test
    void handle__err__process_api_failed() {
        Result<Process, IOException> invoke = Result.err(new IOException("fail"));
        Result<Output, CmdException> result = Cmd.handle(invoke, "pwd");
        assertTrue(result.isErr());
        assertNotNull(result.getErr().getCause());
        assertInstanceOf(IOException.class, result.getErr().getCause());
    }

    @Test
    void handle__err__read_stdout_failed() {
        Process process = TestProcess.builder()
                .stdout("abc")
                .failure(TestProcess.Failure.STREAM_READ_ERR)
                .build();
        Result<Process, IOException> invoke = Result.ok(process);
        Result<Output, CmdException> result = Cmd.handle(invoke, "pwd");
        assertTrue(result.isErr());
        assertEquals("failed to read stdout", result.getErr().getCause().getMessage());
    }

    @Test
    void handle__err__interrupted_when_waiting_for_exitcode() {
        Process process = TestProcess.builder()
                .stdout("abc")
                .failure(TestProcess.Failure.WAIT_FOR_COMPLETION_INTERRUPTED)
                .build();
        Result<Process, IOException> invoke = Result.ok(process);
        Result<Output, CmdException> result = Cmd.handle(invoke, "pwd");
        assertTrue(result.isErr());
        assertNotNull(result.getErr().getCause());
        assertInstanceOf(InterruptedException.class, result.getErr().getCause());
    }

    @Test
    void handle__err__generic_pojo_error() {
        Process process = TestProcess.builder()
                .stdout("abc")
                .failure(TestProcess.Failure.GENERIC_ERROR_INJECTED)
                .build();
        Result<Process, IOException> invoke = Result.ok(process);
        assertThrows(RuntimeException.class, () -> Cmd.handle(invoke, "pwd"));
    }

    @Test
    void invoke__ok() {
        Cmd sh = Cmd.from("pwd");
        Result<Output, CmdException> result = sh.invoke();
        assertTrue(result.isOk());
        assertEquals(0, result.get().exitcode());
    }

    @Test
    void invoke__err() {
        Cmd sh = Cmd.from("curl");
        Result<Output, CmdException> result = sh.invoke();
        assertTrue(result.isErr());
    }

    @Test
    void output__attributes() {
        Output out = newOutput(true);

        assertEquals(10, out.pid());
        assertEquals(0, out.exitcode());
        assertEquals("cmd", out.command());
        assertEquals("a", out.stdout().encode().orElseThrow());
        assertEquals("b", out.stderr().encode().orElseThrow());
        assertFalse(out.isEmpty());
        assertTrue(out.error().isEmpty());
    }

    @Test
    void output__exitedNormally__yes() {
        Output out = newOutput(true);
        assertTrue(out.exitedNormally());
    }

    @Test
    void output__exitedNormally__no() {
        Output out = newOutput(false);
        assertFalse(out.exitedNormally());
    }

    @Test
    void output__devnull() {
        Output out = newOutput(true);
        Output devnulled = out.devnull();
        assertEquals(out.exitcode(), devnulled.exitcode());
        assertTrue(devnulled.stdout().isEmpty());
        assertTrue(devnulled.stderr().isEmpty());
    }

    private Output newOutput(boolean ok) {
        int statusCode = ok ? 0 : 127;
        return Cmd.Output.from(10, "cmd", statusCode, new byte[]{ 97 }, new byte[] { 98 });
    }

}
