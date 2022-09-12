package io.quarkiverse.logging.dev.runtime;

import java.util.Optional;
import java.util.logging.Formatter;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

@Recorder
public class DevLoggingRecorder {

    public RuntimeValue<Optional<Formatter>> createFormatter(HttpConfiguration config, boolean showTraceContext) {
        return new RuntimeValue<>(Optional.of(new DevFormatter(config.host, config.port, showTraceContext)));
    }

}
