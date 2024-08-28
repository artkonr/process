package io.github.artkonr.process;

import io.github.artkonr.result.Result;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Program handling utilities.
 */
class Util {

    /**
     * Processes the byte array to remove whitespace characters etc.
     * @param bytes source array
     * @return processed array
     */
    static byte[] readByteArray(byte[] bytes) {
        if (bytes.length > 1) {
            int end = bytes.length - 1;
            while (end >= 0) {
                byte cursor = bytes[end];
                if (Character.isWhitespace(cursor)) {
                    end = end - 1;
                } else {
                    break;
                }
            }

            return Arrays.copyOfRange(bytes, 0, end + 1);
        } else {
            return bytes;
        }
    }

    /**
     * Safely reads {@link BufferedReader}.
     * @param reader reader
     * @return byte array wrapped in a {@link Result}
     */
    static Result<byte[], Exception> read(BufferedReader reader) {
        try (
                BufferedReader input = reader;
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(buf)
        ) {
            input.transferTo(writer);
            writer.flush();
            return Result.ok(readByteArray(buf.toByteArray()));
        } catch (IOException ex) {
            return Result.err(new CmdException("failed to read stdout/stderr", ex));
        }
    }

    /**
     * Creates an {@link ProcessBuilder} out out a collection of text commands.
     * @param exec program name
     * @param arguments program arguments
     * @return process builder
     */
    static ProcessBuilder formulate(String exec, String... arguments) {
        List<String> all = new ArrayList<>();
        all.add(exec);

        if (arguments != null) {
            Arrays.stream(arguments)
                    .filter(arg -> arg != null && !arg.isBlank())
                    .forEach(all::add);
        }

        return new ProcessBuilder(all)
                .redirectError(ProcessBuilder.Redirect.PIPE);
    }

    /**
     * Extracts command text from a {@link ProcessBuilder}.
     * @param pb process builder
     * @return command text
     */
    static String getCmd(ProcessBuilder pb) {
        return String.join(" ", pb.command());
    }

    /**
     * Extracts command text from a {@link ProcessBuilder}.
     * @param pipeline process builders
     * @return command text
     */
    static String getCmd(List<ProcessBuilder> pipeline) {
        return pipeline.stream()
                .map(Util::getCmd)
                .collect(Collectors.joining(" | "));
    }

    private Util() { }

}
