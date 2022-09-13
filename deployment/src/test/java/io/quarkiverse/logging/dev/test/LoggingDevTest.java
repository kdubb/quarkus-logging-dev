package io.quarkiverse.logging.dev.test;

import static java.util.logging.Level.INFO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.logmanager.Level.ERROR;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.logging.dev.runtime.DevFormatter;
import io.quarkus.test.QuarkusUnitTest;

public class LoggingDevTest {

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(TestExceptions.class));

    @Test
    public void testHTTP() {
        var msg = "127.0.0.1 - auth0|5cec35fb94f02a0e160b5fad 08/Sep/2022:21:25:23 -0700 \"GET /api/v1/user/teams/3UZATo6sz3juEcN9pE3LX0 HTTP/1.1\" 200 1817";
        var record = new ExtLogRecord(INFO, msg, getClass().getName());
        record.setThreadName("Quarkus Main");
        record.setMdc(Map.of("traceId", "51793732132590713", "spanId", "1497135987135289"));
        record.setInstant(ZonedDateTime.of(2001, 1, 1, 12, 34, 56, 0, ZoneId.systemDefault()).toInstant());
        record.setLoggerName("io.quarkus.http.access-log");

        var formatted = new DevFormatter("localhost", 8080, true).format(record);
        System.out.println(formatted);

        var expected = "12:34:56 HTTP  GET /api/v1/user/teams/3UZATo6sz3juEcN9pE3LX0\n" +
                "               |200 OK HTTP/1.1| <auth0|5cec35fb94f02a0e160b5fad>\n" +
                "               [io.qua.htt.access-log] (Quarkus Main) <trace:51793732132590713,span:1497135987135289>\n\n";
        assertThat(removeEscapes(formatted), equalTo(expected));
    }

    @Test
    public void testFullRecord() {
        var record = new ExtLogRecord(ERROR, "A serious error occurred", getClass().getName());
        record.setThreadName("Quarkus Main");
        record.setMdc(Map.of("traceId", "51793732132590713", "spanId", "1497135987135289"));
        record.setThrown(TestExceptions.getEx1());
        record.setInstant(ZonedDateTime.of(2001, 1, 1, 12, 34, 56, 0, ZoneId.systemDefault()).toInstant());
        record.setLoggerName("io.quarkus");

        var formatted = new DevFormatter("localhost", 8080, true).format(record);
        System.out.println(formatted);

        var expected = "12:34:56 ERROR A serious error occurred\n" +
                "               [io.quarkus] (Quarkus Main) <trace:51793732132590713,span:1497135987135289>\n" +
                "               ↪ Something bad happened. Here are more details\n" +
                "                 RuntimeException TestExceptions.getEx1(TestExceptions.java:9)\n" +
                "                 ↪ Could not read from file\n" +
                "                     more details on a newline.\n" +
                "                   IOException TestExceptions.getEx2(TestExceptions.java:13)\n" +
                "                   ↪ File not found\n" +
                "                       more details on a newline.\n" +
                "                     FileNotFoundException TestExceptions.getEx3(TestExceptions.java:17)\n" +
                "               http://localhost:8080/q/exceptions/1323516898\n\n";
        assertThat(removeEscapes(formatted), equalTo(expected));
    }

    @Test
    public void testNoTrace() {
        var record = new ExtLogRecord(ERROR, "A serious error occurred", getClass().getName());
        record.setThreadName("Quarkus Main");
        record.setInstant(ZonedDateTime.of(2001, 1, 1, 12, 34, 56, 0, ZoneId.systemDefault()).toInstant());
        record.setLoggerName("io.quarkus");

        var formatted = new DevFormatter("localhost", 8080, false).format(record);
        System.out.println(formatted);

        var expected = "12:34:56 ERROR A serious error occurred\n" +
                "               [io.quarkus] (Quarkus Main)\n\n";
        assertThat(removeEscapes(formatted), equalTo(expected));
    }

    @Test
    public void testLongSplittableMessage() {
        var record = new ExtLogRecord(INFO, "Installed features: [%s]", ExtLogRecord.FormatStyle.PRINTF, getClass().getName());
        record.setParameters(new Object[] {
                "amazon-s3, cdi, config-yaml, hibernate-validator, kotlin, kubernetes, logging-dev, micrometer, mongodb-client, mongodb-panache, narayana-jta, openfga-client, opentelemetry, opentelemetry-otlp-exporter, reactive-routes, rest-client-reactive, rest-client-reactive-jackson, resteasy-reactive, resteasy-reactive-jackson, security, smallrye-context-propagation, smallrye-fault-tolerance, smallrye-health, smallrye-jwt, smallrye-reactive-messaging, smallrye-reactive-messaging-rabbitmq, vault, vertx, zanzibar, zanzibar-open-fga" });
        record.setThreadName("Quarkus Main");
        record.setMdc(Map.of("traceId", "51793732132590713", "spanId", "1497135987135289"));
        record.setInstant(ZonedDateTime.of(2001, 1, 1, 12, 34, 56, 0, ZoneId.systemDefault()).toInstant());
        record.setLoggerName("io.quarkus");

        var formatted = new DevFormatter("localhost", 8080, true).format(record);
        System.out.println(formatted);

        var expected = "12:34:56 INFO  Installed features: [amazon-s3, cdi, config-yaml, hibernate-validator, kotlin, kubernetes, logging-dev, micrometer, mongodb-client,\n"
                +
                "                 mongodb-panache, narayana-jta, openfga-client, opentelemetry, opentelemetry-otlp-exporter, reactive-routes, rest-client-reactive,\n"
                +
                "                 rest-client-reactive-jackson, resteasy-reactive, resteasy-reactive-jackson, security, smallrye-context-propagation, smallrye-fault-tolerance,\n"
                +
                "                 smallrye-health, smallrye-jwt, smallrye-reactive-messaging, smallrye-reactive-messaging-rabbitmq, vault, vertx, zanzibar, zanzibar-open-fga]\n"
                +
                "               [io.quarkus] (Quarkus Main) <trace:51793732132590713,span:1497135987135289>\n\n";
        assertThat(removeEscapes(formatted), equalTo(expected));
    }

    @Test
    public void testMessageWithWordsLongerThanWrapLength() {
        var record = new ExtLogRecord(INFO, "", getClass().getName());
        record.setMessage(
                "Request failed with status code 400: Query validation error: 'String 'id,email,name' does not match pattern. Must be a comma separated list of the following values: phone_number,email,email_verified,picture,username,user_id,name,nickname,created_at,identities,app_metadata,user_metadata,last_ip,last_login,logins_count,updated_at,blocked,family_name,given_name' on property fields (Comma-separated list of fields to include or exclude (based on value provided for include_fields) in the result. Leave empty to retrieve all fields).");
        record.setThreadName("Quarkus Main");
        record.setMdc(Map.of("traceId", "51793732132590713", "spanId", "1497135987135289"));
        record.setInstant(ZonedDateTime.of(2001, 1, 1, 12, 34, 56, 0, ZoneId.systemDefault()).toInstant());
        record.setLoggerName("io.quarkus");

        var formatted = new DevFormatter("localhost", 8080, true).format(record);
        System.out.println(formatted);

        var expected = "12:34:56 INFO  Request failed with status code 400: Query validation error: 'String 'id,email,name' does not match pattern. Must be a comma separated list of\n"
                +
                "                 the following values: phone_number,email,email_verified,picture,username,user_id,name,nickname,created_at,identities,app_metadata,\n"
                +
                "                 user_metadata,last_ip,last_login,logins_count,updated_at,blocked,family_name,given_name' on property fields (Comma-separated list of fields\n"
                +
                "                 to include or exclude (based on value provided for include_fields) in the result. Leave empty to retrieve all fields).\n"
                +
                "               [io.quarkus] (Quarkus Main) <trace:51793732132590713,span:1497135987135289>\n\n";
        assertThat(removeEscapes(formatted), equalTo(expected));
    }

    @Test
    public void testMessageWithExplicitNewlines() {
        var record = new ExtLogRecord(INFO, "", getClass().getName());
        record.setMessage(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit,\n" +
                        "sed do eiusmod tempor incididunt ut labore et dolore magna\n" +
                        "aliqua. Ut enim ad minim veniam, quis nostrud exercitation\n" +
                        "ullamco laboris nisi ut aliquip ex ea commodo consequat.\n" +
                        "Duis aute irure dolor in reprehenderit in voluptate velit\n" +
                        "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint\n" +
                        "occaecat cupidatat non proident, sunt in culpa qui officia\n" +
                        "deserunt mollit anim id est laborum.\n");
        record.setThreadName("Quarkus Main");
        record.setMdc(Map.of("traceId", "51793732132590713", "spanId", "1497135987135289"));
        record.setInstant(ZonedDateTime.of(2001, 1, 1, 12, 34, 56, 0, ZoneId.systemDefault()).toInstant());
        record.setLoggerName("io.quarkus");

        var formatted = new DevFormatter("localhost", 8080, true).format(record);
        System.out.println(formatted);

        var expected = "12:34:56 INFO  Lorem ipsum dolor sit amet, consectetur adipiscing elit,\n" +
                "                 sed do eiusmod tempor incididunt ut labore et dolore magna\n" +
                "                 aliqua. Ut enim ad minim veniam, quis nostrud exercitation\n" +
                "                 ullamco laboris nisi ut aliquip ex ea commodo consequat.\n" +
                "                 Duis aute irure dolor in reprehenderit in voluptate velit\n" +
                "                 esse cillum dolore eu fugiat nulla pariatur. Excepteur sint\n" +
                "                 occaecat cupidatat non proident, sunt in culpa qui officia\n" +
                "                 deserunt mollit anim id est laborum.\n" +
                "               [io.quarkus] (Quarkus Main) <trace:51793732132590713,span:1497135987135289>\n\n";
        assertThat(removeEscapes(formatted), equalTo(expected));
    }

    static String removeEscapes(String str) {
        return str.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
