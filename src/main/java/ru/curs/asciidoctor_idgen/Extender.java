package ru.curs.asciidoctor_idgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Extender {
    private String outPath;
    private String jsonFilePath;
    private String copyPath;
    private ArrayList<ExtendedBlock> allBlocks;
    private List<String> lines; //список строк файла
    private int shift = 0; //количество новых строк, которое было вставлено в документ, относительно исходного

    Extender(String path, String outPath, String jsonFilePath, ArrayList<ExtendedBlock> allBlocks) throws IOException {
        this.outPath = outPath;
        this.jsonFilePath = jsonFilePath;

        this.allBlocks = allBlocks;

        this.lines = new ArrayList<>();

        int lastDot = path.lastIndexOf('.');
        this.copyPath = path.substring(0, lastDot) + "_copy" + path.substring(lastDot);

        Files.copy((new File(path)).toPath(), (new File(this.copyPath)).toPath(), StandardCopyOption.REPLACE_EXISTING);

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(new File(this.copyPath)), StandardCharsets.UTF_8));
        String nextLine;
        while ((nextLine = bufferedReader.readLine()) != null) {
            this.lines.add(nextLine);
        }
        bufferedReader.close();

    }

    void extend() throws IOException {


        int all_block_idx = 0;
        for (ExtendedBlock extendedBlock : allBlocks) {
            if (!extendedBlock.isIdentified && !extendedBlock.isEmbeddedDoc) {
                if (extendedBlock.context.equals("paragraph")
//                        || extendedBlock.context.endsWith("list") && !extendedBlock.style.equals("bibliography")
                ) {
                    int paragraphAnchorLineIdx = extendedBlock.sourceLine + shift - 1;
                    String paragraphAnchorLine = "";
                    if (paragraphAnchorLineIdx <= this.lines.size() - 1) {
                        paragraphAnchorLine = this.lines.get(paragraphAnchorLineIdx).trim();
                    }

                    if (!(extendedBlock.sourceText.startsWith(paragraphAnchorLine)) || paragraphAnchorLine.isEmpty()) {

                        if (!paragraphAnchorLine.startsWith("include:")) {
                            paragraphAnchorLineIdx = fixParagraphAnchorLineIdx(all_block_idx, extendedBlock, paragraphAnchorLineIdx, shift);
                        }
                    }

                    try {
                        this.lines.add(paragraphAnchorLineIdx, "[[" + extendedBlock.id + "]]");
                        shift += 1;
                        extendedBlock.isIdentified = true;
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println(String.format("Source line: %s, Shift %s, Error message: %s", extendedBlock.sourceLine, shift, e.getMessage()));
                    }
                } else if (extendedBlock.context.equals("image")) {
                    this.lines.add(extendedBlock.sourceLine + shift - 1, "[[" + extendedBlock.id + "]]");
                    shift += 1;
                    extendedBlock.isIdentified = true;
                } else if (extendedBlock.context.equals("table")) {
                    this.lines.add(extendedBlock.sourceLine + shift - 1 - 1, "[[" + extendedBlock.id + "]]");
                    shift += 1;
                    extendedBlock.isIdentified = true;
                } else if (extendedBlock.context.equals("cell")) {
                    addNestedId(all_block_idx, extendedBlock);

                } else if (extendedBlock.context.contains("list_item")) {

                    addNestedId(all_block_idx, extendedBlock);
                }
            }
            all_block_idx++;
        }

        if (jsonFilePath != null) {

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(jsonFilePath)), StandardCharsets.UTF_8))) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.disableHtmlEscaping();
                gsonBuilder.setPrettyPrinting();
                Gson gson = gsonBuilder.create();
                gson.toJson(allBlocks, writer);

            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }

        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(new File(outPath)), StandardCharsets.UTF_8));

        for (String line : this.lines) {
            out.append(line).append("\r\n");
        }

        out.flush();
        out.close();

        Files.delete((new File(this.copyPath).toPath()));
    }

    /**
     * When paragraph is inside a list and escaped by +, sourceLine value is incorrect
     * Not quite elegant solution above
     */
    private int fixParagraphAnchorLineIdx(int all_block_idx, ExtendedBlock extendedBlock, int paragraphAnchorLineIdx, int shift) {

        int newParagraphAnchorLineIdx = paragraphAnchorLineIdx;

        ExtendedBlock prevIdentifiedBlock = getPrevIdentifiedBlock(all_block_idx, extendedBlock);

        for (int line_idx = prevIdentifiedBlock.sourceLine + shift; line_idx < this.lines.size(); line_idx++) {
            String line = this.lines.get(line_idx).trim();
            if (!(line.isEmpty()) && extendedBlock.sourceText.startsWith(line)) {
                newParagraphAnchorLineIdx = line_idx;
                extendedBlock.sourceLine = line_idx - shift + 1;
                break;
            }

        }
        return newParagraphAnchorLineIdx;
    }

    /**
     * When paragraph is inside a list and escaped by +, sourceLine value is incorrect
     * Not quite elegant solution above
     */
    private ExtendedBlock getPrevIdentifiedBlock(int all_block_idx, ExtendedBlock extendedBlock) {
        for (int identified_block_idx = all_block_idx - 1; identified_block_idx >= 0; identified_block_idx--) {
            ExtendedBlock prevIdentifiedBlock = allBlocks.get(identified_block_idx);
            if (prevIdentifiedBlock.isIdentified) {
                return prevIdentifiedBlock;
            }
        }
        return extendedBlock;
    }

    private void addNestedId(int all_block_idx, ExtendedBlock extendedBlock) {

        if (!(extendedBlock.sourceText.equals(""))) {
            String beginText = extendedBlock.sourceText.split("\\r?\\n")[0];
            ExtendedBlock parentBlock = getParentBlock(extendedBlock);
            ExtendedBlock prevIdentifiedBlock = getPrevIdentifiedBlock(all_block_idx, extendedBlock);

            for (int line_idx = prevIdentifiedBlock.sourceLine + shift; line_idx < this.lines.size(); line_idx++) {
                String line = this.lines.get(line_idx).trim();

                if (extendedBlock.context.contains("list_item")) {

                    if (extendedBlock.sourceText.length() >= 3) {
                        if (extendedBlock.marker != null) { // обычный список
                            String marker = normalizeMarker(extendedBlock.marker);
                            Pattern SimpleListRx;
                            if (marker.equals(extendedBlock.marker)) {
                                SimpleListRx = Pattern.compile(
                                        String.format("^[ \\t]*(%s)[ \\t]+(%s)$",
                                                Pattern.quote(marker), Pattern.quote(beginText)));
                            } else {
                                SimpleListRx = Pattern.compile(
                                        String.format("^[ \\t]*\\d*(%s)[ \\t]+(%s)$",
                                                Pattern.quote(marker), Pattern.quote(beginText)));
                            }

                            Matcher m = SimpleListRx.matcher(line.trim());
                            if (m.matches()) {
                                if (parentBlock.style != null && parentBlock.style.equals("bibliography")) {
                                    this.lines.set(line_idx, String.format("%s [[[%s]]] %s",
                                            marker, extendedBlock.id, extendedBlock.sourceText));
                                } else {
                                    this.lines.set(line_idx, String.format("%s [[%s]]%s",
                                            marker, extendedBlock.id, beginText));
                                }
                                extendedBlock.sourceLine = line_idx - shift + 1;
                                extendedBlock.isIdentified = true;

                                break;
                            }
                        } else if (extendedBlock.term != null && extendedBlock.description != null) // список определений
                        {
                            Pattern DescriptionListRx = Pattern.compile(
                                    String.format("^(?!//)[ \\t]*(%s?)(:{2,4}|;;)(?:[ \\t]+(%s))?$",
                                            Pattern.quote(extendedBlock.term),
                                            Pattern.quote(extendedBlock.description.split("\\r?\\n")[0])));

                            Matcher m = DescriptionListRx.matcher(line.trim());
                            if (m.matches()) {
                                this.lines.set(line_idx, String.format("[[%s]]%s", extendedBlock.id, line));
                                extendedBlock.sourceLine = line_idx - shift + 1;
                                extendedBlock.isIdentified = true;

                                break;
                            }
                        }
                    }
                }
            }
        }

    }

    private ExtendedBlock getParentBlock(ExtendedBlock extendedBlock) {
        ExtendedBlock parentBlock = new ExtendedBlock();
        for (ExtendedBlock block : allBlocks) {
            if (extendedBlock.parentId.equals(block.id)) {
                parentBlock = block;
                break;
            }
        }
        return parentBlock;
    }

    private String normalizeMarker(String marker) {
        String normalizedMarker = marker;
        Pattern markerRx = Pattern.compile("^\\d+\\t*(.*)$");

        Matcher m = markerRx.matcher(marker.trim());
        if (m.matches()) {
            normalizedMarker = m.group(1);
        }

        return normalizedMarker;
    }
}
