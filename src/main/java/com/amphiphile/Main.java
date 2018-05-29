package com.amphiphile;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addRequiredOption("i", "input", true, "input file to parse");
        options.addRequiredOption("o", "output", true, "output file to write");
        options.addRequiredOption("j", "json", true, "input file to parse");

        options.addOption("pli", "parse-list-items", false, "Parse list items to separate elements");
        options.addOption("pbi", "parse-biblio-items", false, "Parse bibliography list items to separate elements");
        options.addOption("pc", "parse-cells", false, "Parse cells to separate element");
        options.addOption("ili", "id-list-items", false, "Identify list items");
        options.addOption("ibi", "id-biblio-items", false, "Identify bibliography list items");
        options.addOption("ic", "id-cells", false, "Identify cells");


        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(options, args);


        String adocFilePath = cmdLine.getOptionValue("input");

        Boolean parseListItems = Boolean.parseBoolean(cmdLine.getOptionValue("parse-list-items", "true"));
        Boolean parseBiblioItems = Boolean.parseBoolean(cmdLine.getOptionValue("parse-biblio-items", "true"));
        Boolean parseCells = Boolean.parseBoolean(cmdLine.getOptionValue("parse-cells", "false"));

        Lurker lurker = new Lurker(adocFilePath);
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk(parseListItems, parseBiblioItems, parseCells);

        System.out.printf("All blocks: %d%n", allBlocks.size());

        String outFilePath = cmdLine.getOptionValue("output");
        String jsonFilePath = cmdLine.getOptionValue("json");

        Boolean identifyListItems = Boolean.parseBoolean(cmdLine.getOptionValue("id-list-items", "true"));
        Boolean identifyBiblioItems = Boolean.parseBoolean(cmdLine.getOptionValue("id-biblio-items", "true"));
        Boolean identifyCells = Boolean.parseBoolean(cmdLine.getOptionValue("id-cells", "false"));

        Extender extender = new Extender(adocFilePath, outFilePath, jsonFilePath, allBlocks);
        extender.extend(identifyListItems, identifyBiblioItems, identifyCells);

        System.out.println("ycnex!");

    }
}
