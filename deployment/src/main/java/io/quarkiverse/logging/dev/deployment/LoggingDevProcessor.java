package io.quarkiverse.logging.dev.deployment;

import static io.quarkiverse.logging.dev.runtime.ExceptionsRouteHandler.ID_PARAM;
import static io.quarkiverse.logging.dev.runtime.ExceptionsRouteHandler.ROUTE_PATH;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkiverse.logging.dev.runtime.DevLoggingRecorder;
import io.quarkiverse.logging.dev.runtime.ExceptionsRouteHandler;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

class LoggingDevProcessor {

    private static final String FEATURE = "logging-dev";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    LogConsoleFormatBuildItem setUpFormatter(
            HttpConfiguration httpConfig,
            Capabilities capabilities,
            DevLoggingRecorder recorder) {

        var showTraceContext = capabilities.isPresent(Capability.OPENTELEMETRY_TRACER);

        return new LogConsoleFormatBuildItem(recorder.createFormatter(httpConfig, showTraceContext));
    }

    @BuildStep
    RouteBuildItem setUpRoute(NonApplicationRootPathBuildItem nonApplicationRootPath) {

        return nonApplicationRootPath.routeBuilder()
                .route(ROUTE_PATH + "/:" + ID_PARAM)
                .displayOnNotFoundPage()
                .handler(new ExceptionsRouteHandler())
                .build();
    }
}
