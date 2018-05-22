package com.amphiphile;

import java.io.IOException;
import java.util.ArrayList;

public class Main {


    public static void main(String[] args) {

        String adocFilePath = System.getProperty("input", "G:\\jprojects\\elibrary\\Prikaz514n\\index.adoc");
        Boolean parseListItems = Boolean.parseBoolean(System.getProperty("parse-list-items", "false"));
        Boolean parseCells = Boolean.parseBoolean(System.getProperty("parse-cells", "false"));

        Lurker lurker = new Lurker(adocFilePath, parseListItems, parseCells);
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk();

        System.out.printf("All blocks: %d%n", allBlocks.size());

        String outFilePath = System.getProperty("output", "G:\\jprojects\\elibrary\\Prikaz514n\\index_new.adoc");
        Boolean identifyListItems = Boolean.parseBoolean(System.getProperty("id-list-items", "false"));
        Boolean identifyCells = Boolean.parseBoolean(System.getProperty("id-cells", "false"));
        try {
            Extender extender = new Extender(adocFilePath, outFilePath, identifyListItems, identifyCells, allBlocks);
            extender.extend();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }


        System.out.println("ycnex!");

    }


}
