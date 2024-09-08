package ru.curs.asciidoctor_idgen;

import java.util.ArrayList;

public class OutputLog {
    private final ArrayList<ArrayList<String>> log = new ArrayList<>();

    public ArrayList<ArrayList<String>> getLog() {
        return log;
    }

    public void add(String severity, String message) {
        ArrayList<String> record = new ArrayList<>();
        record.add(severity);
        record.add(message);
        log.add(record);
    }

    public void add(OutputLog outputLog) {
        outputLog.log.forEach(record ->
                this.add(record.get(0), record.get(1))
        );
    }

    public String toString() {
        return String.join("\n",
                log.stream().map(record ->
                        String.join(",", record)
                ).toArray(String[]::new)
        );
    }

}
