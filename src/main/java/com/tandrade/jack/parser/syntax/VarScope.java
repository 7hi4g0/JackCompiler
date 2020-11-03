package com.tandrade.jack.parser.syntax;

public enum VarScope {
    
    FIELD("this"),
    STATIC("static"),
    ARGUMENT("argument"),
    LOCAL("local");

    private String segment;

    private VarScope(String segment) {
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }
}
