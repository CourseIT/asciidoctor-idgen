package com.amphiphile;

import java.io.IOException;
import java.util.ArrayList;

public class Main {


    public static void main(String[] args) {

        String adoc_file_path = System.getProperty("input", "G:\\jprojects\\elibrary\\Prikaz514n\\index.adoc");
        Lurker lurker = new Lurker();
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk(adoc_file_path);

        System.out.printf("All blocks: %d%n", allBlocks.size());

        try {
            Extender extender = new Extender();
            extender.extend(adoc_file_path, allBlocks);
        }catch (IOException e) {
            System.err.println(e.getMessage());
        }


        System.out.println("ycnex!");

    }


}
