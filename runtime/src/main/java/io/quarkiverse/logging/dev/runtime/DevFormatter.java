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

    private static final String HTTP_ACCESS_LOGGER_NAME = "io.quarkus.http.access-log";
    private static final String HTTP_LOG_LEVEL = "HTTP";
    private static final String HTTP_MISSING_FIELD = "-";
    private static final String HTTP_RESPONSE_DELIM = "|";
    private static final String HTTP_SECURITY_BEG_DELIM = "<";
    private static final String HTTP_SECURITY_END_DELIM = ">";
    private static final String HTTP_SECURITY_NO_USER = "none";
    private static final String HTTP_UNKNOWN_STATUS = "Unknown";
    private static final Pattern HTTP_LOG_PATTERN = compile(
            "([^ ]+) ([^ ]+) ([^ ]+) (\\d{2}/\\w+/\\d{4}:\\d{2}:\\d{2}:\\d{2} [+\\-]?\\d+) \"([^\"]+)\" (\\d+) (\\d+)");
    private static final Pattern HTTP_REQ_PATTERN = compile("(\\w+) ([^ ]+) (HTTP/.*)");

    private static final String EXC_PATH = "/q/" + ROUTE_PATH + "/";
    private static final String EXC_MESSAGE_BEG = "↪ ";
    private static final String EXC_URL_FORMAT = "http://%s:%d" + EXC_PATH + "%s";
    private static final String EXC_TRACE_LOC_BEG_DELIM = "(";
    private static final String EXC_TRACE_LOC_END_DELIM = ")";
    private static final String EXC_TRACE_LOC_SEP_DELIM = ":";

    private static final String CTX_LOGGER_BEG_DELIM = "[";
    private static final String CTX_LOGGER_END_DELIM = "]";
    private static final String CTX_THREAD_BEG_DELIM = "(";
    private static final String CTX_THREAD_END_DELIM = ")";
    private static final String CTX_TELEMETRY_BEG_DELIM = "<";
    private static final String CTX_TELEMETRY_TRACE_NAME = "trace";
    private static final String CTX_TELEMETRY_SPAN_NAME = "span";
    private static final String CTX_TELEMETRY_VALUE_DELIM = ":";
    private static final String CTX_TELEMETRY_FIELD_DELIM = ",";
    private static final String CTX_TELEMETRY_END_DELIM = ">";
    private static final String CTX_TELEMETRY_NO_TRACE = "none";
    private static final String CTX_TELEMETRY_NO_SPAN = "none";
    private static final String MDC_TRACE_ID_NAME = "traceId";
    private static final String MDC_SPAN_ID_NAME = "spanId";

    private static final int LEVEL_PAD_LENGTH = 5;
    private static final int MAX_LOG_SECTION_LENGTH = 3;

    private static final String NO_LOG = "";
    private static final String EMPTY_MESSAGE = "";
    private static final String INDENT = blankString(15);
    private static final String MSG_EXTRA_INDENT = blankString(2);
    private static final String NEWLINE = "\n";
    private static final String SPACE = " ";
    private static final String DOUBLE_NEWLINE = NEWLINE + NEWLINE;
    private static final Pattern LOGGER_NAME_SPLIT_PATTERN = Pattern.compile("\\.");
    private static final String LOGGER_NAME_SPLIT = ".";
    private static final char CLASS_NAME_SPLIT = '.';

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
        if (HTTP_ACCESS_LOGGER_NAME.equals(record.getLoggerName())) {
            return formatHttp(record);
        }
        return formatGeneral(record);
    }

    public String formatHttp(ExtLogRecord record) {

        var logMatcher = HTTP_LOG_PATTERN.matcher(record.getMessage());
        if (!logMatcher.matches()) {
            return formatGeneral(record);
        }

        var reqMatcher = HTTP_REQ_PATTERN.matcher(logMatcher.group(5));
        if (!reqMatcher.matches()) {
            return formatGeneral(record);
        }

        // Filter out access logs for our requests...
        if (reqMatcher.group(2).startsWith(EXC_PATH)) {
            return NO_LOG;
        }

        int statusCode;
        String statusMessage;
        try {
            statusCode = Integer.parseInt(logMatcher.group(6));
            var status = HttpResponseStatus.valueOf(statusCode);
            if (status != null) {
                statusMessage = status.reasonPhrase();
            } else {
                statusMessage = HTTP_UNKNOWN_STATUS;
            }
        } catch (Throwable ignored) {
            statusCode = 0;
            statusMessage = HTTP_UNKNOWN_STATUS;
        }

        var statusLevel = statusCode >= 100 && statusCode < 400 ? INFO : ERROR;
        var statusColor = levelColor(statusLevel);

        return formatHttpMessageLine(record.getInstant(), statusColor, reqMatcher.group(1), reqMatcher.group(2),
                reqMatcher.group(3)) +
                NEWLINE +
                formatHttpContextLine(statusColor, statusCode, statusMessage, logMatcher.group(3)) +
                NEWLINE +
                formatContextLine(record) +
                DOUBLE_NEWLINE;
    }

    private String formatHttpMessageLine(Instant instant, Color statusColor, String method, String message,
            String httpVersion) {

        var timestamp = instant.atZone(ZoneId.systemDefault());

        return colorize(TIMESTAMP_FORMATTER.format(timestamp), LO_TEXT_COLOR) +
                SPACE +
                colorize(padLevel(HTTP_LOG_LEVEL), LO_TEXT_COLOR) +
                SPACE +
                colorize(method, statusColor) +
                SPACE +
                colorize(message, HI_TEXT_COLOR) +
                SPACE +
                colorize(httpVersion, LO_TEXT_COLOR);
    }

    private String formatHttpContextLine(Color statusColor, int statusCode, String statusMessage, String user) {

        if (user.isBlank() || user.equals(HTTP_MISSING_FIELD)) {
            user = HTTP_SECURITY_NO_USER;
        }

        return INDENT +
                colorize(HTTP_RESPONSE_DELIM, DELIM_COLOR) +
                colorize(Integer.toString(statusCode), statusColor) +
                SPACE +
                colorize(statusMessage, statusColor.darken(0.15f)) +
                colorize(HTTP_RESPONSE_DELIM, DELIM_COLOR) +
                SPACE +
                colorize(HTTP_SECURITY_BEG_DELIM, DELIM_COLOR) +
                colorize(user, HTTP_CTX_IMPORTANT_COLOR) +
                colorize(HTTP_SECURITY_END_DELIM, DELIM_COLOR);
    }

    public String formatGeneral(ExtLogRecord record) {
        var out = new StringBuilder();

        out.append(formatGeneralMessageLine(record))
                .append(NEWLINE);

        var context = formatContextLine(record);
        if (!context.isBlank()) {
            out.append(context)
                    .append(NEWLINE);
        }

        var exception = formatGeneralExceptionLines(record);
        if (!exception.isBlank()) {
            out.append(exception)
                    .append(NEWLINE);
        }

        out.append(NEWLINE);

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
                    message = EMPTY_MESSAGE;
                    break;
            }
        } else {
            message = colorize(record.getMessage(), HI_TEXT_COLOR);
        }

        var messageLine = colorize(TIMESTAMP_FORMATTER.format(timestamp), LO_TEXT_COLOR) +
                SPACE +
                colorize(level, levelColor(record.getLevel())) +
                SPACE +
                message;

        return wrapMessage(messageLine, INDENT + MSG_EXTRA_INDENT);
    }

    private String formatGeneralExceptionLines(ExtLogRecord record) {
        var thrown = record.getThrown();
        if (thrown == null) {
            return EMPTY_MESSAGE;
        }

        var builder = new StringBuilder();

        addGeneralException(thrown, 1, builder);

        var exceptionId = ExceptionCollector.add(thrown);
        var exceptionLink = String.format(EXC_URL_FORMAT, httpHost, httpPort, exceptionId);

        builder.append(NEWLINE)
                .append(INDENT)
                .append(bold(colorize(exceptionLink, EXC_QUATERNARY_COLOR)));

        return builder.toString();
    }

    private void addGeneralException(Throwable x, int level, StringBuilder builder) {
        var messageIndent = new StringBuilder(INDENT);
        var levelIndent = new StringBuilder(INDENT);
        for (int c = 0; c < level - 1; ++c) {
            messageIndent.append(MSG_EXTRA_INDENT);
            levelIndent.append(MSG_EXTRA_INDENT);
        }
        messageIndent.append(colorize(EXC_MESSAGE_BEG, EXC_PRIMARY_COLOR));
        levelIndent.append(MSG_EXTRA_INDENT);

        var colorMessage = colorize(x.getMessage(), EXC_MESSAGE_COLOR);
        var message = wrapMessage(messageIndent + colorMessage, levelIndent + MSG_EXTRA_INDENT);

        String exceptionCtxLine = colorize(x.getClass().getSimpleName(), EXC_PRIMARY_COLOR);
        if (x.getStackTrace() != null && x.getStackTrace().length > 0) {
            var stackTop = x.getStackTrace()[0];
            var className = stackTop.getClassName();
            var simpleClassNameIdx = className.lastIndexOf(CLASS_NAME_SPLIT);
            var simpleClassName = className.substring(simpleClassNameIdx == -1 ? 0 : simpleClassNameIdx + 1);
            exceptionCtxLine += SPACE +
                    colorize(simpleClassName + CLASS_NAME_SPLIT + stackTop.getMethodName(), EXC_SECONDARY_COLOR) +
                    colorize(EXC_TRACE_LOC_BEG_DELIM, DELIM_COLOR) +
                    colorize(stackTop.getFileName(), EXC_TERTIARY_COLOR) +
                    colorize(EXC_TRACE_LOC_SEP_DELIM, DELIM_COLOR) +
                    colorize(Integer.toString(stackTop.getLineNumber()), EXC_TERTIARY_COLOR) +
                    colorize(EXC_TRACE_LOC_END_DELIM, DELIM_COLOR);
        }

        builder.append(message)
                .append(NEWLINE)
                .append(levelIndent)
                .append(exceptionCtxLine);

        Throwable cause = x.getCause();
        if (cause != null && cause != x) {
            builder.append(NEWLINE);
            addGeneralException(cause, level + 1, builder);
        }
    }

    private String formatContextLine(ExtLogRecord record) {

        var builder = new StringBuilder(INDENT)
                .append(colorize(CTX_LOGGER_BEG_DELIM, DELIM_COLOR))
                .append(colorize(shortenLoggerName(record.getLoggerName()), CTX_TERTIARY_COLOR))
                .append(colorize(CTX_LOGGER_END_DELIM, DELIM_COLOR))
                .append(SPACE)
                .append(colorize(CTX_THREAD_BEG_DELIM, DELIM_COLOR))
                .append(colorize(record.getThreadName(), CTX_SECONDARY_COLOR))
                .append(colorize(CTX_THREAD_END_DELIM, DELIM_COLOR));

        if (showTraceContext) {
            var traceId = record.getMdc(MDC_TRACE_ID_NAME);
            traceId = traceId != null ? traceId : CTX_TELEMETRY_NO_TRACE;

            var spanId = record.getMdc(MDC_SPAN_ID_NAME);
            spanId = spanId != null ? spanId : CTX_TELEMETRY_NO_SPAN;

            builder.append(SPACE)
                    .append(colorize(CTX_TELEMETRY_BEG_DELIM, DELIM_COLOR))
                    .append(colorize(CTX_TELEMETRY_TRACE_NAME + CTX_TELEMETRY_VALUE_DELIM, LO_TEXT_COLOR))
                    .append(colorize(traceId, CTX_PRIMARY_COLOR))
                    .append(colorize(CTX_TELEMETRY_FIELD_DELIM, DELIM_COLOR))
                    .append(colorize(CTX_TELEMETRY_SPAN_NAME + CTX_TELEMETRY_VALUE_DELIM, LO_TEXT_COLOR))
                    .append(colorize(spanId, CTX_PRIMARY_COLOR))
                    .append(colorize(CTX_TELEMETRY_END_DELIM, DELIM_COLOR));
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
                        .append(NEWLINE)
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

                wrappedLines.append(NEWLINE)
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
        var sections = LOGGER_NAME_SPLIT_PATTERN.split(str);
        var last = sections[sections.length - 1];
        if (sections.length == 1) {
            return last;
        }
        var prefix = stream(sections, 0, sections.length - 1)
                .map(s -> s.length() > MAX_LOG_SECTION_LENGTH ? s.substring(0, MAX_LOG_SECTION_LENGTH) : s)
                .collect(joining(LOGGER_NAME_SPLIT));
        return prefix + LOGGER_NAME_SPLIT + last;
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
