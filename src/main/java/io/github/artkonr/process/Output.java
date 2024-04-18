package io.github.artkonr.process;

import java.util.Optional;

public interface Output {

    Output devnull();

    long pid();

    String command();

    int exitcode();

    default boolean exitedNormally() {
        return exitcode() == 0;
    }

    default boolean isEmpty() {
        return stdout().isEmpty() && stderr().isEmpty();
    }

    Data stdout();

    Data stderr();

    default Optional<Output> error() {
        if (exitedNormally()) {
            return Optional.empty();
        }

        return Optional.of(this);
    }

}
