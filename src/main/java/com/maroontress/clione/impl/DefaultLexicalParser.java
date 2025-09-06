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
import com.maroontress.clione.Tokens;

/**
    The default implementation of {@link LexicalParser}.
*/
public final class DefaultLexicalParser implements LexicalParser {

    private final Source source;
    private final Set<String> reservedWords;
    private boolean isTheFirstTokenFound;

    /**
        Creates a new instance.

        @param reader The reader that provides the stream of the source file.
        @param filename The filename.
        @param reservedWords The collection that contains reserved keywords.
            Note that the constructor copies the collection, so changes to the
            collection do not affect this instance.
    */
    public DefaultLexicalParser(Reader reader, String filename,
            Collection<String> reservedWords) {
        this(reader, filename, Set.copyOf(reservedWords));
    }

    private DefaultLexicalParser(Reader reader, String filename,
            Set<String> reservedWords) {
        source = new PhaseTwoSource(new PhaseOneSource(
                new ReaderSource(reader, filename)));
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
    public String getFilename() {
        return source.getFilename();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Token> next() throws IOException {
        return Optional.ofNullable(newToken());
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getReservedWords() {
        return reservedWords;
    }

    private Token newToken() throws IOException {
        var x = new Transcriber(source);
        var type = x.readToken();
        if (type == null) {
            return null;
        }
        var token = x.toToken(type);
        if (type == TokenType.DELIMITER && token.isValue("\n")) {
            isTheFirstTokenFound = false;
            return token;
        }
        if (type == TokenType.DELIMITER || type == TokenType.COMMENT) {
            return token;
        }
        if (!isTheFirstTokenFound
                && type == TokenType.PUNCTUATOR
                && token.isValue("#")) {
            return newDirectiveToken(token.withType(TokenType.DIRECTIVE));
        }
        isTheFirstTokenFound = true;
        return Tokens.normalizeToken(token, reservedWords);
    }

    private Token newDirectiveToken(Token token) throws IOException {
        var children = newDirectiveChildTokens();
        return token.withChildren(children);
    }

    private List<Token> newDirectiveChildTokens() throws IOException {
        var kit = new DirectiveParseKit(source, reservedWords);
        var children = new ArrayList<Token>();
        for (;;) {
            var child = kit.newDirectiveChildToken();
            if (child == null) {
                return children;
            }
            if (child.isType(TokenType.DIRECTIVE_END)) {
                children.add(child);
                return children;
            }
            if (Tokens.isDelimiterOrComment(child)) {
                children.add(child);
                continue;
            }
            var value = child.getValue();
            if (!Keywords.PP_DIRECTIVE_NAMES.contains(value)) {
                // INVALID
                children.add(child);
                kit.addDirectiveTokens(children);
                return children;
            }
            children.add(child.withType(TokenType.DIRECTIVE_NAME));
            AddTokens addTokens = value.equals("include")
                ? kit::addIncludeDirectiveTokens
                : kit::addDirectiveTokens;
            addTokens.accept(children);
            return children;
        }
    }

    @FunctionalInterface
    private interface AddTokens {
        void accept(List<Token> list) throws IOException;
    }
}
