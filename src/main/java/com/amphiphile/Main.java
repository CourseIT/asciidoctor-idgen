package com.amphiphile;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.impl.BlockImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;


public class Main {

    static String adoc_file_path = "G:\\jprojects\\elibrary\\GLPS\\index.adoc";

    public static ArrayList<ExtendedBlock> unidentifiedBlocks = new ArrayList<>();

    public static void main(String[] args) {

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        Map<String, Object> options = OptionsBuilder.options().option("sourcemap", "true")
                .asMap();

        Document document = asciidoctor.loadFile(new File(adoc_file_path), options);


        touch(document);

        System.out.println("ycnex!");

    }


    public static void touch(StructuralNode block) {

        checkBlockId(block);


        if (block.getBlocks() != null) {
            for (StructuralNode abstractBlock : block.getBlocks()) {
                touch(abstractBlock);
            }
        }

    }

    public static void checkBlockId(StructuralNode block) {

        if (!block.getContext().equals("document")) {


            if (block.getId() == null) {

                addUnidentifiedBlock(block);

            }
        }
    }

    public static void addUnidentifiedBlock(StructuralNode unidentifiedBlock) {
        ExtendedBlock extendedBlock = new ExtendedBlock();
        extendedBlock.id = unidentifiedBlock.getId();
        extendedBlock.context = unidentifiedBlock.getContext();
        extendedBlock.sourceLine = unidentifiedBlock.getSourceLocation().getLineNumber();
        if (extendedBlock.context.equals("paragraph")) {
            extendedBlock.sourceText = ((BlockImpl) unidentifiedBlock).getSource();
        } else if (extendedBlock.context.equals("section")) {
            extendedBlock.title = unidentifiedBlock.getTitle();
        }


        unidentifiedBlocks.add(extendedBlock);

    }


}
