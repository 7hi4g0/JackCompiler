package com.tandrade.jack.parser.syntax;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tandrade.jack.parser.token.Token;
import com.tandrade.jack.parser.token.TokenType;
import com.tandrade.jack.parser.token.Tokenizer;

public class CompilationEngine {

    private Tokenizer tokenizer;
    private List<String> output;

    public CompilationEngine(File input) throws IOException {
        this.tokenizer = new Tokenizer(input);
        this.output = new ArrayList<>();
    }

    public void compileClass() {
        output.add("<class>");
        
        consumeToken(TokenType.KEYWORD, "class");
        consumeToken(TokenType.IDENTIFIER);
        consumeToken(TokenType.SYMBOL, "{");

        while (compileSubroutine()) {
        }

        consumeToken(TokenType.SYMBOL, "}");

        output.add("</class>");
    }

    public boolean compileSubroutine() {
        if (!testToken(TokenType.KEYWORD)) {
            return false;
        }
        if (!testToken(TokenType.KEYWORD, "function") &&
            !testToken(TokenType.KEYWORD, "constructor") &&
            !testToken(TokenType.KEYWORD, "method")) {
            return false;
        }

        output.add("<subroutineDec>");

        consumeToken();

        compileReturnType();

        consumeToken(TokenType.IDENTIFIER);

        consumeToken(TokenType.SYMBOL, "(");
        compileParameterList();
        consumeToken(TokenType.SYMBOL, ")");

        compileSubroutineBody();

        
        output.add("</subroutineDec>");

        return true;
    }

    public void compileReturnType() {
        if (testToken(TokenType.KEYWORD, "void")) {
            consumeToken();
            return;
        }

        compileType();
    }

    public void compileType() {
        if (testToken(TokenType.KEYWORD)) {
            if (testToken(TokenType.KEYWORD, "int") || testToken(TokenType.KEYWORD, "char") || testToken(TokenType.KEYWORD, "boolean")) {
                consumeToken();
                return;
            }
        }

        consumeToken(TokenType.IDENTIFIER);
    }

    public void compileParameterList() {
        output.add("<parameterList>");

        if (!testToken(TokenType.SYMBOL, ")")) {

            compileType();
            consumeToken(TokenType.IDENTIFIER);

            while (!testToken(TokenType.SYMBOL, ")")) {
                consumeToken(TokenType.SYMBOL, ",");
                compileType();
                consumeToken(TokenType.IDENTIFIER);
            }
        }

        output.add("</parameterList>");
    }

    public void compileSubroutineBody() {
        output.add("<subroutineBody>");
        
        consumeToken(TokenType.SYMBOL, "{");

        while (compileVarDec()) {}

        compileStatements();

        consumeToken(TokenType.SYMBOL, "}");

        output.add("</subroutineBody>");
    }

    public boolean compileVarDec() {
        if (!testToken(TokenType.KEYWORD, "var")) {
            return false;
        }
        
        output.add("<varDec>");

        consumeToken();

        compileType();
        consumeToken(TokenType.IDENTIFIER);

        while (!testToken(TokenType.SYMBOL, ";")) {
            consumeToken(TokenType.SYMBOL, ",");
            consumeToken(TokenType.IDENTIFIER);
        }

        consumeToken(TokenType.SYMBOL, ";");

        output.add("</varDec>");

        return true;
    }

    public void compileStatements() {
        output.add("<statements>");

        while (compileStatement()) {}

        output.add("</statements>");
    }

    public boolean compileStatement() {
        if (!testToken(TokenType.KEYWORD)) {
            return false;
        }

        switch (tokenizer.getCurrentToken().getValue()) {
            case "let":
                compileLetStatement();
                break;
            case "if":
                break;
            case "while":
                break;
            case "do":
                break;
            case "return":
                break;
            default:
                return false;
        }

        return true;
    }

    public void compileLetStatement() {
        output.add("<letStatement>");

        consumeToken(TokenType.KEYWORD, "let");

        output.add("</letStatement>");
    }

    private boolean testToken(TokenType type) {
        return tokenizer.getCurrentToken().getTokenType() == type;
    }

    private boolean testToken(TokenType type, String value) {
        Token token = tokenizer.getCurrentToken();
        return token.getTokenType() == type && token.getValue().equals(value);
    }

    private void consumeToken() {
        Token token = tokenizer.advance();

        output.add(token.toString());
    }

    private void consumeToken(TokenType type) {
        Token token = tokenizer.advance();
        if (token.getTokenType() != type) {
            throw new IllegalArgumentException("Unexpected token: " + token + "\nExpected token of type <" + type.getElement() + ">");
        }

        output.add(token.toString());
    }

    private void consumeToken(TokenType type, String value) {
        Token token = tokenizer.advance();
        if (token.getTokenType() != type) {
            throw new IllegalArgumentException("Unexpected token: " + token + "\nExpected token of type <" + type.getElement() + ">");
        }
        if (!token.getValue().equals(value)) {
            throw new IllegalArgumentException("Unexpected token: " + token + "\nExpected token " + value + ">");
        }

        output.add(token.toString());
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: CompilationEngine <file|directory>");
            return;
        }
        
        File inputFile = new File(args[0]);
        List<File> files;

        if (inputFile.isDirectory()) {
            files = Arrays.asList(inputFile.listFiles((d, f) -> f.endsWith(".jack")));
        } else {
            files = Collections.singletonList(inputFile);
        }

        for (File file : files) {
            CompilationEngine engine = new CompilationEngine(file);

            engine.compileClass();

            String filename = file.getName();
            int extIndex = filename.lastIndexOf('.');
            if (extIndex == -1) {
                extIndex = filename.length();
            }

            Path outputFilename = file.toPath().resolveSibling(filename.substring(0, extIndex) + "Syntax.xml");

            Files.write(outputFilename, engine.output, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}