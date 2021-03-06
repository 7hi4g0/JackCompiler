package com.tandrade.jack.parser.token;

public class Token {
    private TokenType type;
    private String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getTokenType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "<" + type.getElement() + "> " + value + " </" + type.getElement() + ">";
    }
}
