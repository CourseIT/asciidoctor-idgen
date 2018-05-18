package com.amphiphile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

class Extender {
    void extend(String path, ArrayList<ExtendedBlock> allTheBlocks) {

        int lastDot = path.lastIndexOf('.');
        String copyPath = path.substring(0, lastDot) + "_copy" + path.substring(lastDot);

        try {
            Files.copy((new File(path)).toPath(), (new File(copyPath)).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }
}
