package com.tandrade.jack.parser.token;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tokenizer {
    private static final String keywordRegex = "(class|constructor|function|method|field|static|var|int|char|boolean|void|true|false|null|this|let|do|if|else|while|return)\\b";
    private static final String symbolRegex = "([{}()\\[\\].,;+\\-*/&|<>=~])";
    private static final String constantRegex = "(\\d{1,4}|[12]\\d{4}|3[01]\\d{3}|32[0-6]\\d{2}|327[0-5]\\d|3276[0-7])";
    private static final String stringRegex = "\"([^\"\n\r]*)\"";
    private static final String identifierRegex = "([a-zA-Z_]\\w*)\\b";

    private static final List<TokenInfo> tokenInfos = Arrays.asList(
        new TokenInfo(TokenType.KEYWORD, keywordRegex),
        new TokenInfo(TokenType.SYMBOL, symbolRegex),
        new TokenInfo(TokenType.INT_CONST, constantRegex),
        new TokenInfo(TokenType.STR_CONST, stringRegex),
        new TokenInfo(TokenType.IDENTIFIER, identifierRegex)
    );

    private Queue<Token> tokens;

    public Tokenizer(File input) throws IOException {
        BufferedReader buf = new BufferedReader(new FileReader(input));
        tokens = new ArrayDeque<>();
        
        String line;
        boolean insideBlockComment = false;

        while ((line = buf.readLine()) != null) {
            line = line.trim();

            while (!line.isEmpty()) {
                Matcher m;

                if (line.startsWith("/*")) {
                    insideBlockComment = true;
                }
                if (insideBlockComment) {
                    int endOfBlock = line.indexOf("*/");

                    if (endOfBlock == -1) {
                        break;
                    }

                    line = line.substring(endOfBlock + 2).trim();
                    insideBlockComment = false;
                    continue;
                }
                if (line.startsWith("//")) {
                    break;
                }

                boolean matched = false;

                for (TokenInfo info : tokenInfos) {
                    m = info.pattern.matcher(line);

                    if (m.find()) {
                        tokens.add(new Token(info.type, m.group(1)));

                        // TODO: Maybe work with index instead of recreating Strings
                        line = m.replaceFirst("");
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    buf.close();
                    throw new IllegalArgumentException("Unknown char: " + line);
                }

                line = line.trim();
            }
        }

        buf.close();
    }

    public boolean hasMoreTokens() {
        return !tokens.isEmpty();
    }

    public Token getCurrentToken() {
        return tokens.element();
    }

    public Token advance() {
        return tokens.remove();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: Tokenizer <file|directory>");
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
            Tokenizer tokenizer = new Tokenizer(file);

            List<String> tokenTags = tokenizer.tokens.stream().map(Token::toString).collect(Collectors.toList());
    
            tokenTags.add(0, "<tokens>");
            tokenTags.add("</tokens>");

            String filename = file.getName();
            int extIndex = filename.lastIndexOf('.');
            if (extIndex == -1) {
                extIndex = filename.length();
            }

            Path outputFilename = file.toPath().resolveSibling(filename.substring(0, extIndex) + "Tokens.xml");

            Files.write(outputFilename, tokenTags, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static class TokenInfo {
        private TokenType type;
        private Pattern pattern;

        private TokenInfo(TokenType type, String regex) {
            this.type = type;
            this.pattern = Pattern.compile("^" + regex + "");
        }
    }
}