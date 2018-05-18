package com.amphiphile;

import java.util.ArrayList;

public class Main {


    public static void main(String[] args) {

        String adoc_file_path = System.getProperty("input", "G:\\jprojects\\elibrary\\Prikaz514n\\index.adoc");
        Lurker lurker = new Lurker();
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk(adoc_file_path);

        System.out.printf("All blocks: %d%n", allBlocks.size());
        System.out.println("ycnex!");

    }


}
