package com.tandrade.jack.parser.syntax;

public class VarInfo {

    private String type;
    private VarScope scope;
    private int index;
    
    public VarInfo(String type, VarScope scope, int index) {
        this.type = type;
        this.scope = scope;
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public VarScope getScope() {
        return scope;
    }
    
    public int getIndex() {
        return index;
    }
}
