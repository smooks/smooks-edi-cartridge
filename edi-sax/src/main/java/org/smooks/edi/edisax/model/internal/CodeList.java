package org.smooks.edi.edisax.model.internal;

import java.util.ArrayList;
import java.util.List;

public class CodeList {
    private String documentation;
    private List<String> codes = new ArrayList<>();

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public List<String> getCodes() {
        return codes;
    }

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }
}
