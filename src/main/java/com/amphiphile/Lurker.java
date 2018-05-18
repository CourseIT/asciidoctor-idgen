package com.amphiphile;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.*;
import org.asciidoctor.ast.impl.BlockImpl;
import org.asciidoctor.ast.impl.ListImpl;
import org.asciidoctor.ast.impl.TableImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Lurker {
    private static ArrayList<ExtendedBlock> allBlocks = new ArrayList<>();

    private static void touch(StructuralNode block) {


        if (!(block.getContext().equals("document") ||
                block.getContext().equals("preamble"))) {

            addBlock(block);
        }

        if (block.getBlocks() != null) {
            for (StructuralNode abstractBlock : block.getBlocks()) {
                touch(abstractBlock);
            }

        }

    }

    private static void addBlock(StructuralNode block) {
        ExtendedBlock extendedBlock = new ExtendedBlock();

        extendedBlock.context = block.getContext();
        extendedBlock.id = block.getId();

        if (extendedBlock.id != null) {
            extendedBlock.isIdentified = true;
        } else {
            IdGenerator idGenerator = new IdGenerator();
            extendedBlock.id = String.join("_", extendedBlock.context, idGenerator.generateId(4));
        }
        extendedBlock.id = extendedBlock.id.toLowerCase();
        extendedBlock.style = (String) block.getAttributes().get("style");
        if (block.getSourceLocation() != null) {
            extendedBlock.sourceLine = block.getSourceLocation().getLineNumber();
        }
        switch (extendedBlock.context) {
            case "image":
                extendedBlock.target = (String) block.getAttributes().get("target");
                break;
            case "paragraph":
                extendedBlock.sourceText = ((BlockImpl) block).getSource();
                break;
            case "table":
                extendedBlock.title = block.getTitle();
                break;
        }
        if (!(extendedBlock.context.endsWith("list"))) {
            allBlocks.add(extendedBlock);
        }

        checkNestedItems(block);


    }

    private static void checkNestedItems(StructuralNode block) {
        if (block.getContext().endsWith("list")) {

            for (Object listItem : ((ListImpl) block).getItems()) {

                addListItem((ListItem) listItem, block);

            }
        } else if (block.getContext().equals("table")) {


            Table table = (TableImpl) block;

            for (Row header : table.getHeader()) {
                checkRows(header, table);

            }

            for (Row footer : table.getFooter()) {
                checkRows(footer, table);

            }

            for (Row body : table.getBody()) {
                checkRows(body, table);

            }

        }
    }

    private static void addListItem(ListItem listItem, StructuralNode parent) {
        ExtendedBlock extendedBlock = new ExtendedBlock();


        extendedBlock.context = listItem.getContext();
        extendedBlock.id = getInlineId(listItem.getSource(), parent);

        if (extendedBlock.id != null) {
            extendedBlock.isIdentified = true;
        } else {
            IdGenerator idGenerator = new IdGenerator();
            extendedBlock.id = String.join("_", extendedBlock.context, idGenerator.generateId(4));
        }
        extendedBlock.id = extendedBlock.id.toLowerCase();

        if (listItem.getSourceLocation() != null) {
            extendedBlock.sourceLine = listItem.getSourceLocation().getLineNumber();
        } else {
            extendedBlock.marker = listItem.getMarker();
        }
        extendedBlock.sourceText = listItem.getSource();

        allBlocks.add(extendedBlock);


    }

    private static String getInlineId(String sourceText, StructuralNode parent) {

        String result = null;

        String parentStyle = (String) parent.getAttributes().get("style");

        if (parentStyle != null && parentStyle.equals("bibliography")) {
            Pattern InlineBiblioAnchorRx = Pattern.compile("^\\[\\[\\[([\\p{Alpha}_:][\\w:.-]*)(?:, *(.+?))?]]].*");
            Matcher m = InlineBiblioAnchorRx.matcher(sourceText);
            if (m.matches()) {
                result = m.group(1);
            }

        } else {

            Pattern InlineAnchorRx = Pattern.compile("(?:\\\\)?(?:\\[\\[([\\p{Alpha}_:][\\w:.-]*)(?:, *(.+?))?]]|" +
                    "anchor:([\\p{Alpha}_:][\\w:.-]*)\\[(?:]|(.*?[^\\\\])])).*");
            Matcher m = InlineAnchorRx.matcher(sourceText);
            if (m.matches()) {
                if (m.group(1) != null) {
                    result = m.group(1);
                } else {
                    result = m.group(3);
                }
            }
        }


        return result;
    }

    private static void checkRows(Row row, Table parent) {
        for (Cell cell : row.getCells()) {
            addCellItem(cell, parent);

        }
    }

    private static void addCellItem(Cell cell, Table parent) {
        Object style = cell.getAttributes().get("style");

        if (style != null) {

            if (style.toString().equals("asciidoc")) {
                Asciidoctor asciidoctor = Asciidoctor.Factory.create();
                Map<String, Object> options = OptionsBuilder.options().option("sourcemap", "true")
                        .option(Asciidoctor.STRUCTURE_MAX_LEVEL, 2)
                        .asMap();

                Document document = asciidoctor.load(cell.getSource(), options);
                touch(document);
            }
        } else {
            ExtendedBlock extendedBlock = new ExtendedBlock();

            extendedBlock.context = cell.getContext();
            extendedBlock.id = getInlineId(cell.getSource(), parent);

            if (extendedBlock.id != null) {
                extendedBlock.isIdentified = true;
            } else {
                IdGenerator idGenerator = new IdGenerator();
                extendedBlock.id = String.join("_", extendedBlock.context, idGenerator.generateId(4));
            }
            extendedBlock.id = extendedBlock.id.toLowerCase();
            extendedBlock.sourceText = cell.getSource();
            allBlocks.add(extendedBlock);
        }
    }

    ArrayList<ExtendedBlock> lurk(String path) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        Map<String, Object> options = OptionsBuilder.options().option("sourcemap", "true")
                .option(Asciidoctor.STRUCTURE_MAX_LEVEL, 2)
                .asMap();

        Document document = asciidoctor.loadFile(new File(path), options);
        touch(document);
        return allBlocks;
    }


}
