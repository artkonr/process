package io.github.artkonr.process;

import io.github.artkonr.result.Result;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.slf4j.simple.SimpleLogger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class ChainTest {

    @Test
    void factory__process_builder__ok() {
        ProcessBuilder pb = new ProcessBuilder("pwd");
        Chain shell = Chain.from(pb);
        assertEquals(List.of(pb), shell.getPipeline());
    }

    @Test
    void factory__process_builder__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Chain.from((ProcessBuilder) null));
    }

    @Test
    void factory__command__ok() {
        Cmd pb = Cmd.from("pwd");
        Chain shell = Chain.from(pb);
        assertEquals(List.of(pb.handle), shell.getPipeline());
    }

    @Test
    void factory__command__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Chain.from((Cmd) null));
    }

    @Test
    void factory__text_command__ok() {
        Chain shell = Chain.from("tr", "-d", "a");
        assertEquals(
                List.of(List.of("tr", "-d", "a")),
                shell.getPipeline().stream().map(ProcessBuilder::command).toList()
        );
    }

    @Test
    void factory__text_command__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Chain.from((String) null));
        assertThrows(IllegalArgumentException.class, () -> Chain.from(null, (String[]) null));
    }

    @Test
    void pipeTo__process_builder__ok() {
        Chain shell = Chain.from("pwd");
        Chain chain = shell.pipeTo(new ProcessBuilder("tr", "-d", "/"));

        assertEquals(
                List.of(List.of("pwd"), List.of("tr", "-d", "/")),
                chain.getPipeline().stream().map(ProcessBuilder::command).toList()
        );
    }

    @Test
    void pipeTo__process_builder__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Chain.from("pwd").pipeTo((ProcessBuilder) null));
    }

    @Test
    void pipeTo__text_command__ok() {
        Chain shell = Chain.from("pwd");
        Chain chain = shell.pipeTo("tr", "-d", "/");
        assertEquals(
                List.of(List.of("pwd"), List.of("tr", "-d", "/")),
                chain.getPipeline().stream().map(ProcessBuilder::command).toList()
        );
    }

    @Test
    void pipeTo__text_command__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Chain.from("pwd").pipeTo(null, null));
    }

    @Test
    void pipeTo__command__ok() {
        Chain first = Chain.from("pwd");
        Cmd then = Cmd.from("tr", "-d", "/");
        Chain chain = first.pipeTo(then);
        assertEquals(
                List.of(List.of("pwd"), List.of("tr", "-d", "/")),
                chain.getPipeline().stream().map(ProcessBuilder::command).toList()
        );
    }

    @Test
    void pipeTo__command__null_arg() {
        assertThrows(IllegalArgumentException.class, () -> Chain.from("pwd").pipeTo((Cmd) null));
    }

    @Test
    void invoke__pipeline_ok() {
        Chain pipeline = Chain.from("pwd")
                .pipeTo("tr", "-d", "/");
        Result<Output, CmdException> result = pipeline.invoke();
        assertTrue(result.isOk());
        assertTrue(result.get().stdout().encode().isPresent());
        assertEquals(-1, result.get().stdout().encode().get().indexOf('/'));
    }

    @Test
    void invoke__pipeline_first_err() {
        Chain pipeline = Chain.from("curl")
                .pipeTo("tr", "-d", "'-'");
        Result<Output, CmdException> result = pipeline.invoke();
        assertTrue(result.isErr());
        assertTrue(result.getErr().getMessage().contains("cmd='curl"));
    }

    @Test
    void invoke__pipeline_all_err_take_from_first() {
        Chain pipeline = Chain.from("curl")
                .pipeTo("tr");
        Result<Output, CmdException> result = pipeline.invoke();
        assertTrue(result.isErr());
        assertTrue(result.getErr().getMessage().contains("cmd='curl'"));
    }

    @Test
    void invoke__pipeline_3_commands_second_error() {
        Chain pipeline = Chain.from("pwd")
                .pipeTo("curl")
                .pipeTo("tr", "-d", "'-'");
        Result<Output, CmdException> result = pipeline.invoke();
        assertTrue(result.isErr());
        assertTrue(result.getErr().getMessage().contains("cmd='curl"));
    }

    @Test
    void invoke__pipeline_with_1_command() {
        Chain pipeline = Chain.from("pwd");
        Result<Output, CmdException> result = pipeline.invoke();
        assertTrue(result.isOk());
        assertTrue(result.get().stdout().encode().isPresent());
        assertTrue(result.get().stdout().encode().get().indexOf('/') > -1);
    }

    @Test
    void output__attributes() {
        Output out = newOutput(
                new ProcessCompletion("curl", true),
                new ProcessCompletion("tr -d '-'", true)
        );

        assertTrue(out.pid() >= 0 && out.pid() < 100);
        assertEquals(0, out.exitcode());
        assertEquals("curl | tr -d '-'", out.command());
        assertEquals("a", out.stdout().encode().orElseThrow());
        assertEquals("b", out.stderr().encode().orElseThrow());
        assertFalse(out.isEmpty());
        assertTrue(out.error().isEmpty());
    }

    @Test
    void output__exitedNormally__yes() {
        Output out = newOutput(
                new ProcessCompletion("curl", true),
                new ProcessCompletion("tr -d '-'", true)
        );
        assertTrue(out.exitedNormally());
    }

    @Test
    void output__exitedNormally__no() {
        Output out = newOutput(
                new ProcessCompletion("curl", false),
                new ProcessCompletion("tr -d '-'", true)
        );
        assertFalse(out.exitedNormally());
    }

    @Test
    void output__devnull() {
        Output out = newOutput(
                new ProcessCompletion("curl", true),
                new ProcessCompletion("tr -d '-'", true)
        );
        Output devnulled = out.devnull();
        assertEquals(out.exitcode(), devnulled.exitcode());
        assertTrue(devnulled.stdout().isEmpty());
        assertTrue(devnulled.stderr().isEmpty());
    }

    @Test
    void output__error__first() {
        Output out = newOutput(
                new ProcessCompletion("curl", false),
                new ProcessCompletion("tr -d '-'", true)
        );
        Optional<Output> err = out.error();
        assertTrue(err.isPresent());
        assertEquals("curl", err.get().command());
    }

    @Test
    void output__error__final() {
        Output out = newOutput(
                new ProcessCompletion("curl", true),
                new ProcessCompletion("tr -d '-'", false)
        );
        Optional<Output> err = out.error();
        assertTrue(err.isPresent());
        assertEquals("tr -d '-'", err.get().command());
    }

    private Output newOutput(ProcessCompletion... processes) {
        List<Output> converted = Arrays.stream(processes)
                .map(item -> {
                    int statusCode = item.ok ? 0 : 127;
                    return Cmd.Output.from(
                            ThreadLocalRandom.current().nextInt(0, 100),
                            item.cmd,
                            statusCode,
                            new byte[]{ 97 },
                            new byte[] { 98 }
                    );
                })
                .toList();

        List<Output> intermediate = converted.subList(0, converted.size() - 1);
        Output fin = converted.get(converted.size() - 1);
        return new Chain.Output(intermediate, fin);
    }

    private record ProcessCompletion(String cmd, boolean ok) { }

}