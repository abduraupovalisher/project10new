import myenum.TokenType;
import utils.NumberUtils;
import utils.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static myenum.StringEnum.*;
import static myenum.TokenType.*;

public class JackTokenizer {

    private List<String> tokens;
    private int pointer;
    private String thisToken;
    private String fileName;
    private String filePath;
    private final String BLANK = " ";

    public JackTokenizer(String filePath) {
        initPointer();
        tokens = new ArrayList<>();
        String line;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filePath));
            this.filePath = filePath;
            File file = new File(filePath);
            String tempFileName = file.getName();
            this.fileName = tempFileName.substring(0, tempFileName.lastIndexOf('.'));
            line = in.readLine();

            // Variable to check if the line is a multi-line comment
            boolean isMuilLineNeglect = false;
            while (line != null) {
                line = line.trim();

                // Multi-line comment handling
                if (line.startsWith("/*") && !line.endsWith("*/")) {
                    isMuilLineNeglect = true;
                    line = in.readLine();
                    continue;
                } else if (line.endsWith("*/") || line.startsWith("*/")) {
                    isMuilLineNeglect = false;
                    line = in.readLine();
                    continue;
                } else if (line.startsWith("/*") && line.endsWith("*/")) {
                    line = in.readLine();
                    continue;
                }

                // Neglect empty lines and single-line comments
                if (line.equals("") || isMuilLineNeglect || line.startsWith("//")) {
                    line = in.readLine();
                    continue;
                }

                // Tokenize line while handling string constants
                String[] segment = line.split("//")[0].trim().split("\"");
                boolean even = true;
                for (int i = 0; i < segment.length; i++) {
                    String statement = segment[i];
                    if (even) {
                        String[] words = statement.split("\\s+");
                        for (String word : words) {
                            List<String> thisLineTokens = new ArrayList<>();
                            splitToToken(word, thisLineTokens);
                            tokens.addAll(thisLineTokens);
                        }
                        even = false;
                    } else {
                        tokens.add(StringUtils.wrapByDoubleQuotation(statement));
                        even = true;
                    }
                }
                line = in.readLine();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Splits a given word into tokens and adds them to the provided list.
     * This method handles symbols and recursively splits the word if it contains any symbols.
     *
     * @param word   the word to be split into tokens
     * @param tokens the list to which the tokens will be added
     */
    private void splitToToken(String word, List<String> tokens) {
        if (word == null || word.isEmpty()) {
            return;
        }
        if (word.length() == 1) {
            tokens.add(word);
            return;
        }
        boolean isContainSymbol = false;

        for (String symbol : symbols) {
            if (word.contains(symbol)) {
                isContainSymbol = true;
                int symbolIdx = word.indexOf(symbol);
                splitToToken(word.substring(0, symbolIdx), tokens);
                tokens.add(symbol);
                if (symbolIdx + 1 < word.length()) {
                    splitToToken(word.substring(symbolIdx + 1), tokens);
                }
                break;
            }
        }
        if (!isContainSymbol) {
            tokens.add(word);
        }
    }

    public void advance() {
        pointer++;
        this.thisToken = tokens.get(pointer);
    }

    public Boolean hasMoreTokens() {
        return pointer < tokens.size() - 1;
    }

    /**
     * Determines the type of the current token.
     *
     * @return the TokenType of the current token.
     * @throws RuntimeException if the token starts with a digit but is not a valid integer constant.
     */
    public TokenType tokenType() {
        // Check if the current token is a keyword
        if (StringUtils.isKeyword(thisToken)) {
            return KEYWORD;
        }

        // Check if the current token is a symbol
        if (symbols.contains(thisToken)) {
            return SYMBOL;
        }

        // Check if the current token is an integer constant
        if (NumberUtils.isNumeric(thisToken)) {
            return INT_CONSTANT;
        }

        // Check if the current token is a string constant
        if (thisToken.startsWith("\"") && thisToken.endsWith("\"")) {
            return STRING_CONSTANT;
        }

        // Throw a syntax error if the token starts with a digit but isn't a valid integer constant
        if (Character.isDigit(thisToken.charAt(0))) {
            throw new RuntimeException("Syntax error: Identifier cannot start with a digit.");
        }

        // If none of the above, assume the token is an identifier
        return IDENTIFIER;
    }

    public String keyword() {
        if (tokenType() != KEYWORD) {
            throw new RuntimeException("only when type of token is 'KEYWORD' can keyword()");
        }
        return thisToken;
    }

    public String symbol() {
        if (tokenType() != SYMBOL) {
            throw new RuntimeException("only when type of token is 'SYMBOL' can symbol()");
        }
        String token = thisToken;
        switch (thisToken) {
            case ">":
                token = "&gt;";
                break;
            case "<":
                token = "&lt;";
                break;
            case "&":
                token = "&amp;";
                break;
        }
        return token;
    }

    public String identifier() {
        if (tokenType() != IDENTIFIER) {
            throw new RuntimeException("only when type of token is 'IDENTIFIER' can identifier()");
        }
        return thisToken;
    }

    public int intVal() {
        if (tokenType() != INT_CONSTANT) {
            throw new RuntimeException("only when type of token is 'INT_CONSTANT' can intVal()");
        }
        return Integer.parseInt(thisToken);
    }

    public String stringVal() {
        if (tokenType() != STRING_CONSTANT) {
            throw new RuntimeException("only when type of token is 'STRING_CONSTANT' can stringVal()");
        }
        return thisToken.replace("\"", "");
    }

    public void initPointer() {
        pointer = -1;
    }

    public String getThisToken() {
        return switch (tokenType()) {
            case SYMBOL -> symbol();
            case KEYWORD -> keyword();
            case IDENTIFIER -> identifier();
            case INT_CONSTANT -> String.valueOf(intVal());
            case STRING_CONSTANT -> stringVal();
        };
    }
}
