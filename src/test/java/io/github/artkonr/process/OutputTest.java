package io.github.artkonr.process;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputTest {


    @Test
    void exitedNormally__yay() {
        Output output = new Impl() { };
        assertTrue(output.exitedNormally());
    }

    @Test
    void exitedNormally__nay() {
        Output output = new Impl() {
            @Override
            public int exitcode() {
                return 127;
            }
        };
        assertFalse(output.exitedNormally());
    }

    @Test
    void isEmpty__has_stdout() {
        Output output = new Impl() {
            @Override
            public Data stdout() {
                return new Data(new byte[]{ 96 });
            }
        };
        assertFalse(output.isEmpty());
    }

    @Test
    void isEmpty__has_stderr() {
        Output output = new Impl() {
            @Override
            public Data stderr() {
                return new Data(new byte[]{ 97 });
            }
        };
        assertFalse(output.isEmpty());
    }

    @Test
    void isEmpty__has_stdout_and_stderr() {
        Output output = new Impl() {
            @Override
            public Data stdout() {
                return new Data(new byte[]{ 96 });
            }

            @Override
            public Data stderr() {
                return new Data(new byte[]{ 97 });
            }
        };
        assertFalse(output.isEmpty());
    }

    @Test
    void isEmpty__no_output() {
        Output output = new Impl() { };
        assertTrue(output.isEmpty());
    }

    @Test
    void error__has_error() {
        Output output = new Impl() {
            @Override
            public int exitcode() {
                return 127;
            }
        };
        assertTrue(output.error().isPresent());
        assertSame(output, output.error().get());
    }

    @Test
    void error__no_error() {
        Output output = new Impl() { };
        assertTrue(output.error().isEmpty());
    }

    private abstract static class Impl implements Output {
        @Override
        public Output devnull() {
            return null;
        }

        @Override
        public long pid() {
            return 0;
        }

        @Override
        public String command() {
            return null;
        }

        @Override
        public int exitcode() {
            return 0;
        }

        @Override
        public Data stdout() {
            return new Data(null);
        }

        @Override
        public Data stderr() {
            return new Data(null);
        }
    }

}