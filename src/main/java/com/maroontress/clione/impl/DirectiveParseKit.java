package com.maroontress.clione.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.Tokens;

/**
    Provides the feature to parse a preprocessor directive.

    <p>This class reads characters from the source specified in the constructor
    and parses them as preprocessor directives. The #include directive is
    parsed specially.</p>
*/
public final class DirectiveParseKit {

    private final Source source;
    private final Set<String> reservedWords;

    /**
        Constructs a new instance.

        @param source The source.
        @param reservedWords The set of reserved words.
    */
    public DirectiveParseKit(Source source, Set<String> reservedWords) {
        this.source = source;
        this.reservedWords = reservedWords;
    }

    /**
        Adds tokens of the directive to the specified list.

        @param list The list of tokens.
        @throws IOException if an I/O error occurs.
    */
    public void addDirectiveTokens(List<Token> list) throws IOException {
        for (;;) {
            var token = newDirectiveChildToken();
            if (token == null) {
                return;
            }
            list.add(token);
            if (token.isType(TokenType.DIRECTIVE_END)) {
                return;
            }
        }
    }

    /**
        Adds tokens of the include directive to the specified list.

        @param list The list of tokens.
        @throws IOException if an I/O error occurs.
    */
    public void addIncludeDirectiveTokens(List<Token> list)
            throws IOException {
        addSpecialDirectiveTokens(list, this::newIncludeDirectiveChildToken);
    }

    /**
        Adds tokens of the line directive to the specified list.

        @param list The list of tokens.
        @throws IOException if an I/O error occurs.
    */
    public void addLineDirectiveTokens(List<Token> list) throws IOException {
        addSpecialDirectiveTokens(list, this::newLineDirectiveChildToken,
                this::addLineDirectiveLastTokens);
    }

    private void addLineDirectiveLastTokens(List<Token> list)
            throws IOException {
        addSpecialDirectiveTokens(list, this::newLineDirectiveChildToken);
    }

    private void addSpecialDirectiveTokens(
            List<Token> list, TokenSupplier supplier) throws IOException {
        addSpecialDirectiveTokens(list, supplier, this::addDirectiveTokens);
    }

    /**
        Adds tokens of the special directive to the specified list.

        @param list The list of tokens.
        @param supplier The token supplier.
        @param addNextTokens The function to add next tokens.
        @throws IOException if an I/O error occurs.
    */
    private void addSpecialDirectiveTokens(
            List<Token> list, TokenSupplier supplier,
            TokenListConsumer addNextTokens) throws IOException {
        for (;;) {
            var token = supplier.get();
            if (token == null) {
                return;
            }
            list.add(token);
            if (token.isType(TokenType.DIRECTIVE_END)) {
                return;
            }
            if (Tokens.isDelimiterOrComment(token)) {
                continue;
            }
            addNextTokens.accept(list);
            return;
        }
    }

    /**
        Returns the next token of the directive.

        @return The next token.
        @throws IOException if an I/O error occurs.
    */
    public Token newDirectiveChildToken() throws IOException {
        return newChildToken(Transcriber::readDirectiveToken);
    }

    /**
        Returns the next token of the include directive.

        @return The next token.
        @throws IOException if an I/O error occurs.
    */
    public Token newIncludeDirectiveChildToken() throws IOException {
        return newChildToken(Transcriber::readIncludeDirectiveToken);
    }

    /**
        Returns the next token of the line directive.

        @return The next token.
        @throws IOException if an I/O error occurs.
    */
    public Token newLineDirectiveChildToken() throws IOException {
        return newChildToken(Transcriber::readLineDirectiveToken);
    }

    private Token newChildToken(NextTokenReader reader) throws IOException {
        var x = new Transcriber(source);
        var type = reader.apply(x);
        if (type == null) {
            return null;
        }
        return Tokens.normalizeToken(x.toToken(type), reservedWords);
    }

    @FunctionalInterface
    private interface NextTokenReader {
        TokenType apply(Transcriber x) throws IOException;
    }

    @FunctionalInterface
    private interface TokenSupplier {
        Token get() throws IOException;
    }

    /**
        The functional interface that accepts a list of tokens.
    */
    @FunctionalInterface
    public interface TokenListConsumer {
        /**
            Accepts a list of tokens.

            @param list The list of tokens.
            @throws IOException if an I/O error occurs.
        */
        void accept(List<Token> list) throws IOException;
    }
}
