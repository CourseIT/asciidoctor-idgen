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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {

    private static ArrayList<ExtendedBlock> allBlocks = new ArrayList<>();
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

            addBlock(block);
            checkNestedItems(block);
        }

    }

    private static void addBlock(StructuralNode block) {
        ExtendedBlock extendedBlock = new ExtendedBlock();

        extendedBlock.id = block.getId();
        extendedBlock.context = block.getContext();
        if (block.getSourceLocation() != null) {
            extendedBlock.sourceLine = block.getSourceLocation().getLineNumber();
        }
        switch (extendedBlock.context) {
            case "image":
                extendedBlock.target = (String) block.getAttributes().get("target");
                break;
            case "paragraph":
                extendedBlock.sourceText = ((BlockImpl) block).getSource();
                checkFootnotes(extendedBlock.sourceText);
                break;
            case "table"://TODO: don't forget me
                break;

        }
//        if (!(extendedBlock.context.equals("section") ||
//                extendedBlock.context.endsWith("list") ||
//                extendedBlock.context.equals("table"))) {
        allBlocks.add(extendedBlock);
//        }
        if (extendedBlock.id == null) {
            unidentifiedBlocks.add(extendedBlock);
        }

        checkNestedItems(block);


    }

    private static void checkNestedItems(StructuralNode block) {
        if (block.getContext().endsWith("list")) {

            for (Object listItem : ((ListImpl) block).getItems()) {

                addListItem((ListItem) listItem);

            }
        } else if (block.getContext().equals("table")) {
            // TODO: table
        }
    }

    private static void addListItem(ListItem listItem) {
        ExtendedBlock extendedBlock = new ExtendedBlock();

        extendedBlock.id = getListItemId(listItem);

        extendedBlock.context = listItem.getContext();
        if (listItem.getSourceLocation() != null) {
            extendedBlock.sourceLine = listItem.getSourceLocation().getLineNumber();
        } else {
            extendedBlock.marker = listItem.getMarker();
        }
        extendedBlock.sourceText = listItem.getSource();

        checkFootnotes(extendedBlock.sourceText);

//        if (!(extendedBlock.context.equals("section") ||
//                extendedBlock.context.endsWith("list") ||
//                extendedBlock.context.equals("table"))) {
        allBlocks.add(extendedBlock);
//        }
        if (extendedBlock.id == null) {
            unidentifiedBlocks.add(extendedBlock);
        }


    }

    private static String getListItemId(ListItem listItem) {
        Pattern InlineAnchorRx = Pattern.compile("(?:\\\\)?(?:\\[\\[([\\p{Alpha}_:][\\w:.-]*)(?:, *(.+?))?]]|anchor:([\\p{Alpha}_:][\\w:.-]*)\\[(?:]|(.*?[^\\\\])])).*");
        Matcher m = InlineAnchorRx.matcher(listItem.getSource());
        if (m.matches()) {
            if (m.group(1) != null) {
                return m.group(1);
            } else {
                return m.group(3);
            }
        } else
            return null;
    }

    private static void checkFootnotes(String sourceText) {

        if (sourceText.contains("footnote")) {//TODO: убрать условие

            Pattern InlineFootnoteMacroRx = Pattern.compile(".*\\\\?(footnote(?:ref)?):\\[(.*?[^\\\\])].*", Pattern.MULTILINE);
            Matcher m = InlineFootnoteMacroRx.matcher(sourceText);

            if (m.matches()) {
                if (m.group(1).equals("footnote")) {
                    System.out.println("footnote");
                    String footnoteId = null;
                    String footnoteText = m.group(2);
                } else {
                    System.out.println("footnoteref");
                }

            }
        }
    }


//TODO: add footnote as block
}
