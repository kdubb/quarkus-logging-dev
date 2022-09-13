package io.quarkiverse.logging.dev.runtime;

import static io.quarkiverse.logging.dev.runtime.ColorUtil.bold;
import static io.quarkiverse.logging.dev.runtime.ColorUtil.colorize;
import static io.quarkiverse.logging.dev.runtime.Colors.*;
import static io.quarkiverse.logging.dev.runtime.ExceptionsRouteHandler.ROUTE_PATH;
import static java.lang.Math.max;
import static java.time.temporal.ChronoField.*;
import static java.util.Arrays.stream;
import static java.util.logging.Level.INFO;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static org.jboss.logmanager.Level.ERROR;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import io.netty.handler.codec.http.HttpResponseStatus;

public class DevFormatter extends ExtFormatter {

    private static final String EXCEPTION_PATH = "/q/" + ROUTE_PATH + "/";
    private static final int LEVEL_PAD_LENGTH = 5;
    private static final int MAX_LOG_SECTION_LENGTH = 3;
    private static final String INDENT = blankString(15);

    String httpHost;
    int httpPort;
    boolean showTraceContext;

    public DevFormatter(String httpHost, int httpPort, boolean showTraceContext) {
        this.httpHost = httpHost;
        this.httpPort = httpPort;
        this.showTraceContext = showTraceContext;
    }

    @Override
    public String format(ExtLogRecord record) {
        if ("io.quarkus.http.access-log".equals(record.getLoggerName())) {
            return formatHttp(record);
        }
        return formatGeneral(record);
    }

    private static final Pattern httpLogPattern = compile(
            "([^ ]+) ([^ ]+) ([^ ]+) (\\d{2}/\\w+/\\d{4}:\\d{2}:\\d{2}:\\d{2} [+\\-]?\\d+) \"([^\"]+)\" (\\d+) (\\d+)");
    private static final Pattern httpReqPattern = compile("(\\w+) ([^ ]+) (HTTP/.*)");

    public String formatHttp(ExtLogRecord record) {

        var logMatcher = httpLogPattern.matcher(record.getMessage());
        if (!logMatcher.matches()) {
            return formatGeneral(record);
        }

        var reqMatcher = httpReqPattern.matcher(logMatcher.group(5));
        if (!reqMatcher.matches()) {
            return formatGeneral(record);
        }

        // Filter out access logs for our requests...
        if (reqMatcher.group(2).startsWith("/q/exceptions/")) {
            return "";
        }

        int statusCode;
        String statusMessage;
        try {
            statusCode = Integer.parseInt(logMatcher.group(6));
            var status = HttpResponseStatus.valueOf(statusCode);
            if (status != null) {
                statusMessage = status.reasonPhrase();
            } else {
                statusMessage = "UNKNOWN";
            }
        } catch (Throwable ignored) {
            statusCode = 0;
            statusMessage = "UNKNOWN";
        }

        var statusLevel = statusCode >= 100 && statusCode < 400 ? INFO : ERROR;
        var statusColor = levelColor(statusLevel);

        return formatHttpMessageLine(record.getInstant(), statusColor, reqMatcher.group(1), reqMatcher.group(2),
                reqMatcher.group(3)) +
                '\n' +
                formatHttpContextLine(statusColor, statusCode, statusMessage, logMatcher.group(3)) +
                '\n' +
                formatContextLine(record) +
                "\n\n";
    }

    private String formatHttpMessageLine(Instant instant, Color statusColor, String method, String message,
            String httpVersion) {

        var timestamp = instant.atZone(ZoneId.systemDefault());

        return colorize(TIMESTAMP_FORMATTER.format(timestamp), LO_TEXT_COLOR) +
                ' ' +
                colorize(padLevel("HTTP"), LO_TEXT_COLOR) +
                ' ' +
                colorize(method, statusColor) +
                ' ' +
                colorize(message, HI_TEXT_COLOR) +
                ' ' +
                colorize(httpVersion, LO_TEXT_COLOR);
    }

    private String formatHttpContextLine(Color statusColor, int statusCode, String statusMessage, String user) {

        if (user.isBlank() || user.equals("-")) {
            user = "none";
        }

        return INDENT +
                colorize("|", DELIM_COLOR) +
                colorize(Integer.toString(statusCode), statusColor) +
                ' ' +
                colorize(statusMessage, statusColor.darken(0.15f)) +
                colorize("|", DELIM_COLOR) +
                ' ' +
                colorize("<", DELIM_COLOR) +
                colorize(user, HTTP_CTX_IMPORTANT_COLOR) +
                colorize(">", DELIM_COLOR);
    }

    public String formatGeneral(ExtLogRecord record) {
        var out = new StringBuilder();

        out.append(formatGeneralMessageLine(record))
                .append('\n');

        var context = formatContextLine(record);
        if (!context.isBlank()) {
            out.append(context)
                    .append('\n');
        }

        var exception = formatGeneralExceptionLines(record);
        if (!exception.isBlank()) {
            out.append(exception)
                    .append('\n');
        }

        out.append('\n');

        return out.toString();
    }

    private String formatGeneralMessageLine(ExtLogRecord record) {

        var timestamp = record.getInstant().atZone(ZoneId.systemDefault());
        var level = padLevel(record.getLevel().getName());

        String message;
        if (record.getParameters() != null) {
            switch (record.getFormatStyle()) {
                case PRINTF:
                    message = COLOR_PRINTF.format(record.getMessage(), record.getParameters());
                    break;
                case MESSAGE_FORMAT:
                    message = colorize(MessageFormat.format(record.getMessage(), record.getParameters()), HI_TEXT_COLOR);
                    break;
                case NO_FORMAT:
                    message = colorize(record.getMessage(), HI_TEXT_COLOR);
                    break;
                default:
                    message = "";
                    break;
            }
        } else {
            message = colorize(record.getMessage(), HI_TEXT_COLOR);
        }

        var messageLine = colorize(TIMESTAMP_FORMATTER.format(timestamp), LO_TEXT_COLOR) +
                ' ' +
                colorize(level, levelColor(record.getLevel())) +
                ' ' +
                message;

        return wrapMessage(messageLine, INDENT + "  ");
    }

    private String formatGeneralExceptionLines(ExtLogRecord record) {
        var thrown = record.getThrown();
        if (thrown == null) {
            return "";
        }

        var builder = new StringBuilder();

        addGeneralException(thrown, 1, builder);

        var exceptionId = ExceptionCollector.add(thrown);
        var exceptionLink = "http://" + httpHost + ":" + httpPort + EXCEPTION_PATH + exceptionId;

        builder.append('\n')
                .append(INDENT)
                .append(bold(colorize(exceptionLink, EXC_QUATERNARY_COLOR)));

        return builder.toString();
    }

    private void addGeneralException(Throwable x, int level, StringBuilder builder) {
        var messageIndent = new StringBuilder(INDENT);
        var levelIndent = new StringBuilder(INDENT);
        for (int c = 0; c < level - 1; ++c) {
            messageIndent.append("  ");
            levelIndent.append("  ");
        }
        messageIndent.append(colorize("↪ ", EXC_PRIMARY_COLOR));
        levelIndent.append("  ");

        var colorMessage = colorize(x.getMessage(), EXC_MESSAGE_COLOR);
        var message = wrapMessage(messageIndent + colorMessage, levelIndent + "  ");

        String exceptionCtxLine = colorize(x.getClass().getSimpleName(), EXC_PRIMARY_COLOR);
        if (x.getStackTrace() != null && x.getStackTrace().length > 0) {
            var stackTop = x.getStackTrace()[0];
            var className = stackTop.getClassName();
            var simpleClassNameIdx = className.lastIndexOf('.');
            var simpleClassName = className.substring(simpleClassNameIdx == -1 ? 0 : simpleClassNameIdx + 1);
            exceptionCtxLine += ' ' +
                    colorize(simpleClassName + '.' + stackTop.getMethodName(), EXC_SECONDARY_COLOR) +
                    colorize("(", DELIM_COLOR) +
                    colorize(stackTop.getFileName(), EXC_TERTIARY_COLOR) +
                    colorize(":", DELIM_COLOR) +
                    colorize(Integer.toString(stackTop.getLineNumber()), EXC_TERTIARY_COLOR) +
                    colorize(")", DELIM_COLOR);
        }

        builder.append(message)
                .append('\n')
                .append(levelIndent)
                .append(exceptionCtxLine);

        Throwable cause = x.getCause();
        if (cause != null && cause != x) {
            builder.append('\n');
            addGeneralException(cause, level + 1, builder);
        }
    }

    private String formatContextLine(ExtLogRecord record) {

        var builder = new StringBuilder(INDENT)
                .append(colorize("[", DELIM_COLOR))
                .append(colorize(shortenLoggerName(record.getLoggerName()), CTX_TERTIARY_COLOR))
                .append(colorize("]", DELIM_COLOR))
                .append(' ')
                .append(colorize("(", DELIM_COLOR))
                .append(colorize(record.getThreadName(), CTX_SECONDARY_COLOR))
                .append(colorize(")", DELIM_COLOR));

        if (showTraceContext) {
            var traceId = record.getMdc("traceId");
            traceId = traceId != null ? traceId : "none";

            var spanId = record.getMdc("spanId");
            spanId = spanId != null ? spanId : "none";

            builder.append(' ')
                    .append(colorize("<", DELIM_COLOR))
                    .append(colorize("trace:", LO_TEXT_COLOR))
                    .append(colorize(traceId, CTX_PRIMARY_COLOR))
                    .append(colorize(",", DELIM_COLOR))
                    .append(colorize("span:", LO_TEXT_COLOR))
                    .append(colorize(spanId, CTX_PRIMARY_COLOR))
                    .append(colorize(">", DELIM_COLOR));
        }

        return builder.toString();
    }

    private static String blankString(int length) {
        return new String(new char[length]).replace('\0', ' ');
    }

    private static String wrapMessage(String message, String wrapIndent) {
        return wrap(message, 160, wrapIndent);
    }

    /**
     * Wrap string skipping escape sequences.
     *
     * @param str String to break into wrapped lines.
     * @param wrapLength Length at which to wrap lines.
     * @param indent Indent to use for wrapped lines.
     * @return Wrapped string.
     */
    public static String wrap(final String str, int wrapLength, String indent) {
        if (str == null) {
            return null;
        }

        var wrappedLines = new StringBuilder(str.length() + 32);

        var lineOffset = new AtomicInteger();

        int strLength = str.length();
        int strOffset = -1;

        while (strOffset < strLength) {

            var wordStrOffset = max(strOffset, 0);
            var wordLineOffset = lineOffset.get();
            strOffset = wrapAdvance(str, strOffset + 1, strLength, lineOffset);

            // Handle explicit line breaks
            if (str.charAt(wordStrOffset) == '\n') {
                if (strOffset >= strLength) {
                    break;
                }

                wrappedLines
                        .append('\n')
                        .append(indent);
                wordStrOffset++;
                lineOffset.set(indent.length());
            }

            if (lineOffset.get() >= wrapLength) {

                // Ensure no dangling spaces and, if possible,
                // keep separator on current line
                if (str.charAt(wordStrOffset) == ' ') {
                    wordStrOffset++;
                } else if (wordLineOffset < wrapLength) {
                    wrappedLines.append(str, wordStrOffset, wordStrOffset + 1);
                    wordStrOffset++;
                }

                wrappedLines.append('\n')
                        .append(indent)
                        .append(str, wordStrOffset, strOffset);

                var overage = lineOffset.get() - wordLineOffset;
                lineOffset.set(indent.length() + overage);

                // Skip leading whitespace
                if (overage == 1) {
                    while (strOffset < strLength && str.charAt(strOffset) == ' ') {
                        strOffset++;
                    }
                }

            } else {
                wrappedLines.append(str, wordStrOffset, strOffset);
            }
        }

        return wrappedLines.toString();
    }

    private static int wrapAdvance(String str, int strOffset, int strLength, AtomicInteger logicalOffset) {
        while (strOffset < strLength) {
            var ch = str.charAt(strOffset);
            switch (ch) {
                case ' ':
                case ',':
                case '.':
                case ';':
                case '|':
                case '\n':
                    logicalOffset.incrementAndGet();
                    return strOffset;

                case '\u001b':
                case '\u009b':
                    var nextOffset = strOffset + 1;
                    if (nextOffset < strLength && str.charAt(nextOffset) == '[') {
                        // skip to end of escape
                        strOffset = nextOffset + 1;
                        while (strOffset < strLength && isEscapeChar(str, strOffset)) {
                            strOffset++;
                        }
                        strOffset++;
                        break;
                    }

                default:
                    strOffset++;
                    logicalOffset.incrementAndGet();
            }
        }
        return strOffset;
    }

    private static boolean isEscapeChar(String str, int offset) {
        var ch = str.charAt(offset);
        return ch >= '0' && ch <= '?';
    }

    private static String padLevel(String str) {
        var needed = LEVEL_PAD_LENGTH - str.length();
        if (needed <= 0) {
            return str;
        }
        return str + blankString(needed);
    }

    private static String shortenLoggerName(String str) {
        var sections = str.split("\\.");
        var last = sections[sections.length - 1];
        if (sections.length == 1) {
            return last;
        }
        var prefix = stream(sections, 0, sections.length - 1)
                .map(s -> s.length() > MAX_LOG_SECTION_LENGTH ? s.substring(0, MAX_LOG_SECTION_LENGTH) : s)
                .collect(joining("."));
        return prefix + "." + last;
    }

    private static final ColorPrintf COLOR_PRINTF = new ColorPrintf(
            HI_TEXT_COLOR,
            Map.of(
                    UUID.class, Color.of(0xdd, 0xff, 0xdd),
                    Class.class, Color.of(0xff, 0xff, 0xdd)),
            0);

    static private final DateTimeFormatter TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .toFormatter();

}
