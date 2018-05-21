package com.amphiphile;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.*;
import org.asciidoctor.ast.impl.BlockImpl;
import org.asciidoctor.ast.impl.ListImpl;
import org.asciidoctor.ast.impl.TableImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Lurker {
    private static ArrayList<ExtendedBlock> allBlocks = new ArrayList<>();
    private String path;

    Lurker(String path) {
        this.path = path;
    }

    private static void touch(StructuralNode block, Boolean isEmbeddedDoc) {


        if (!(block.getContext().equals("document") ||
                block.getContext().equals("preamble"))) {

            addBlock(block, isEmbeddedDoc);
        }

        if (block.getBlocks() != null) {
            for (StructuralNode abstractBlock : block.getBlocks()) {
                touch(abstractBlock, isEmbeddedDoc);
            }

        }

    }

    private static void addBlock(StructuralNode block, Boolean isEmbeddedDoc) {
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
        extendedBlock.isEmbeddedDoc = isEmbeddedDoc;
        extendedBlock.style = (String) block.getAttributes().get("style");
        if (block.getSourceLocation() != null) {
            extendedBlock.sourceLine = block.getSourceLocation().getLineNumber();
        }
        switch (extendedBlock.context) {
            case "image":
                extendedBlock.target = (String) block.getAttributes().get("target");
                extendedBlock.title = block.getTitle();
                break;
            case "paragraph":
                extendedBlock.sourceText = ((BlockImpl) block).getSource();
                break;
            case "table":
                extendedBlock.title = block.getTitle();
                break;
            case "section":
                extendedBlock.title = block.getTitle();
                break;
        }

        allBlocks.add(extendedBlock);

        Map<String, String> blockParams = new HashMap<>();
        blockParams.put("id", extendedBlock.id);
        blockParams.put("style", extendedBlock.style);
        blockParams.put("isEmbeddedDoc", extendedBlock.isEmbeddedDoc.toString());

        checkNestedItems(block, blockParams);

    }

    private static void checkNestedItems(StructuralNode block, Map blockParams) {
        if (block.getContext().endsWith("list")) {

            for (Object listItem : ((ListImpl) block).getItems()) {

                addListItem((ListItem) listItem, blockParams);

            }
        } else if (block.getContext().equals("table")) {


            Table table = (TableImpl) block;

            for (Row header : table.getHeader()) {
                checkRows(header, blockParams);

            }

            for (Row footer : table.getFooter()) {
                checkRows(footer, blockParams);

            }

            for (Row body : table.getBody()) {
                checkRows(body, blockParams);

            }

        }
    }

    private static void addListItem(ListItem listItem, Map listParams) {
        ExtendedBlock extendedBlock = new ExtendedBlock();


        extendedBlock.context = listItem.getContext();
        extendedBlock.id = getInlineId(listItem.getSource(), listParams);

        if (extendedBlock.id != null) {
            extendedBlock.isIdentified = true;
        } else {
            IdGenerator idGenerator = new IdGenerator();
            extendedBlock.id = String.join("_", extendedBlock.context, idGenerator.generateId(4));
        }
        extendedBlock.id = extendedBlock.id.toLowerCase();
        extendedBlock.parentId = listParams.get("id").toString();
        extendedBlock.isEmbeddedDoc = Boolean.parseBoolean(listParams.get("isEmbeddedDoc").toString());
        if (listItem.getSourceLocation() != null) {
            extendedBlock.sourceLine = listItem.getSourceLocation().getLineNumber();
        } else {
            extendedBlock.marker = listItem.getMarker();
        }
        extendedBlock.sourceText = listItem.getSource();

        allBlocks.add(extendedBlock);

        if (listItem.getBlocks().size() != 0) {
            for (StructuralNode listBlock : listItem.getBlocks()) {
                addBlock(listBlock, Boolean.parseBoolean(listParams.get("isEmbeddedDoc").toString()));
            }
        }


    }

    private static String getInlineId(String sourceText, Map blockParams) {

        String result = null;

        if (blockParams.get("style") != null && blockParams.get("style").toString().equals("bibliography")) {
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

    private static void checkRows(Row row, Map tableParams) {
        for (Cell cell : row.getCells()) {
            addCellItem(cell, tableParams);

        }
    }

    private static void addCellItem(Cell cell, Map tableParams) {
        Object style = cell.getAttributes().get("style");

        if (style != null) {

            if (style.toString().equals("asciidoc")) {
                Asciidoctor asciidoctor = Asciidoctor.Factory.create();
                Map<String, Object> options = OptionsBuilder.options()
                        .option("sourcemap", "true")
                        .option("catalog_assets", "true")
                        .option(Asciidoctor.STRUCTURE_MAX_LEVEL, 4)
                        .asMap();

                Document document = asciidoctor.load(cell.getSource(), options);
                touch(document, true);
            }
        } else {
            ExtendedBlock extendedBlock = new ExtendedBlock();

            extendedBlock.context = cell.getContext();
            extendedBlock.id = getInlineId(cell.getSource(), tableParams);

            if (extendedBlock.id != null) {
                extendedBlock.isIdentified = true;
            } else {
                IdGenerator idGenerator = new IdGenerator();
                extendedBlock.id = String.join("_", extendedBlock.context, idGenerator.generateId(4));
            }
            extendedBlock.id = extendedBlock.id.toLowerCase();
            extendedBlock.parentId = tableParams.get("id").toString();
            extendedBlock.isEmbeddedDoc = Boolean.parseBoolean(tableParams.get("isEmbeddedDoc").toString());
            extendedBlock.sourceText = cell.getSource();
            allBlocks.add(extendedBlock);
        }
    }

    ArrayList<ExtendedBlock> lurk() {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        Map<String, Object> options = OptionsBuilder.options()
                .option("sourcemap", "true")
                .option("catalog_assets", "true")
                .option(Asciidoctor.STRUCTURE_MAX_LEVEL, 4)
                .asMap();

        Document document = asciidoctor.loadFile(new File(path), options);
        touch(document, false);
        return allBlocks;
    }

}
