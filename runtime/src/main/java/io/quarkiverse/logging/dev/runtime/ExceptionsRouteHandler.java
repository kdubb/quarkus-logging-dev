package io.quarkiverse.logging.dev.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ExceptionsRouteHandler implements Handler<RoutingContext> {

    public static final String ROUTE_PATH = "exceptions";
    public static final String ID_PARAM = "id";

    @Override
    public void handle(RoutingContext event) {

        var exceptionId = event.pathParam(ID_PARAM);
        var exception = ExceptionCollector.find(exceptionId);
        if (exception == null) {
            event.response()
                    .setStatusCode(404)
                    .setStatusMessage("NOT FOUND")
                    .send();
            return;
        }

        var output = new StringWriter();

        output.append(HTML_PRE.replace("{{NAME}}", exception.getClass().getName()));

        exception.printStackTrace(new PrintWriter(output));

        output.append(HTML_POST);

        event.response()
                .setStatusCode(200)
                .setStatusMessage("OK")
                .putHeader("Content-Type", "text/html")
                .send(output.toString());
    }

    private static final String HTML_PRE = "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>404 - Resource Not Found</title>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <style>\n" +
            "html, body {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "    font-family: 'Open Sans', Helvetica, Arial, sans-serif;\n" +
            "    font-size: 100%;\n" +
            "    font-weight: 100;\n" +
            "    line-height: 1.4;\n" +
            "}\n" +
            "\n" +
            "html {\n" +
            "    overflow-y: scroll;\n" +
            "}\n" +
            "\n" +
            "body {\n" +
            "    background: #f9f9f9;\n" +
            "}\n" +
            ".container {\n" +
            "    width: 80%;\n" +
            "    margin: 0 auto;\n" +
            "}\n" +
            ".content {\n" +
            "    padding: 1em 0 1em 0;\n" +
            "}\n" +
            "\n" +
            "header, .component-name {\n" +
            "    background-color: #ad1c1c;\n" +
            "}\n" +
            "\n" +
            "ul {\n" +
            "    line-height: 1.5rem;\n" +
            "    margin: 0.25em 0 0.25em 0;\n" +
            "}\n" +
            "\n" +
            ".exception-message {\n" +
            "    background: #be2828;\n" +
            "}\n" +
            "\n" +
            "h1, h2 {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "}\n" +
            "\n" +
            "h1 {\n" +
            "    font-size: 2rem;\n" +
            "    color: #fff;\n" +
            "    line-height: 3.75rem;\n" +
            "    font-weight: 700;\n" +
            "    padding: 0.4rem 0rem 0.4rem 0rem;\n" +
            "}\n" +
            "\n" +
            "h2 {\n" +
            "    font-size: 1.2rem;\n" +
            "    color: rgba(255, 255, 255, 0.85);\n" +
            "    line-height: 2.5rem;\n" +
            "    font-weight: 400;\n" +
            "    padding: 0.4rem 0rem 0.4rem 0rem;\n" +
            "}\n" +
            "\n" +
            ".intro {    font-size: 1.2rem;\n" +
            "    font-weight: 400;\n" +
            "    margin: 0.25em 0 1em 0;\n" +
            "}\n" +
            "h3 {\n" +
            "    font-size: 1.2rem;\n" +
            "    line-height: 2.5rem;\n" +
            "    font-weight: 400;\n" +
            "    color: #555;\n" +
            "    margin: 0.25em 0 0.25em 0;\n" +
            "}\n" +
            "\n" +
            ".trace, .resources {\n" +
            "    background: #fff;\n" +
            "    padding: 15px;\n" +
            "    margin: 15px auto;\n" +
            "    border: 1px solid #ececec;\n" +
            "}\n" +
            ".trace {\n" +
            "    overflow-y: scroll;\n" +
            "}\n" +
            ".hidden {\n" +
            "   display: none;\n" +
            "}\n" +
            "\n" +
            "pre {\n" +
            "    white-space: pre;\n" +
            "    font-family: Consolas, Monaco, Menlo, \"Ubuntu Mono\", \"Liberation Mono\", monospace;\n" +
            "    font-size: 12px;\n" +
            "    line-height: 1.5;\n" +
            "    color: #555;\n" +
            "    overflow-x: scroll;\n" +
            "    border: thin solid black;\n" +
            "    border-radius: 3px;\n" +
            "    padding: 10px;\n" +
            "    padding-bottom: 20px;\n" +
            "}\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<header>\n" +
            "    <h1 class=\"container\">Exception Detail</h1>\n" +
            "    <div class=\"exception-message\">\n" +
            "        <h2 class=\"container\">{{NAME}}</h2>\n" +
            "    </div>\n" +
            "</header>\n" +
            "<div class=\"container content\">\n" +
            "<pre>\n";

    private static final String HTML_POST = "</pre>\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>\n" +
            "\n";
}
