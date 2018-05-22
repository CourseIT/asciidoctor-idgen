package com.amphiphile;

import java.io.IOException;
import java.util.ArrayList;

public class Main {


    public static void main(String[] args) {

        String adocFilePath = System.getProperty("input", "G:\\jprojects\\elibrary\\Prikaz514n\\index.adoc");
        Lurker lurker = new Lurker(adocFilePath);
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk();

        System.out.printf("All blocks: %d%n", allBlocks.size());

        String outFilePath = System.getProperty("output", "G:\\jprojects\\elibrary\\Prikaz514n\\index_new.adoc");

        try {
            Extender extender = new Extender(adocFilePath, outFilePath, allBlocks);
            extender.extend();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }


        System.out.println("ycnex!");

    }


}
