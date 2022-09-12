package io.quarkiverse.logging.dev.runtime;

import java.util.*;

public class ExceptionCollector {

    private static final int MAX_EXCEPTIONS = 1000;

    private static final Map<String, Throwable> exceptions = new HashMap<>();
    private static final List<Throwable> exceptionsOrder = new ArrayList<>();

    public static String add(Throwable exception) {
        var exceptionId = exceptionId(exception);
        exceptions.put(exceptionId, exception);
        if (exceptions.size() > MAX_EXCEPTIONS) {
            var removed = exceptionsOrder.remove(exceptionsOrder.size() - 1);
            var removedExceptionId = exceptionId(removed);
            exceptions.remove(removedExceptionId);
        }
        return exceptionId;
    }

    public static Throwable find(String exceptionId) {
        return exceptions.get(exceptionId);
    }

    public static String exceptionId(Throwable exception) {
        return Integer.toUnsignedString(Arrays.hashCode(exception.getStackTrace()));
    }

}
