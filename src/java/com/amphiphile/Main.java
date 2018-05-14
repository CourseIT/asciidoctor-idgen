package com.amphiphile;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {

    static String adoc_file_path = "G:\\jprojects\\elibrary\\GLPS\\index.adoc";

    public static void main(String[] args) {

//        Asciidoctor asciidoctor = JRubyAsciidoctor.create();
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        Map options = OptionsBuilder.options().option("sourcemap", "true")
//                .option(Asciidoctor.STRUCTURE_MAX_LEVEL, 10)
                .asMap();

        Document document = asciidoctor.loadFile(new File(adoc_file_path), options);



        touch(document);

        System.out.println("ycnex!");

    }


    public static void touch(StructuralNode block) {


        if (block.getBlocks() != null) {
            for (StructuralNode abstractBlock : block.getBlocks()) {
                touch(abstractBlock);
            }
        }
    }

}
