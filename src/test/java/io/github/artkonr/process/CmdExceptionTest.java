package io.github.artkonr.process;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CmdExceptionTest {

    @Test
    void errorExitCode() {
        String expected =
           """
           command failed with unexpected exitcode: \
           cmd='echo' expected=40 actual=127 message='some failure'\
           """;

        CmdException cx = CmdException.errorExitCode(40, "echo", 127, "some failure");
        assertEquals(expected, cx.getMessage());
    }

    @Test
    void wrap_same_type() {
        Exception source = new CmdException("generic");
        CmdException wrapped = CmdException.wrap(source);
        assertSame(source, wrapped);
    }

    @Test
    void wrap() {
        Exception source = new RuntimeException("generic");
        CmdException wrapped = CmdException.wrap(source);
        assertNotNull(wrapped.getCause());
        assertSame(source, wrapped.getCause());
    }
}