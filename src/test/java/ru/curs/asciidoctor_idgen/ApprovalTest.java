package ru.curs.asciidoctor_idgen;

import com.spun.util.io.FileUtils;
import org.approvaltests.Approvals;
import org.approvaltests.namer.NamerFactory;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class ApprovalTest {

    @ParameterizedTest
    @CsvSource({
            "list-1",
            "error-1",
            "list-2",
    })
    void enrich(String adocCase) throws IOException {
        Path tmpdir = Files.createTempDirectory(Paths.get("target"), "approve");
        String adocFilePath = "src/test/data/%s.adoc".formatted(adocCase);
        Lurker lurker = new Lurker(adocFilePath);
        String outFilePath = "%s/%s-out.adoc".formatted(tmpdir, adocCase);
        String htmlFilePath = "%s/%s-out.html".formatted(tmpdir, adocCase);
        String jsonFilePath = "%s/%s-json.adoc".formatted(tmpdir, adocCase);
        var log = Main.enrich(adocFilePath, outFilePath, jsonFilePath, true);
        File adocFile = new File(outFilePath);
        NamerFactory.setAdditionalInformation(adocCase);
        Approvals.verify(
                FileUtils.readFile(adocFile)
                        .replaceAll("[a-z0-9]{6}]]", "FFFFFF]]")
        );
        NamerFactory.setAdditionalInformation(adocCase + ".html");
        Approvals.verify(
                Jsoup.parse(new File(htmlFilePath)).selectXpath("//div[@id='content']").get(0).toString()
                        .replaceAll("_[a-z0-9]{6}\"", "_FFFFFF\"")
        );
        NamerFactory.setAdditionalInformation(adocCase + ".log");
        Approvals.verify(log.toString().replaceAll("approve[0-9]+", "approve"));

    }
}