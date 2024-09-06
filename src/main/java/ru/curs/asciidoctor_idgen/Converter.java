package ru.curs.asciidoctor_idgen;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import static org.asciidoctor.OptionsBuilder.options;

class Converter {
    private String adocFilePath;

    Converter(String adocFilePath) {
        this.adocFilePath = adocFilePath;
    }

    String convert() {
        final ArrayList<String> logOutput = new ArrayList<>();
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.registerLogHandler(logRecord -> {
            var fullLogMessage =
                    String.format("%s: %s", logRecord.getSeverity(), logRecord.getMessage());
            logOutput.add(fullLogMessage);
            System.out.println(fullLogMessage);
        });

        Map<String, Object> options = options().safe(SafeMode.UNSAFE)
                .asMap();

        asciidoctor.convertFile(new File(this.adocFilePath), options);
        return String.join("\n", logOutput);
    }
}
