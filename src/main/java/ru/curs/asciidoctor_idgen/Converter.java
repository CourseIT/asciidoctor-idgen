package ru.curs.asciidoctor_idgen;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;


class Converter {
    private String adocFilePath;

    public static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }

    Converter(String adocFilePath) {
        this.adocFilePath = adocFilePath;
    }

    OutputLog convert() {
        final OutputLog outputLog = new OutputLog();
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.registerLogHandler(logRecord -> {
            var fullLogMessage =
                    String.format("%s: %s", logRecord.getSeverity(), logRecord.getMessage());
            System.out.println(fullLogMessage);
            outputLog.add(logRecord.getSeverity().name(), logRecord.getMessage());
        });

        var options = Options.builder().safe(SafeMode.UNSAFE).build();

        asciidoctor.convertFile(new File(this.adocFilePath), options);
        var outputHtmlPath = String.format("%s.html", removeFileExtension(this.adocFilePath, false));
        Document jsoup;
        try {
            jsoup = Jsoup.parse(new File(outputHtmlPath));
        } catch (IOException e) {
            throw new RuntimeException("Output Html not found", e);
        }
        jsoup.selectXpath("//li").forEach(listItem ->
                {
                    var aTagsWithId = listItem.selectXpath(".//a[@id]");
                    if (!aTagsWithId.isEmpty()) {
                        var aTagWithId = aTagsWithId.get(0);
                        String id = aTagWithId.id();
                        aTagWithId.id(String.format("anchor_%s", id));
                        listItem.id(id);
                    }
                }

        );
        try (PrintWriter out = new PrintWriter(outputHtmlPath)) {
            out.print(jsoup);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Can't overwrite resulting html", e);
        }
        return outputLog;
    }
}
