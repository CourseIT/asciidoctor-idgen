package ru.curs.asciidoctor_idgen;

public class EnrichParams {
    private String input;
    private String output;
    private String json;
    private Boolean ohtml;
    private Boolean sni;

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public String getJson() {
        return json;
    }

    public Boolean getOhtml() {
        return ohtml;
    }

    public Boolean getSectionNaturalIds() {
        return sni;
    }
}
