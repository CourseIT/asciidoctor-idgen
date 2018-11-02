package ru.curs.asciidoctor_idgen;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addRequiredOption("i", "input", true, "input file to parse");
        options.addRequiredOption("o", "output", true, "output file to write");
        options.addRequiredOption("j", "json", true, "input file to parse");

        options.addOption("ohtml", "output-html", false, "Convert output file to html");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(options, args);

        String adocFilePath = cmdLine.getOptionValue("input");


        Lurker lurker = new Lurker(adocFilePath);
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk();

        System.out.printf("Parsed successfully. All blocks: %d%n", allBlocks.size());

        String outFilePath = cmdLine.getOptionValue("output");
        String jsonFilePath = cmdLine.getOptionValue("json");


        Extender extender = new Extender(adocFilePath, outFilePath, jsonFilePath, allBlocks);
        extender.extend();

        System.out.println("Identified successfully.");

        if (cmdLine.hasOption("ohtml")) {
            Converter converter = new Converter(outFilePath);
            converter.convert();
            System.out.println("Converted successfully.");
        }


    }
}
