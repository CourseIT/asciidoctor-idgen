package com.amphiphile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

class Extender {
    private String path;
    private String outPath;
    private Boolean identifyListItems;
    private Boolean identifyCells;
    private ArrayList<ExtendedBlock> allBlocks;
    private int shift = 0; //количество новых линий, которое было вставлено в документ, относительно исходного

    Extender(String path, String outPath, ArrayList<ExtendedBlock> allBlocks) {
        this.path = path;
        this.outPath = outPath;
        this.identifyListItems = false;
        this.identifyCells = false;

        this.allBlocks = allBlocks;
    }


    private static <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    void extend(Boolean identifyListItems, Boolean identifyCells) throws IOException {

        this.identifyListItems = identifyListItems;
        this.identifyCells = identifyCells;

        int lastDot = path.lastIndexOf('.');
        String copyPath = path.substring(0, lastDot) + "_copy" + path.substring(lastDot);


        Files.copy((new File(path)).toPath(), (new File(copyPath)).toPath(), StandardCopyOption.REPLACE_EXISTING);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(copyPath));
        List<String> lines = new ArrayList<>();
        String nextLine;

        while ((nextLine = bufferedReader.readLine()) != null) {
            lines.add(nextLine);
        }
        bufferedReader.close();


        for (ExtendedBlock extendedBlock : allBlocks) {
            if (!extendedBlock.isIdentified && !extendedBlock.isEmbeddedDoc) {
                System.out.println(extendedBlock.context);
                if (extendedBlock.context.equals("paragraph")) {
                    lines.add(extendedBlock.sourceLine + shift - 1, "[[" + extendedBlock.id + "]]");
                    shift += 1;
                    extendedBlock.isIdentified = true;
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
                } else if (extendedBlock.context.endsWith("list")) {

                    if (!extendedBlock.style.equals("bibliography")) {
                        lines.add(extendedBlock.sourceLine + shift - 1 - 1, "[[" + extendedBlock.id + "]]");
                        shift += 1;
                        extendedBlock.isIdentified = true;
                    }
                } else if (extendedBlock.context.contains("list_item")) {

                    addNestedId(lines, extendedBlock);
                }
            }
        }
        FileWriter fw = new FileWriter(outPath);

        for (String line : lines) {
            fw.write(line + "\n");
        }

        fw.close();
        Files.delete((new File(copyPath).toPath()));
    }


    private void addNestedId(List<String> lines, ExtendedBlock extendedBlock) {


        if (!(extendedBlock.sourceText.equals(""))) {
            String beginText = extendedBlock.sourceText.split("\\r?\\n")[0];
            ExtendedBlock parentBlock = getParentBlock(extendedBlock);
            int startIdx = getValueOrDefault(parentBlock.sourceLine, 0);
            for (int line_idx = startIdx + shift; line_idx < lines.size(); line_idx++) {
                String line = lines.get(line_idx);

                if (extendedBlock.context.contains("list_item") && identifyListItems) {
                    //FIXME: уточнить поиск на дубликаты, а также на простые строки (например, "1")
                    if (line.startsWith(String.format("%s %s", extendedBlock.marker, beginText))
                            ) {
                        if (parentBlock.style.equals("bibliography")) {
                            lines.set(line_idx, String.format("%s [[[%s]]]%s", extendedBlock.marker, extendedBlock.id, extendedBlock.sourceText));
                        }
                        {
                            lines.set(line_idx, String.format("%s [[%s]]%s", extendedBlock.marker, extendedBlock.id, extendedBlock.sourceText));
                        }
                        extendedBlock.isIdentified = true;

                        break;
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
}
