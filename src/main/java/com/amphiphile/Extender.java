package com.amphiphile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

class Extender {
    private int shift = 0; //количество новых линий, которое было вставлено в документ, относительно исходного

    void extend(String path, ArrayList<ExtendedBlock> allTheBlocks) throws IOException {

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


        for (int idx = 0; idx < allTheBlocks.size(); idx++)

        {
            ExtendedBlock extendedBlock = allTheBlocks.get(idx);

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
                } else if (extendedBlock.context.endsWith("list")) {

                    if (!extendedBlock.style.equals("bibliography")) {
                        lines.add(extendedBlock.sourceLine + shift - 1 - 1, "[[" + extendedBlock.id + "]]");
                        shift += 1;
                        extendedBlock.isIdentified = true;
                    }
                } else if (extendedBlock.context.contains("list_item")) {


                    for (int line_idx = idx; line_idx < lines.size(); line_idx++) {
                        String line = lines.get(line_idx);


                        if (line.startsWith(String.format("%s %s", extendedBlock.marker, extendedBlock.sourceText.split("\\r?\\n")[0]))
                                ) {
                            lines.set(line_idx, String.format("%s [[%s]]%s", extendedBlock.marker, extendedBlock.id, extendedBlock.sourceText));
                            break;
                        }
                    }


                    extendedBlock.isIdentified = true;
                }
            }
        }
        String newFilePath = path.substring(0, lastDot) + "_new" + path.substring(lastDot);
        FileWriter fw = new FileWriter(newFilePath);

        for (String line : lines) {
            fw.write(line + "\n");
        }

        fw.close();
        Files.delete((new File(copyPath).toPath()));
    }
}
