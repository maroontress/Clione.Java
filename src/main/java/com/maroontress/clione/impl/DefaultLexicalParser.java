package com.maroontress.clione.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.maroontress.clione.Keywords;
import com.maroontress.clione.LexicalParser;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    The default implementation of {@link LexicalParser}.
*/
public final class DefaultLexicalParser implements LexicalParser {

    private final Source source;
    private final Set<String> reservedWords;

    /**
        Creates a new instance.

        <p>The instance considers {@link Keywords#C11} as reserved
        words.</p>

        @param reader The reader that provides the stream of the source file.
    */
    public DefaultLexicalParser(Reader reader) {
        this(reader, Keywords.C11);
    }

    /**
        Creates a new instance.

        @param reader The reader that provides the stream of the source file.
        @param reservedWords The collection that contains reserved keywords.
        Note that the constructor copies the collection, so changes to the
        collection do not affect this instance.
    */
    public DefaultLexicalParser(Reader reader,
                                Collection<String> reservedWords) {
        this(reader, Set.copyOf(reservedWords));
    }

    private DefaultLexicalParser(Reader reader, Set<String> reservedWords) {
        source = new PhaseTwoSource(new PhaseOneSource(
                new ReaderSource(reader)));
        this.reservedWords = reservedWords;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        source.close();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SourceChar> getEof() throws IOException {
        var c = source.getChar();
        if (!c.isEof()) {
            source.ungetChar(c);
            return Optional.empty();
        }
        return Optional.of(c);
    }

    /** {@inheritDoc} */
    @Override
    public SourceLocation getLocation() {
        return source.getLocation();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Token> next() throws IOException {
        return Optional.ofNullable(newToken());
    }

    private Token newToken() throws IOException {
        var x = new Transcriber(source);
        var type = readToken(x);
        if (type == null) {
            return null;
        }
        var token = x.toToken(type);
        if (type == TokenType.IDENTIFIER) {
            return reservedWords.contains(token.getValue())
                    ? token.withType(TokenType.RESERVED)
                    : token;
        }
        if (type == TokenType.DIRECTIVE) {
            return newDirectiveToken(token);
        }
        return token;
    }

    private Token newDirectiveToken(Token token) throws IOException {
        var children = newDirectiveChildTokens();
        return token.withChildren(children);
    }

    private List<Token> newDirectiveChildTokens() throws IOException {
        var children = new ArrayList<Token>();
        for (;;) {
            var child = newDirectiveChildToken();
            if (child == null) {
                return children;
            }
            var type = child.getType();
            if (type == TokenType.DIRECTIVE_END) {
                children.add(child);
                return children;
            }
            if (type == TokenType.DELIMITER || type == TokenType.COMMENT) {
                children.add(child);
                continue;
            }
            var value = child.getValue();
            if (!Keywords.PP_DIRECTIVE_NAMES.contains(value)) {
                // INVALID
                children.add(child);
                addDirectiveTokens(children);
                return children;
            }
            children.add(child.withType(TokenType.DIRECTIVE_NAME));
            if (value.equals("include")) {
                addIncludeDirectiveTokens(children);
                return children;
            }
            addDirectiveTokens(children);
            return children;
        }
    }

    private void addDirectiveTokens(List<Token> list)
            throws IOException {
        for (;;) {
            var token = newDirectiveChildToken();
            if (token == null) {
                return;
            }
            list.add(token);
            var type = token.getType();
            if (type == TokenType.DIRECTIVE_END) {
                return;
            }
        }
    }

    private void addIncludeDirectiveTokens(List<Token> list)
            throws IOException {
        for (;;) {
            var token = newIncludeDirectiveChildToken();
            if (token == null) {
                return;
            }
            list.add(token);
            var type = token.getType();
            if (type == TokenType.DIRECTIVE_END) {
                return;
            }
            if (type == TokenType.DELIMITER
                || type == TokenType.COMMENT) {
                continue;
            }
            addDirectiveTokens(list);
            return;
        }
    }

    private Token newDirectiveChildToken() throws IOException {
        return newChildToken(DefaultLexicalParser::readDirectiveToken);
    }

    private Token newIncludeDirectiveChildToken() throws IOException {
        return newChildToken(DefaultLexicalParser::readIncludeDirectiveToken);
    }

    private Token newChildToken(NextTokenReader reader) throws IOException {
        var x = new Transcriber(source);
        var type = reader.apply(x);
        if (type == null) {
            return null;
        }
        var token = x.toToken(type);
        if (type == TokenType.IDENTIFIER) {
            return reservedWords.contains(token.getValue())
                    ? token.withType(TokenType.RESERVED)
                    : token;
        }
        return token;
    }

    private static TokenType readToken(Transcriber x) throws IOException {
        return x.readTokenOtherwise(Switches.DEFAULT,
                DefaultLexicalParser::readSymbol);
    }

    private static TokenType readDirectiveToken(Transcriber x)
            throws IOException {
        return x.readTokenOtherwise(Switches.DIRECTIVE,
                DefaultLexicalParser::readSymbol);
    }

    private static TokenType readIncludeDirectiveToken(Transcriber x)
            throws IOException {
        return x.readTokenOtherwise(Switches.INCLUDE_DIRECTIVE,
                DefaultLexicalParser::readSymbol);
    }

    private static TokenType readSymbol(Transcriber x, SourceChar i)
            throws IOException {
        var s = x.getSource();
        var b = x.getBuilder();
        var c = i.toChar();
        if (Character.isHighSurrogate(c)) {
            var j = s.getChar();
            if (j.isEof()) {
                b.append(i);
                return TokenType.UNKNOWN;
            }
            var n = j.toChar();
            if (!Character.isLowSurrogate(n)) {
                s.ungetChar(j);
                b.append(i);
                return TokenType.UNKNOWN;
            }
            b.append(i);
            b.append(j);
            var u = Character.toCodePoint(c, n);
            if (!Character.isUnicodeIdentifierStart(u)) {
                return TokenType.UNKNOWN;
            }
            x.readIdentifier();
            return TokenType.IDENTIFIER;
        }
        if (Character.isUnicodeIdentifierStart(c)) {
            b.append(i);
            x.readIdentifier();
            return TokenType.IDENTIFIER;
        }
        b.append(i);
        return TokenType.UNKNOWN;
    }

    @FunctionalInterface
    private interface NextTokenReader {
        TokenType apply(Transcriber x) throws IOException;
    }
}
