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
                compileIfStatement();
                break;
            case "while":
                compileWhileStatement();
                break;
            case "do":
                compileDoStatement();
                break;
            case "return":
                compileReturnStatement();
                break;
            default:
                return false;
        }

        return true;
    }

    public void compileLetStatement() {
        output.add("<letStatement>");

        consumeToken(TokenType.KEYWORD, "let");
        consumeToken(TokenType.IDENTIFIER);

        if (testToken(TokenType.SYMBOL, "[")) {
            consumeToken();
            compileExpression();
            consumeToken(TokenType.SYMBOL, "]");
        }

        consumeToken(TokenType.SYMBOL, "=");

        compileExpression();

        consumeToken(TokenType.SYMBOL, ";");

        output.add("</letStatement>");
    }

    public void compileIfStatement() {
        output.add("<ifStatement>");

        consumeToken(TokenType.KEYWORD, "if");

        consumeToken(TokenType.SYMBOL, "(");
        compileExpression();
        consumeToken(TokenType.SYMBOL, ")");

        consumeToken(TokenType.SYMBOL, "{");

        compileStatements();

        consumeToken(TokenType.SYMBOL, "}");

        if (testToken(TokenType.KEYWORD, "else")) {
            consumeToken();

            consumeToken(TokenType.SYMBOL, "{");
    
            compileStatements();
    
            consumeToken(TokenType.SYMBOL, "}");
        }

        output.add("</ifStatement>");
    }

    public void compileWhileStatement() {
        output.add("<whileStatement>");

        consumeToken(TokenType.KEYWORD, "while");

        consumeToken(TokenType.SYMBOL, "(");
        compileExpression();
        consumeToken(TokenType.SYMBOL, ")");

        consumeToken(TokenType.SYMBOL, "{");

        compileStatements();

        consumeToken(TokenType.SYMBOL, "}");

        output.add("</whileStatement>");
    }

    public void compileDoStatement() {
        output.add("<doStatement>");

        consumeToken(TokenType.KEYWORD, "do");

        consumeToken(TokenType.IDENTIFIER);

        if (testToken(TokenType.SYMBOL, ".")) {
            consumeToken();
            consumeToken(TokenType.IDENTIFIER);
        }

        consumeToken(TokenType.SYMBOL, "(");
        compileExpressionList();
        consumeToken(TokenType.SYMBOL, ")");

        consumeToken(TokenType.SYMBOL, ";");

        output.add("</doStatement>");
    }

    public void compileReturnStatement() {
        output.add("<returnStatement>");

        consumeToken(TokenType.KEYWORD, "return");

        if (!testToken(TokenType.SYMBOL, ";")) {
            compileExpression();
        }

        consumeToken(TokenType.SYMBOL, ";");

        output.add("</returnStatement>");
    }

    public void compileTerm() {
        output.add("<term>");

        Token token = tokenizer.getCurrentToken();

        switch (token.getTokenType()) {
            case INT_CONST:
            case STR_CONST:
                consumeToken();
                break;
            case KEYWORD: {
                switch (token.getValue()) {
                    case "true":
                    case "false":
                    case "null":
                    case "this":
                        consumeToken();
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected token: " + token);
                }
            }
                break;
            case SYMBOL:{
                switch (token.getValue()) {
                    case "(":
                        consumeToken();
                        compileExpression();
                        consumeToken(TokenType.SYMBOL, ")");
                        break;
                    case "-":
                    case "~":
                        consumeToken();
                        compileTerm();
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected token: " + token);
                }
            }
                break;
            case IDENTIFIER: {
                consumeToken();

                if (testToken(TokenType.SYMBOL)){
                    switch (tokenizer.getCurrentToken().getValue()) {
                        case "[":{
                            consumeToken();
                            compileExpression();
                            consumeToken(TokenType.SYMBOL, "]");
                        }
                        break;
                        case ".":{
                            consumeToken();
        
                            consumeToken(TokenType.IDENTIFIER);
                        }
                        case "(":{
                            consumeToken(TokenType.SYMBOL, "(");
                            compileExpressionList();
                            consumeToken(TokenType.SYMBOL, ")");
                        }
                        break;
                    }
                }
            }
                break;
        }

        output.add("</term>");
    }

    public boolean compileOp() {
        if (!testToken(TokenType.SYMBOL)) {
            return false;
        }

        switch (tokenizer.getCurrentToken().getValue()) {
            case "+":
            case "-":
            case "*":
            case "/":
            case "&":
            case "|":
            case "<":
            case ">":
            case "=":
                consumeToken();
                return true;
        }

        return false;
    }

    public void compileExpression() {
        output.add("<expression>");

        compileTerm();

        while (compileOp()) {
            compileTerm();
        }

        output.add("</expression>");
    }

    public void compileExpressionList() {
        output.add("<expressionList>");

        if (!testToken(TokenType.SYMBOL, ")")) {
            compileExpression();

            while (testToken(TokenType.SYMBOL, ",")) {
                consumeToken();
                compileExpression();
            }
        }

        output.add("</expressionList>");
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