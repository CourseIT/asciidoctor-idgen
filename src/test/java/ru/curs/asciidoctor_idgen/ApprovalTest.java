package ru.curs.asciidoctor_idgen;

import com.spun.util.io.FileUtils;
import org.approvaltests.Approvals;
import org.approvaltests.namer.NamerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

class ApprovalTest {

    @ParameterizedTest
    @CsvSource({"list-1",})
    void extender(String adocCase) throws IOException {
        Path tmpdir = Files.createTempDirectory(Paths.get("target"), "approve");
        String adocFilePath = "src/test/data/%s.adoc".formatted(adocCase);
        Lurker lurker = new Lurker(adocFilePath);
        ArrayList<ExtendedBlock> allBlocks = lurker.lurk();
        String outFilePath = "%s/%s-out.adoc".formatted(tmpdir, adocCase);
        String jsonFilePath = "%s/%s-json.adoc".formatted(tmpdir, adocCase);
        Extender extender = new Extender(adocFilePath, outFilePath, jsonFilePath, allBlocks);
        extender.extend();
        File adocFile = new File(outFilePath);
        NamerFactory.setAdditionalInformation(adocCase);
        Approvals.verify(
                FileUtils.readFile(adocFile)
                        .replaceAll("[a-z0-9]{6}]]", "FFFFFF]]")
        );
    }
}