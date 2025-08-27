package com.maroontress.clione;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class TokensTest {

    @Test
    public void concatenateReserved() throws IOException {
        var left = newToken("in");
        var right = newToken("t");
        var t = Tokens.concatenate(left, right, Set.of("int"));
        assertThat(t.getType(), is(TokenType.RESERVED));
        assertThat(t.getValue(), is("int"));
    }

    @Test
    public void concatenateIdentifier() throws IOException {
        var left = newToken("int");
        var right = newToken("32");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.IDENTIFIER));
        assertThat(t.getValue(), is("int32"));
    }

    @Test
    public void concatenatePunctuator() throws IOException {
        var left = newToken("#");
        var right = newToken("#");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.PUNCTUATOR));
        assertThat(t.getValue(), is("##"));
    }

    @Test
    public void concatenateOperator() throws IOException {
        var left = newToken("+");
        var right = newToken("+");
        var t = Tokens.concatenate(left, right, Set.of());
        // "++" is an operator and should parse as a single token
        assertThat(t.getType(), is(TokenType.OPERATOR));
        assertThat(t.getValue(), is("++"));
    }

    @Test
    public void concatenateNumber() throws IOException {
        var left = newToken("0");
        var right = newToken("x");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.NUMBER));
        assertThat(t.getValue(), is("0x"));
    }

    @Test
    public void concatenateUnknown() throws IOException {
        var left = newToken("+");
        var right = newToken("-");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.UNKNOWN));
        assertThat(t.getValue(), is("+-"));
    }

    @Test
    void stringizeEmpty() throws IOException {
        var result = Tokens.stringize(List.of(), new SourceLocation(1, 1));

        assertThat(result.getType(), is(TokenType.STRING));
        assertThat(result.getValue(), is("\"\""));
    }

    @Test
    void stringizeTrimsAndCollapsesWhitespace() throws IOException {
        var list = newTokenList("""
             /**/ foo /**/ bar /**/
            """);

        var result = Tokens.stringize(list, new SourceLocation(1, 1));

        assertThat(result.getType(), is(TokenType.STRING));
        assertThat(result.getValue(), is("\"foo bar\""));
    }

    @Test
    void stringizePreservesEmbeddedStringAndEscapes() throws IOException {
        var list = newTokenList("""
            x "a \\ b" y
            """);

        var result = Tokens.stringize(list, new SourceLocation(1, 1));

        assertThat(result.getType(), is(TokenType.STRING));
        assertThat(result.getValue(), is("""
            "x \\"a \\\\ b\\" y"
            """.trim()));
    }

    private Token newToken(String s) throws IOException {
        return LexicalParser.of(new StringReader(s))
                .next()
                .orElseThrow();
    }

    private List<Token> newTokenList(String s) throws IOException {
        var parser = LexicalParser.of(new StringReader(s));
        try {
            return Stream.generate(() -> getToken(parser))
                    .takeWhile(t -> t.isPresent())
                    .map(t -> t.get())
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Optional<Token> getToken(LexicalParser parser) {
        try {
            return parser.next();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
