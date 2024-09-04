package ru.curs.asciidoctor_idgen;

import jakarta.annotation.Nullable;
import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.ArrayList;

@SpringBootApplication
public class Main {

    public static void main(String[] args) throws ParseException, IOException {
        if (args.length == 0) {
            SpringApplication.run(Main.class, args);
        } else {
            Options options = new Options();
            options.addRequiredOption("i", "input", true, "input file to parse");
            options.addRequiredOption("o", "output", true, "output file to write");
            options.addRequiredOption("j", "json", true, "input file to parse");

            options.addOption("ohtml", "output-html", false, "Convert output file to html");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmdLine = parser.parse(options, args);

            String adocFilePath = cmdLine.getOptionValue("input");
            String outFilePath = cmdLine.getOptionValue("output");
            String jsonFilePath = cmdLine.getOptionValue("json");
            Boolean outputHtml = cmdLine.hasOption("ohtml");

            enrich(adocFilePath, outFilePath, jsonFilePath, outputHtml);
        }
    }

    public static void enrich(String adocFilePath, String outFilePath
            , String jsonFilePath, @Nullable Boolean outputHtml) throws IOException {
        Lurker lurker = new Lurker(adocFilePath);
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk();

        System.out.printf("Parsed successfully. All blocks: %d%n", allBlocks.size());

        Extender extender = new Extender(adocFilePath, outFilePath, jsonFilePath, allBlocks);
        extender.extend();

        System.out.println("Identified successfully.");

        if (outputHtml) {
            Converter converter = new Converter(outFilePath);
            converter.convert();
            System.out.println("Converted successfully.");
        }
    }
}
