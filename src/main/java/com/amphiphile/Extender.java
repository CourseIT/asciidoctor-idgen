package com.amphiphile;

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
    private String path;
    private String outPath;
    private String jsonFilePath;
    private Boolean identifyListItems;
    private Boolean identifyBiblioItems;
    private Boolean identifyCells;
    private ArrayList<ExtendedBlock> allBlocks;
    private int shift = 0; //количество новых линий, которое было вставлено в документ, относительно исходного

    Extender(String path, String outPath, String jsonFilePath, ArrayList<ExtendedBlock> allBlocks) {
        this.path = path;
        this.outPath = outPath;
        this.jsonFilePath = jsonFilePath;
        this.identifyListItems = false;
        this.identifyBiblioItems = false;
        this.identifyCells = false;

        this.allBlocks = allBlocks;
    }

    void extend(Boolean identifyListItems, Boolean identifyBiblioItems, Boolean identifyCells) throws IOException {

        this.identifyListItems = identifyListItems;
        this.identifyBiblioItems = identifyBiblioItems;
        this.identifyCells = identifyCells;

        int lastDot = path.lastIndexOf('.');
        String copyPath = path.substring(0, lastDot) + "_copy" + path.substring(lastDot);

        Files.copy((new File(path)).toPath(), (new File(copyPath)).toPath(), StandardCopyOption.REPLACE_EXISTING);

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(new File(copyPath)), StandardCharsets.UTF_8));

        List<String> lines = new ArrayList<>();
        String nextLine;

        while ((nextLine = bufferedReader.readLine()) != null) {
            lines.add(nextLine);
        }
        bufferedReader.close();

        for (ExtendedBlock extendedBlock : allBlocks) {
            if (!extendedBlock.isIdentified && !extendedBlock.isEmbeddedDoc) {
                if (extendedBlock.context.equals("paragraph")
//                        || extendedBlock.context.endsWith("list") && !extendedBlock.style.equals("bibliography")
                ) {
                    try {
                        lines.add(extendedBlock.sourceLine + shift - 1, "[[" + extendedBlock.id + "]]");
                        shift += 1;
                        extendedBlock.isIdentified = true;
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println(String.format("Source line: %s, Shift %s, Error message: %s", extendedBlock.sourceLine, shift, e.getMessage()));
                    }
                } else if (extendedBlock.context.equals("image")) {
                    lines.add(extendedBlock.sourceLine + shift - 1, "[[" + extendedBlock.id + "]]");
                    shift += 1;
                    extendedBlock.isIdentified = true;
                } else if (extendedBlock.context.equals("table")) {
                    lines.add(extendedBlock.sourceLine + shift - 1 - 1, "[[" + extendedBlock.id + "]]");
                    shift += 1;
                    extendedBlock.isIdentified = true;
                } else if (extendedBlock.context.equals("cell")) {
                    addNestedId(lines, extendedBlock);

                } else if (extendedBlock.context.contains("list_item")) {

                    addNestedId(lines, extendedBlock);
                }
            }
        }

        if (jsonFilePath != null) {

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(jsonFilePath)), StandardCharsets.UTF_8))) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.disableHtmlEscaping();
//                gsonBuilder.setPrettyPrinting();
                Gson gson = gsonBuilder.create();
                gson.toJson(allBlocks, writer);

            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }

        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(new File(outPath)), StandardCharsets.UTF_8));

        for (String line : lines) {
            out.append(line).append("\r\n");
        }

        out.flush();
        out.close();

        Files.delete((new File(copyPath).toPath()));
    }

    private void addNestedId(List<String> lines, ExtendedBlock extendedBlock) {

        if (!(extendedBlock.sourceText.equals(""))) {
            String beginText = extendedBlock.sourceText.split("\\r?\\n")[0];
            ExtendedBlock parentBlock = getParentBlock(extendedBlock);
//            int startIdx = DefaultValueHandler.getValueOrDefault(parentBlock.sourceLine, 0);
//
//            if (extendedBlock.marker != null && extendedBlock.marker.length() >= 2) {
//                startIdx = 1;// для вложенных листов неправильно работает sourceline
//            }
            int startIdx = 1;
            for (int line_idx = startIdx + shift - 1; line_idx < lines.size(); line_idx++) {
                String line = lines.get(line_idx).trim();

                if (extendedBlock.context.contains("list_item")) {

                    if (identifyListItems ||
                            identifyBiblioItems && parentBlock.style.equals("bibliography")) {
                        if (extendedBlock.sourceText.length() >= 7) {
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
                                        lines.set(line_idx, String.format("%s [[[%s]]] %s",
                                                marker, extendedBlock.id, extendedBlock.sourceText));
                                    } else {
                                        lines.set(line_idx, String.format("%s [[%s]]%s",
                                                marker, extendedBlock.id, beginText));
                                    }
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
                                    lines.set(line_idx, String.format("[[%s]]%s", extendedBlock.id, line));
                                    extendedBlock.isIdentified = true;

                                    break;
                                }
                            }
                        }
                    }
                } else if (extendedBlock.context.equals("cell") && identifyCells)
                    //FIXME: уточнить поиск на дубликаты, а также на простые строки (например, "1")
                    if (line.contains(beginText)) {
                        lines.set(line_idx, line.replaceAll(beginText, String.format("[[%s]]%s", extendedBlock.id, beginText)));
                        extendedBlock.isIdentified = true;
                        break;
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
