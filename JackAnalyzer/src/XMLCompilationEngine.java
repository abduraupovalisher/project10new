import myenum.TokenType;
import utils.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static myenum.StringEnum.*;

public class XMLCompilationEngine implements ICompilationEngine {
    private BufferedWriter bw;
    private JackTokenizer jackTokenizer;

    public XMLCompilationEngine(JackTokenizer jackTokenizer, String test) {
        String outputPath = jackTokenizer.getFilePath();
        try {
            outputPath = outputPath.replace(".jack", "T.xml");
            bw = new BufferedWriter(new FileWriter(outputPath));
            write("<tokens>");
            this.jackTokenizer = jackTokenizer;
            jackTokenizer.advance();
            while (jackTokenizer.hasMoreTokens()) {
                eat(jackTokenizer.tokenType());
            }
            write("</tokens>");
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public XMLCompilationEngine(JackTokenizer jackTokenizer) {
        String outputPath = jackTokenizer.getFilePath();
        try {
            outputPath = outputPath.replace(".jack", ".xml");
            bw = new BufferedWriter(new FileWriter(outputPath));
            this.jackTokenizer = jackTokenizer;
            while (jackTokenizer.hasMoreTokens()) {
                jackTokenizer.advance();
                compileClass();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void compileClass() {
        write("<class>");

        eat("class");
        eat(TokenType.IDENTIFIER);
        eat("{");
        while (jackTokenizer.isClassVarType()) {
            compileClassVarDec();
        }
        while(jackTokenizer.isFunKeyword()){
            compileSubroutine();
        }
        eat("}");
        write("</class>");
    }

    public void compileClassVarDec() {
        write("<classVarDec>");
// static int a, b, c;
        eat(TokenType.KEYWORD);
        compileType();
        eat(TokenType.IDENTIFIER);

        while(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(",")){
            eat(",");
            eat(TokenType.IDENTIFIER);
        } // field int a, b, c;
        eat(";"); //int a, b, c;
        write("</classVarDec>");
    }

    public void compileVarDec() {
        write("<varDec>");
        if (!jackTokenizer.isVarType()) {
            throw new RuntimeException("expect 'var' to declare variable");
        }
        eat(TokenType.KEYWORD);
        compileType();
        eat(TokenType.IDENTIFIER);

        while(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(",")){
            eat(",");
            eat(TokenType.IDENTIFIER);
        }
        eat(";");
        write("</varDec>");
    }

    public String compileType() {
        // int | char | boolean | className
        if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
            eat(TokenType.IDENTIFIER);
        } else if (jackTokenizer.isPrimitiveType() ) {
            eat(TokenType.KEYWORD);
        } else {
            throw new RuntimeException("expect 'int' or 'char' or 'boolean' or identifier to declare variable");
        }
        return jackTokenizer.getThisToken();
    }

    public void compileSubroutine() {
        while (jackTokenizer.isFunKeyword()) {
            write("<subroutineDec>");

            if (!(jackTokenizer.isFunKeyword())) {
                throw new RuntimeException("expect 'constructor' or 'function' or 'method' to declare subroutine");
            }
            eat(TokenType.KEYWORD);
            if (jackTokenizer.isPrimitiveType() || jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                compileType();
            } else if (jackTokenizer.getThisToken().equals(KEYWORD_VOID))  {
                eat(TokenType.KEYWORD);
            } else {
                throw new RuntimeException("expect primitive type or empty string to declare subroutine");
            }

            eat(TokenType.IDENTIFIER);
            eat("(");
            // handles empty parameter list and non-empty parameter list
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(")")) {
                write("<parameterList>");
                write("</parameterList>");
            } else if (jackTokenizer.isPrimitiveType()) {
                compileParameterList();
            } else {
                throw new RuntimeException("expect primitive type or empty string to declare paramList");
            }
            eat(")");
            compileSubroutineBody();
            write("</subroutineDec>");
        }

    }

    public void compileParameterList() {
        write("<parameterList>");
        //arg type
        compileType();

        //identifier
        eat(TokenType.IDENTIFIER);

        while (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(")"))) {
            eat(",");
            compileType();
            eat(TokenType.IDENTIFIER);
        }
        write("</parameterList>");
    }

    public void compileSubroutineBody() {
        write("<subroutineBody>");

        eat("{");
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyword().equals(KEYWORD_VAR)) {
            compileVarDec();
        }
        compileStatements();
        eat("}");
        write("</subroutineBody>");
    }

    public void compileStatements() {
        write("<statements>");
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.isStatement()) {
            switch (jackTokenizer.keyword()) {
                case KEYWORD_LET:
                    compileLet();
                    break;
                case KEYWORD_IF:
                    compileIf();
                    break;
                case KEYWORD_WHILE:
                    compileWhile();
                    break;
                case KEYWORD_DO:
                    compileDo();
                    break;
                case KEYWORD_RETURN:
                    compileReturn();
                    break;
            }
        }
        write("</statements>");
    }


    public void compileLet() {
        write("<letStatement>");
        eat("let");
        eat(TokenType.IDENTIFIER);
        if(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals("[")){
            eat("[");
            compileExpression();
            eat("]");
        }
        eat("=");
        compileExpression();
        eat(";");
        write("</letStatement>");
    }

    public void compileDo() {
        write("<doStatement>");
        eat(KEYWORD_DO);
        eat(TokenType.IDENTIFIER);
        if(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals("(")) {
            eat("(");
            compileExpressionList();
            eat(")");
        } else if(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(".")){
            eat(".");
            eat(TokenType.IDENTIFIER);
            eat("(");
            compileExpressionList();
            eat(")");
        }
        eat(";");
        write("</doStatement>");
    }

    public int compileExpressionList() {
        int count = 0;
        write("<expressionList>");
        while (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(")"))) {
            compileExpression();
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(",")) {
                eat(",");
            }
            count+=1;
        }
        write("</expressionList>");
        return count;
    }

    public void compileWhile() {
        write("<whileStatement>");
        eat(KEYWORD_WHILE);
        eat("(");
        compileExpression();
        eat(")");
        eat("{");
        compileStatements();
        eat("}");
        write("</whileStatement>");
    }

    public void compileReturn() {
        write("<returnStatement>");
        eat(KEYWORD_RETURN);
        if(!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(";"))){
            compileExpression();
        }
        eat(";");
        write("</returnStatement>");
    }

    public void compileIf() {
        write("<ifStatement>");
        eat(KEYWORD_IF);
        eat("(");
        compileExpression();
        eat(")");
        eat("{");
        compileStatements();
        eat("}");
        if(jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyword().equals(KEYWORD_ELSE)){
            eat(KEYWORD_ELSE);
            eat("{");
            compileStatements();
            eat("}");
        }
        write("</ifStatement>");
    }

    public void compileExpression() {
        write("<expression>");
        compileTerm();
        while(jackTokenizer.isOp()){
            eat(TokenType.SYMBOL);
            compileTerm();
        }
        write("</expression>");
    }

    public void compileTerm() {
        write("<term>");
        if(jackTokenizer.tokenType() == TokenType.INT_CONSTANT){
            eat(TokenType.INT_CONSTANT);
        } else if(jackTokenizer.tokenType() == TokenType.STRING_CONSTANT){
            eat(TokenType.STRING_CONSTANT);
        } else if(jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.isKeywordConstant()){
            eat(TokenType.KEYWORD);
        } else if(jackTokenizer.tokenType() == TokenType.IDENTIFIER){
            eat(TokenType.IDENTIFIER);
            if(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals("[")){
                eat("[");
                compileExpression();
                eat("]");
            } else if(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals("(")){
                eat("(");
                compileExpressionList();
                eat(")");
            } else if(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals(".")){
                eat(".");
                eat(TokenType.IDENTIFIER);
                eat("(");
                compileExpressionList();
                eat(")");

                // p1.distance(p2)
            }
        } else if(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol().equals("(")){
            eat("(");
            compileExpression();
            eat(")");
        } else if(jackTokenizer.isUnaryOp()){
            eat(TokenType.SYMBOL);
            compileTerm();
        }
        write("</term>");
    }

    private void advance() {
        if (jackTokenizer.hasMoreTokens()) {
            jackTokenizer.advance();
        }
    }

    private void eat(String str) {
        if (jackTokenizer.getThisToken().equals(str)) {
            // writes <tokenType> str </tokenType>
            write(jackTokenizer.getThisTokenAsTag());
        } else {
            throw new RuntimeException("expect " + str + " but get " + jackTokenizer.getThisToken());
        }
        advance();
    }

    private void eat(TokenType tokenType) {
        if (jackTokenizer.tokenType() == tokenType) {
            // writes <tokenType> val </tokenType>
            write(jackTokenizer.getThisTokenAsTag());
        } else {
            throw new RuntimeException("expect " + StringUtils.getTokenType(tokenType) + " but get " + StringUtils.getTokenType(jackTokenizer.tokenType()));
        }
        advance();
    }

    private void write(String str) {
        try {
            bw.write(str);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
