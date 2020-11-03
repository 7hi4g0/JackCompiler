package com.tandrade.jack.parser.syntax;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tandrade.jack.parser.token.Token;
import com.tandrade.jack.parser.token.TokenType;
import com.tandrade.jack.parser.token.Tokenizer;

import static java.util.Map.entry;

public class CompilationEngine {

    private static Map<String, String> OP_MAP = Map.ofEntries(
        entry("+", "add"),
        entry("-", "sub"),
        entry("*", "call Math.multiply 2"),
        entry("/", "call Math.divide 2"),
        entry("&", "and"),
        entry("|", "or"),
        entry("<", "lt"),
        entry(">", "gt"),
        entry("=", "eq")
    );
    private static Map<String, String> UNARY_OP_MAP = Map.ofEntries(
        entry("-", "neg"),
        entry("~", "not")
    );

    private Tokenizer tokenizer;
    private Map<String, VarInfo> classVariableTable;
    private Map<String, VarInfo> localVariableTable;
    private Map<VarScope, Integer> variableCount;
    private Map<String, Integer> localLabelCount;
    private Token lastToken;
    private String currentClassName;
    private List<String> output;

    public CompilationEngine(File input) throws IOException {
        this.tokenizer = new Tokenizer(input);
        this.output = new ArrayList<>();
        this.classVariableTable = new HashMap<>();
        this.localVariableTable = null;
        this.localLabelCount = null;
        this.lastToken = null;
        this.currentClassName = null;
        this.variableCount = new EnumMap<>(Map.of(VarScope.FIELD, 0, VarScope.STATIC, 0, VarScope.ARGUMENT, 0, VarScope.LOCAL, 0));
    }

    public void compileClass() {
        consumeToken(TokenType.KEYWORD, "class");
        consumeToken(TokenType.IDENTIFIER);

        this.currentClassName = lastToken.getValue();

        consumeToken(TokenType.SYMBOL, "{");

        while (compileClassVarDec()) {
        }
        while (compileSubroutine()) {
        }

        consumeToken(TokenType.SYMBOL, "}");
    }

    public boolean compileClassVarDec() {
        if (!testToken(TokenType.KEYWORD)) {
            return false;
        }
        if (!testToken(TokenType.KEYWORD, "static") &&
            !testToken(TokenType.KEYWORD, "field")) {
            return false;
        }

        consumeToken();
        VarScope scope = VarScope.valueOf(lastToken.getValue().toUpperCase());

        compileType();
        String type = lastToken.getValue();

        consumeToken(TokenType.IDENTIFIER);
        String variableName = lastToken.getValue();

        addClassVariable(scope, type, variableName);

        while (testToken(TokenType.SYMBOL, ",")) {
            consumeToken();
            consumeToken(TokenType.IDENTIFIER);
            variableName = lastToken.getValue();
    
            addClassVariable(scope, type, variableName);
        }

        consumeToken(TokenType.SYMBOL, ";");
        
        return true;
    }

    public void addClassVariable(VarScope scope, String type, String name) {
        int index = variableCount.get(scope);
        variableCount.put(scope, index + 1);

        classVariableTable.put(name, new VarInfo(type, scope, index));
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

        consumeToken();
        String subroutineType = lastToken.getValue();

        localLabelCount = new HashMap<>();
        localVariableTable = new HashMap<>();
        variableCount.put(VarScope.ARGUMENT, 0);
        variableCount.put(VarScope.LOCAL, 0);

        if (!subroutineType.equals("function")) {
            addLocalVariable(VarScope.ARGUMENT, currentClassName, "this");
        }

        // TODO: Do I need to use it?
        compileReturnType();

        consumeToken(TokenType.IDENTIFIER);
        String subroutineName = currentClassName + "." + lastToken.getValue();

        consumeToken(TokenType.SYMBOL, "(");
        compileParameterList();
        consumeToken(TokenType.SYMBOL, ")");

        compileSubroutineBody(subroutineType, subroutineName);

        localLabelCount = null;
        localVariableTable = null;

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
        if (!testToken(TokenType.SYMBOL, ")")) {

            compileType();
            String type = lastToken.getValue();

            consumeToken(TokenType.IDENTIFIER);
            String parameterName = lastToken.getValue();

            addLocalVariable(VarScope.ARGUMENT, type, parameterName);

            while (!testToken(TokenType.SYMBOL, ")")) {
                consumeToken(TokenType.SYMBOL, ",");
                compileType();
                type = lastToken.getValue();
    
                consumeToken(TokenType.IDENTIFIER);
                parameterName = lastToken.getValue();

                addLocalVariable(VarScope.ARGUMENT, type, parameterName);
            }
        }
    }

    public void addLocalVariable(VarScope scope, String type, String name) {
        int index = variableCount.get(scope);
        variableCount.put(scope, index + 1);

        localVariableTable.put(name, new VarInfo(type, scope, index));
    }

    public VarInfo getVarInfo(String variableName) {
        VarInfo info = classVariableTable.get(variableName);

        if (localVariableTable.containsKey(variableName)) {
            info = localVariableTable.get(variableName);
        }

        return info;
    }

    public void compileSubroutineBody(String subroutineType, String subroutineName) {
        consumeToken(TokenType.SYMBOL, "{");

        while (compileVarDec()) {}

        output.add(subroutineType + " " + subroutineName + " " + variableCount.get(VarScope.LOCAL));

        compileStatements();

        consumeToken(TokenType.SYMBOL, "}");
    }

    public boolean compileVarDec() {
        if (!testToken(TokenType.KEYWORD, "var")) {
            return false;
        }
        
        consumeToken();

        compileType();
        String type = lastToken.getValue();

        consumeToken(TokenType.IDENTIFIER);
        String variableName = lastToken.getValue();

        addLocalVariable(VarScope.LOCAL, type, variableName);

        while (!testToken(TokenType.SYMBOL, ";")) {
            consumeToken(TokenType.SYMBOL, ",");
            consumeToken(TokenType.IDENTIFIER);
            variableName = lastToken.getValue();
    
            addLocalVariable(VarScope.LOCAL, type, variableName);
        }

        consumeToken(TokenType.SYMBOL, ";");

        return true;
    }

    public void compileStatements() {
        while (compileStatement()) {}
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
        boolean arrayWrite = false;

        consumeToken(TokenType.KEYWORD, "let");
        consumeToken(TokenType.IDENTIFIER);
        String destVar = lastToken.getValue();

        VarInfo info = getVarInfo(destVar);

        if (info == null) {
            throw new IllegalArgumentException("Unkown identifier: " + destVar);
        }

        if (testToken(TokenType.SYMBOL, "[")) {
            arrayWrite = true;

            output.add("push " + info.getScope().toString().toLowerCase() + " " + info.getIndex());

            consumeToken();
            compileExpression();
            consumeToken(TokenType.SYMBOL, "]");

            output.add("add");
        }

        consumeToken(TokenType.SYMBOL, "=");

        compileExpression();

        if (arrayWrite) {
            output.add("pop temp 0");
            output.add("pop pointer 1");
            output.add("push temp 0");
            output.add("pop that 0");
        } else {
            output.add("pop " + info.getScope().toString().toLowerCase() + " " + info.getIndex());
        }

        consumeToken(TokenType.SYMBOL, ";");
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
        consumeToken(TokenType.KEYWORD, "while");

        int count = 0;
        if (localLabelCount.containsKey("while")) {
            count = localLabelCount.get("while");
        }
        localLabelCount.put("while", count + 1);

        output.add("label WHILE" + count);

        consumeToken(TokenType.SYMBOL, "(");
        compileExpression();
        consumeToken(TokenType.SYMBOL, ")");

        output.add("not");
        output.add("if-goto WHILE_END" + count);

        consumeToken(TokenType.SYMBOL, "{");

        compileStatements();

        consumeToken(TokenType.SYMBOL, "}");

        output.add("goto WHILE" + count);
        output.add("label WHILE_END" + count);
    }

    public void compileDoStatement() {
        consumeToken(TokenType.KEYWORD, "do");

        consumeToken(TokenType.IDENTIFIER);
        String variableOrClassOrSubroutine = lastToken.getValue();
        String className = currentClassName;
        String subroutineName = variableOrClassOrSubroutine;

        if (testToken(TokenType.SYMBOL, ".")) {
            consumeToken();
            consumeToken(TokenType.IDENTIFIER);
            subroutineName = lastToken.getValue();

            className = variableOrClassOrSubroutine;

            VarInfo info = getVarInfo(variableOrClassOrSubroutine);

            if (info != null) {
                className = info.getType();
            }
        }

        consumeToken(TokenType.SYMBOL, "(");
        compileExpressionList(className + "." + subroutineName);
        consumeToken(TokenType.SYMBOL, ")");

        output.add("pop temp 0");

        consumeToken(TokenType.SYMBOL, ";");
    }

    public void compileReturnStatement() {
        consumeToken(TokenType.KEYWORD, "return");

        if (!testToken(TokenType.SYMBOL, ";")) {
            compileExpression();
        } else {
            output.add("push constant 0");
        }

        output.add("return");

        consumeToken(TokenType.SYMBOL, ";");
    }

    public void compileTerm() {
        Token token = tokenizer.getCurrentToken();

        switch (token.getTokenType()) {
            case INT_CONST:
                consumeToken();
                output.add("push constant " + lastToken.getValue());
                break;
            case STR_CONST:
                consumeToken();
                String stringConstant = lastToken.getValue();
                output.add("push constant " + stringConstant.length());
                output.add("call String.new 1");

                stringConstant.chars().forEach(code -> {
                    output.add("push constant " + code);
                    output.add("call String.appendChar 2");
                });
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
                        String op = UNARY_OP_MAP.get(lastToken.getValue());
                        compileTerm();
                        output.add(op);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected token: " + token);
                }
            }
                break;
            case IDENTIFIER: {
                consumeToken();
                String variableOrClassOrSubroutine = lastToken.getValue();
                String className = currentClassName;
                String subroutineName = variableOrClassOrSubroutine;
                boolean treated = false;

                if (testToken(TokenType.SYMBOL)){
                    switch (tokenizer.getCurrentToken().getValue()) {
                        case "[":{
                            VarInfo info = getVarInfo(variableOrClassOrSubroutine);
                            output.add("push " + info.getScope().toString().toLowerCase() + " " + info.getIndex());

                            consumeToken();
                            compileExpression();
                            consumeToken(TokenType.SYMBOL, "]");

                            output.add("add");
                            output.add("pop pointer 1");
                            output.add("push that 0");

                            treated = true;
                        }
                        break;
                        case ".":{
                            consumeToken();
        
                            consumeToken(TokenType.IDENTIFIER);
                            subroutineName = lastToken.getValue();

                            className = variableOrClassOrSubroutine;

                            VarInfo info = getVarInfo(variableOrClassOrSubroutine);

                            if (info != null) {
                                className = info.getType();
                            }
                        }
                        case "(":{
                            consumeToken(TokenType.SYMBOL, "(");
                            compileExpressionList(className + "." + subroutineName);
                            consumeToken(TokenType.SYMBOL, ")");
                            treated = true;
                        }
                        break;
                    }
                }
                
                if (!treated) {
                    VarInfo info = getVarInfo(variableOrClassOrSubroutine);

                    output.add("push " + info.getScope().toString().toLowerCase() + " " + info.getIndex());
                }
            }
                break;
        }
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
        compileTerm();

        while (compileOp()) {
            String op = OP_MAP.get(lastToken.getValue());
            compileTerm();

            output.add(op);
        }
    }

    public void compileExpressionList(String subroutineName) {
        int parameterCount = 0;

        if (!testToken(TokenType.SYMBOL, ")")) {
            compileExpression();
            parameterCount++;

            while (testToken(TokenType.SYMBOL, ",")) {
                consumeToken();
                compileExpression();
                parameterCount++;
            }
        }

        output.add("call " + subroutineName + " " + parameterCount);
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

        lastToken = token;
    }

    private void consumeToken(TokenType type) {
        Token token = tokenizer.advance();
        if (token.getTokenType() != type) {
            throw new IllegalArgumentException("Unexpected token: " + token + "\nExpected token of type <" + type.getElement() + ">");
        }

        lastToken = token;
    }

    private void consumeToken(TokenType type, String value) {
        Token token = tokenizer.advance();
        if (token.getTokenType() != type) {
            throw new IllegalArgumentException("Unexpected token: " + token + "\nExpected token of type <" + type.getElement() + ">");
        }
        if (!token.getValue().equals(value)) {
            throw new IllegalArgumentException("Unexpected token: " + token + "\nExpected token " + value + ">");
        }

        lastToken = token;
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

            Path outputFilename = file.toPath().resolveSibling(filename.substring(0, extIndex) + ".vm");

            Files.write(outputFilename, engine.output, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}