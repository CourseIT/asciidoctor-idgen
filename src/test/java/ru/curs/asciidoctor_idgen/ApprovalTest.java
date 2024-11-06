package ru.curs.asciidoctor_idgen;

import com.spun.util.io.FileUtils;
import org.approvaltests.Approvals;
import org.approvaltests.namer.NamerFactory;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class ApprovalTest {

    @ParameterizedTest
    @CsvSource({
            "existing-id-1, adoc",
            "list-1, adoc",
            "list-1, html",
            "list-1, log",
            "error-1, log",
            "list-2, adoc",
            "section-1, adoc",
            "table-1, adoc",
    })
    void enrich(String adocCase, String type) throws Exception {
        Path tmpdir = Files.createTempDirectory(Paths.get("target"), "approve");
        String adocFilePath = "src/test/data/%s.adoc".formatted(adocCase);
        String outFilePath = "%s/%s-out.adoc".formatted(tmpdir, adocCase);
        String htmlFilePath = "%s/%s-out.html".formatted(tmpdir, adocCase);
        String jsonFilePath = "%s/%s-json.adoc".formatted(tmpdir, adocCase);
        var log = Main.enrich(adocFilePath, outFilePath, jsonFilePath, true);
        File adocFile = new File(outFilePath);
        if ("adoc".equals(type)) {
            NamerFactory.setAdditionalInformation(adocCase);
            Approvals.verify(
                    FileUtils.readFile(adocFile)
                            .replaceAll("[a-z0-9]{6}]]", "FFFFFF]]")
            );
        } else if ("html".equals(type)) {
            NamerFactory.setAdditionalInformation(adocCase + ".html");
            Approvals.verify(
                    Jsoup.parse(new File(htmlFilePath)).selectXpath("//div[@id='content']").get(0).toString()
                            .replaceAll("_[a-z0-9]{6}\"", "_FFFFFF\"")
            );
        } else if ("log".equals(type)) {
            NamerFactory.setAdditionalInformation(adocCase + ".log");
            Approvals.verify(log.toString().replaceAll("approve[0-9]+", "approve"));
        } else throw new Exception("Can't recoginze test type");
    }
}