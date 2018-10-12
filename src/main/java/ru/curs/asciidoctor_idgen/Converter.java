package ru.curs.asciidoctor_idgen;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.SafeMode;

import java.io.File;
import java.util.Map;

import static org.asciidoctor.OptionsBuilder.options;

class Converter {
    private String adocFilePath;

    Converter(String adocFilePath) {
        this.adocFilePath = adocFilePath;
    }

    void convert() {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        Map<String, Object> options = options().safe(SafeMode.UNSAFE)
                .asMap();

        asciidoctor.convertFile(new File(this.adocFilePath), options);
    }
}
