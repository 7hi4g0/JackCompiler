package com.tandrade.jack.parser.token;

public enum TokenType {
    KEYWORD("keyword"),
    SYMBOL("symbol"),
    IDENTIFIER("identifier"),
    INT_CONST("integerConstant"),
    STR_CONST("stringConstant");

    private String element;

    private TokenType(String element) {
        this.element = element;
    }

    public String getElement() {
        return element;
    }
}
