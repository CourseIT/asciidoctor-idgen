package com.amphiphile;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.impl.BlockImpl;
import org.asciidoctor.ast.impl.ListImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;


public class Main {

    private static ArrayList<ExtendedBlock> unidentifiedBlocks = new ArrayList<>();

    public static void main(String[] args) {

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        Map<String, Object> options = OptionsBuilder.options().option("sourcemap", "true")
                .asMap();

        String adoc_file_path = System.getProperty("input", "G:\\jprojects\\elibrary\\Prikaz514n\\index.adoc");

        Document document = asciidoctor.loadFile(new File(adoc_file_path), options);

        touch(document);


        System.out.printf("Unidentified blocks: %d%n", unidentifiedBlocks.size());

        System.out.println("ycnex!");

    }


    private static void touch(StructuralNode block) {

        checkBlockId(block);


        if (block.getBlocks() != null) {
            for (StructuralNode abstractBlock : block.getBlocks()) {
                touch(abstractBlock);
            }

        }

    }

    private static void checkBlockId(StructuralNode block) {

        if (!(block.getContext().equals("document") ||
                block.getContext().equals("preamble"))) {

            if (block.getId() == null) {
                addUnidentifiedBlock(block);
            } else {
                checkNestedItemsId(block);
            }
        }

    }

    private static void addUnidentifiedBlock(StructuralNode unidentifiedBlock) {
        ExtendedBlock extendedBlock = new ExtendedBlock();
        extendedBlock.id = unidentifiedBlock.getId();
        extendedBlock.context = unidentifiedBlock.getContext();
        if (unidentifiedBlock.getSourceLocation() != null) {
            extendedBlock.sourceLine = unidentifiedBlock.getSourceLocation().getLineNumber();
        }
        switch (extendedBlock.context) {
            case "image":
                extendedBlock.target = (String) unidentifiedBlock.getAttributes().get("target");
                break;
            case "paragraph":
                extendedBlock.sourceText = ((BlockImpl) unidentifiedBlock).getSource();
                break;
            case "table"://TODO: don't forget me
                break;

        }

        unidentifiedBlocks.add(extendedBlock);

        checkNestedItemsId(unidentifiedBlock);


    }

    private static void checkNestedItemsId(StructuralNode block) {
        if (block.getContext().endsWith("list")) {

            for (Object listItem : ((ListImpl) block).getItems()) {
//                FIXME: фильтровать с Id == null
                addListItem((ListItem) listItem);
            }
        } else if (block.getContext().equals("table")) {
            // TODO: table
            System.out.println("TODO");
        }
    }


    private static void addListItem(ListItem listItem) {
        ExtendedBlock extendedBlock = new ExtendedBlock();

        extendedBlock.id = listItem.getId(); // FIXME: Так не работает, надо разбирать строку
        extendedBlock.context = listItem.getContext();
        if (listItem.getSourceLocation() != null) {
            extendedBlock.sourceLine = listItem.getSourceLocation().getLineNumber();
        } else {
            extendedBlock.marker = listItem.getMarker();
        }
        extendedBlock.sourceText = listItem.getSource();

        unidentifiedBlocks.add(extendedBlock);

    }
}
