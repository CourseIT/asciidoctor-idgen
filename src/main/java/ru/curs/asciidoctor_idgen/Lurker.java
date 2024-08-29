package ru.curs.asciidoctor_idgen;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.*;
import org.asciidoctor.jruby.ast.impl.BlockImpl;
import org.asciidoctor.jruby.ast.impl.DescriptionListImpl;
import org.asciidoctor.jruby.ast.impl.ListImpl;
import org.asciidoctor.jruby.ast.impl.TableImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

class Lurker {
    private static ArrayList<ExtendedBlock> allBlocks = new ArrayList<>();
    private String path;

    Lurker(String path) {
        this.path = path;
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

        identify(extendedBlock);
        extendedBlock.isEmbeddedDoc = isEmbeddedDoc;
        extendedBlock.style = (String) DefaultValueHandler.getValueOrDefault(block.getAttributes().get("style"), "");

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
                extendedBlock.htmlText = escapeHtml4((String) block.getContent());

                break;
            case "section":
                extendedBlock.title = block.getTitle();

                break;
            case "table":
                extendedBlock.title = block.getTitle();
                extendedBlock.sourceText = getTableSource((TableImpl) block);
                extendedBlock.htmlText = escapeHtml4(block.convert());
                break;
        }
        extendedBlock.docTitle = block.getDocument().getDoctitle();
        allBlocks.add(extendedBlock);

        if (extendedBlock.context.endsWith("list")) {

            Map<String, String> blockParams = new HashMap<>();
            blockParams.put("id", extendedBlock.id);
            blockParams.put("style", DefaultValueHandler.getValueOrDefault(extendedBlock.style, ""));
            blockParams.put("isEmbeddedDoc", extendedBlock.isEmbeddedDoc.toString());
            blockParams.put("docTitle", extendedBlock.docTitle);

            addNestedItems(block, blockParams);
        }


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


            if (block instanceof ListImpl) {
                for (Object listItem : ((ListImpl) block).getItems()) {
                    addListItem((ListItem) listItem, blockParams);
                }
            } else if (block instanceof DescriptionListImpl) {
                for (Object listItem : ((DescriptionListImpl) block).getItems()) {
                    addListItem((DescriptionListEntry) listItem, blockParams);

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

        identify(extendedBlock);
        extendedBlock.parentId = listParams.get("id").toString();
        extendedBlock.isEmbeddedDoc = Boolean.parseBoolean(listParams.get("isEmbeddedDoc").toString());
        if (listItem.getSourceLocation() != null) {
            extendedBlock.sourceLine = listItem.getSourceLocation().getLineNumber();
            extendedBlock.marker = listItem.getMarker();
        } else {
            extendedBlock.marker = listItem.getMarker();
        }
        extendedBlock.sourceText = listItem.getSource();
        extendedBlock.htmlText = escapeHtml4(listItem.getText());

        extendedBlock.docTitle = listItem.getDocument().getDoctitle();
        allBlocks.add(extendedBlock);

        if (listItem.getBlocks().size() != 0) {
            for (StructuralNode listBlock : listItem.getBlocks()) {
                addBlock(listBlock, Boolean.parseBoolean(listParams.get("isEmbeddedDoc").toString()));
            }
        }

    }

    private void identify(ExtendedBlock extendedBlock) {
        if (extendedBlock.id != null) {
            extendedBlock.isIdentified = true;
        } else {
            IdGenerator idGenerator = new IdGenerator();
            extendedBlock.id = String.join("_", extendedBlock.context, idGenerator.generateId(6));
            extendedBlock.id = extendedBlock.id.toLowerCase();
        }
    }

    private void addListItem(DescriptionListEntry listItem, Map listParams) {
        ExtendedBlock extendedBlock = new ExtendedBlock();

        extendedBlock.context = "list_item";

        ListItem term = listItem.getTerms().get(0);//TODO: multiple terms;
        ListItem description = listItem.getDescription();

        extendedBlock.term = term.getSource();
        extendedBlock.description = description.getSource();

        extendedBlock.sourceText = String.format("%s:: %s",
                extendedBlock.term, extendedBlock.description);

        extendedBlock.htmlText = String.format("%s %s",
                escapeHtml4(term.getText()), escapeHtml4(description.getText()));
        extendedBlock.id = getInlineId(extendedBlock.sourceText, listParams);

        identify(extendedBlock);
        extendedBlock.parentId = listParams.get("id").toString();
        extendedBlock.isEmbeddedDoc = Boolean.parseBoolean(listParams.get("isEmbeddedDoc").toString());

        extendedBlock.docTitle = listParams.get("docTitle").toString();
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
                Options options = Options.builder()
                        .sourcemap(true)
                        .catalogAssets(true)
                        .build();
                // deleted .option(Asciidoctor.STRUCTURE_MAX_LEVEL, 4)
                Document document = asciidoctor.load(cell.getSource(), options);
                touch(document, true);
            }
        } else {
            ExtendedBlock extendedBlock = new ExtendedBlock();

            extendedBlock.context = cell.getContext();
            extendedBlock.id = getInlineId(cell.getSource(), tableParams);

            identify(extendedBlock);
            extendedBlock.parentId = tableParams.get("id").toString();
            extendedBlock.isEmbeddedDoc = Boolean.parseBoolean(tableParams.get("isEmbeddedDoc").toString());
            extendedBlock.sourceText = cell.getSource();
            extendedBlock.htmlText = escapeHtml4(cell.getText());
            extendedBlock.docTitle = cell.getDocument().getDoctitle();
            allBlocks.add(extendedBlock);
        }
    }


    ArrayList<ExtendedBlock> lurk() {

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("table-caption", "Таблица");

        Options options = Options.builder()
                .sourcemap(true)
                .catalogAssets(true)
                .attributes(attributes)
                .build();
        // deleted .option(Asciidoctor.STRUCTURE_MAX_LEVEL, 4)

        Document document = asciidoctor.loadFile(new File(path), options);
        touch(document, false);

        return allBlocks;
    }

}
