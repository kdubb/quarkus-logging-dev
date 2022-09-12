package io.quarkiverse.logging.dev.test;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestExceptions {

    public static Throwable getEx1() {
        return new RuntimeException("Something bad happened. Here are more details", getEx2());
    }

    public static Throwable getEx2() {
        return new IOException("Could not read from file\nmore details on a newline.", getEx3());
    }

    public static Throwable getEx3() {
        return new FileNotFoundException("File not found\nmore details on a newline.");
    }

}
