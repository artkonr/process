package io.github.artkonr.process;

import io.github.artkonr.result.Result;

public interface Shell {

    Result<Output, CmdException> invoke();

    Chain pipeTo(ProcessBuilder pb);

    Chain pipeTo(String exec, String... arguments);

    Chain pipeTo(Cmd command);

}
