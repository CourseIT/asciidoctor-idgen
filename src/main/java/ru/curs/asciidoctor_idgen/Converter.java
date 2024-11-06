package ru.curs.asciidoctor_idgen;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;


class Converter {
    private String adocFilePath;

    public static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }

    Converter(String adocFilePath) {
        this.adocFilePath = adocFilePath;
    }

    void nodeDescendant(ArrayList<Node> nodes, Node nodeToProcess) {
        nodes.add(nodeToProcess);
        nodeToProcess.childNodes().forEach(childNode -> nodeDescendant(nodes, childNode));
    }

    OutputLog convert() {
        final OutputLog outputLog = new OutputLog();
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.registerLogHandler(logRecord -> {
                var fullLogMessage =
                        String.format("%s: %s", logRecord.getSeverity(), logRecord.getMessage());
                System.out.println(fullLogMessage);
                outputLog.add(logRecord.getSeverity().name(), logRecord.getMessage());
            });

//            val options = Options.builder().backend("html5")
//                    .safe(SafeMode.UNSAFE).sourcemap(true).toFile(false).standalone(true)
//                    .attributes(attributes)
//                    .apply { if (baseDir != null) baseDir(baseDir) }
//            .build()

            System.out.println(this.adocFilePath);
            System.out.println((new File(this.adocFilePath)).getParentFile());
            var baseDir = (new File("./" + this.adocFilePath)).getParentFile();

            var options = Options.builder()
                    .safe(SafeMode.UNSAFE)
                    .baseDir(baseDir)
                    .build();

            asciidoctor.convertFile(new File(this.adocFilePath), options);
        }
        var outputHtmlPath = String.format("%s.html", removeFileExtension(this.adocFilePath, false));
        Document jsoup;
        try {
            jsoup = Jsoup.parse(new File(outputHtmlPath));
        } catch (IOException e) {
            throw new RuntimeException("Output Html not found", e);
        }
        System.out.println("Converted to html successfuuly");
        outputLog.add("Info", "Converted to html successfuuly");
        jsoup.selectXpath("//li").forEach(listItem ->
                {
                    var descendant = new ArrayList<Node>();
                    nodeDescendant(descendant, listItem);
                    descendant.stream().filter(node -> node instanceof Element).forEach(child -> {
                                var childElement = (Element) child;
                                if (child.nodeName().equals("a") && !childElement.id().isBlank()) {
                                    String id = childElement.id();
                                    childElement.id(String.format("anchor_%s", id));
                                    listItem.id(id);
                                }
                            }
                    );
//                    var aTagsWithId = listItem.selectXpath(".//a[@id]");
//                    if (!aTagsWithId.isEmpty()) {
//                        var aTagWithId = aTagsWithId.get(0);
//                        String id = aTagWithId.id();
//                        aTagWithId.id(String.format("anchor_%s", id));
//                        listItem.id(id);
//                    }
                }

        );
        try (PrintWriter out = new PrintWriter(outputHtmlPath)) {
            out.print(jsoup);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Can't overwrite resulting html", e);
        }
        return outputLog;
    }
}
