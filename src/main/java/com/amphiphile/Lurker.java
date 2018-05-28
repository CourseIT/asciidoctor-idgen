package com.amphiphile;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.*;
import org.asciidoctor.ast.impl.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Lurker {
    private static ArrayList<ExtendedBlock> allBlocks = new ArrayList<>();
    private Boolean parseListItems;
    private Boolean parseBiblioItems;
    private Boolean parseCells;
    private String path;

    Lurker(String path) {
        this.path = path;
        this.parseListItems = false;
        this.parseBiblioItems = false;
        this.parseCells = false;
    }

    private void touch(StructuralNode block, Boolean isEmbeddedDoc) {


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

    private void addBlock(StructuralNode block, Boolean isEmbeddedDoc) {
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
        extendedBlock.style = (String) DefaultValueHandler.getValueOrDefault(block.getAttributes().get("style"), "");

        if (block.getSourceLocation() != null) {
            extendedBlock.sourceLine = block.getSourceLocation().getLineNumber();
        }
        if (extendedBlock.context.equals("image")) {
            extendedBlock.target = (String) block.getAttributes().get("target");
            extendedBlock.title = block.getTitle();

        } else if (extendedBlock.context.equals("paragraph")) {
            extendedBlock.sourceText = ((BlockImpl) block).getSource();

        } else if (extendedBlock.context.equals("section")) {
            extendedBlock.title = block.getTitle();

        } else if (extendedBlock.context.endsWith("list")) {
            if (!this.parseListItems) {

                if (block instanceof ListImpl) {
                    extendedBlock.sourceText = getListSource((ListImpl) block);
                } else if (block instanceof DescriptionListImpl) {
                    extendedBlock.sourceText = getListSource((DescriptionListImpl) block);
                }
            }
        } else if (extendedBlock.context.equals("table")) {
            extendedBlock.title = block.getTitle();
            if (!this.parseCells) {
                extendedBlock.sourceText = getTableSource((TableImpl) block);
            }
        }

        allBlocks.add(extendedBlock);

        if (extendedBlock.context.endsWith("list") && (this.parseListItems || this.parseBiblioItems) ||
                extendedBlock.context.equals("table") && this.parseCells) {

            Map<String, String> blockParams = new HashMap<>();
            blockParams.put("id", extendedBlock.id);
            blockParams.put("style", DefaultValueHandler.getValueOrDefault(extendedBlock.style, ""));
            blockParams.put("isEmbeddedDoc", extendedBlock.isEmbeddedDoc.toString());

            addNestedItems(block, blockParams);
        }


    }

    private String getListSource(ListImpl list) {
        String sourceText = "";

        for (Object listItem : list.getItems()) {
            ListItem item = (ListItem) listItem;
            String itemSourceText = String.format("%s %s", item.getMarker(), item.getSource());

            if (!sourceText.equals("")) {
                sourceText = String.join("\n\n", sourceText, itemSourceText);
            } else {
                sourceText = itemSourceText;
            }

            if (item.getBlocks().size() != 0) {
                for (StructuralNode listBlock : item.getBlocks()) {
                    addBlock(listBlock, false);
                }
            }

        }

        return sourceText;
    }

    private String getListSource(DescriptionListImpl list) {
        String sourceText = "";

        for (Object listItem : list.getItems()) {
            DescriptionListEntryImpl item = (DescriptionListEntryImpl) listItem;

            String itemSourceText = String.format("%s:: %s",
                    item.getTerms().get(0).getSource(), item.getDescription().getSource());//TODO: multiple terms

            if (!sourceText.equals("")) {
                sourceText = String.join("\n\n", sourceText, itemSourceText);
            } else {
                sourceText = itemSourceText;
            }
        }

        return sourceText;
    }

    private String getTableSource(TableImpl table) {
        String sourceText = "|===";

        String rowSourceText;
        for (Row header : table.getHeader()) {

            rowSourceText = getRowSource(header);
            if (!rowSourceText.equals("")) {
                sourceText = String.join("\n\n", sourceText, rowSourceText);
            }

        }

        for (Row footer : table.getFooter()) {
            rowSourceText = getRowSource(footer);
            if (!rowSourceText.equals("")) {
                sourceText = String.join("\n\n", sourceText, rowSourceText);
            }
        }

        for (Row body : table.getBody()) {

            rowSourceText = getRowSource(body);
            if (!rowSourceText.equals("")) {
                sourceText = String.join("\n\n", sourceText, rowSourceText);
            }
        }

        sourceText = String.join("\n", sourceText, "|===");

        return sourceText;
    }

    private String getRowSource(Row row) {
        String rowSource = "";
        for (Cell cell : row.getCells()) {
            String cellSource = cell.getSource();

            Object style = cell.getAttributes().get("style");

            if (style != null) {

                if (style.toString().equals("asciidoc")) {
                    cellSource = cellSource + "\n";

                }
            }
            rowSource = String.join("|", rowSource, cellSource);
        }
        return rowSource;
    }

    private void addNestedItems(StructuralNode block, Map blockParams) {
        if (block.getContext().endsWith("list")) {

            if (this.parseListItems ||
                    this.parseBiblioItems && blockParams.get("style").toString().equals("bibliography")) {

                if (block instanceof ListImpl) {
                    for (Object listItem : ((ListImpl) block).getItems()) {
                        addListItem((ListItem) listItem, blockParams);
                    }
                } else if (block instanceof DescriptionListImpl) {
                    for (Object listItem : ((DescriptionListImpl) block).getItems()) {
                        addListItem((DescriptionListEntry) listItem, blockParams);

                    }
                }
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

    private void addListItem(ListItem listItem, Map listParams) {
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

    private void addListItem(DescriptionListEntry listItem, Map listParams) {
        ExtendedBlock extendedBlock = new ExtendedBlock();

        extendedBlock.context = "list_item";

        extendedBlock.sourceText = String.format("%s:: %s",
                listItem.getTerms().get(0).getSource(), listItem.getDescription().getSource());//TODO: multiple terms;
        extendedBlock.id = getInlineId(extendedBlock.sourceText, listParams);

        if (extendedBlock.id != null) {
            extendedBlock.isIdentified = true;
        } else {
            IdGenerator idGenerator = new IdGenerator();
            extendedBlock.id = String.join("_", extendedBlock.context, idGenerator.generateId(4));
        }
        extendedBlock.id = extendedBlock.id.toLowerCase();
        extendedBlock.parentId = listParams.get("id").toString();
        extendedBlock.isEmbeddedDoc = Boolean.parseBoolean(listParams.get("isEmbeddedDoc").toString());

        allBlocks.add(extendedBlock);

    }

    private String getInlineId(String sourceText, Map blockParams) {

        String result = null;

        String beginText = sourceText.split("\\r?\\n")[0];

        if (blockParams.get("style") != null && blockParams.get("style").toString().equals("bibliography")) {
            Pattern InlineBiblioAnchorRx = Pattern.compile("^\\[\\[\\[([\\p{Alpha}_:][\\w:.-]*)(?:, *(.+?))?]]].*");
            Matcher m = InlineBiblioAnchorRx.matcher(beginText);
            if (m.matches()) {
                result = m.group(1);
            }

        } else {

            Pattern InlineAnchorRx = Pattern.compile("(?:\\\\)?(?:\\[\\[([\\p{Alpha}_:][\\w:.-]*)(?:, *(.+?))?]]|" +
                    "anchor:([\\p{Alpha}_:][\\w:.-]*)\\[(?:]|(.*?[^\\\\])])).*");
            Matcher m = InlineAnchorRx.matcher(beginText);
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

    private void checkRows(Row row, Map tableParams) {
        for (Cell cell : row.getCells()) {
            addCellItem(cell, tableParams);

        }
    }

    private void addCellItem(Cell cell, Map tableParams) {
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

    ArrayList<ExtendedBlock> lurk(Boolean parseListItems, boolean parseBiblioItems, Boolean parseCells) {

        this.parseListItems = parseListItems;
        this.parseBiblioItems = parseBiblioItems;
        this.parseCells = parseCells;
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
