package com.maroontress.clione;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
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
        var where = new SourceLocation(1, 1);
        var result = Tokens.stringize(List.of(), where);

        assertThat(result.getType(), is(TokenType.STRING));
        assertThat(result.getValue(), is("\"\""));
        assertSourceLocationList(result.getChars(), List.of(where, where));
    }

    @Test
    void stringizeTrimsAndCollapsesWhitespace() throws IOException {
        var list = newTokenList("""
             /**/ foo /**/ bar /**/
            """);
        var where = new SourceLocation(1, 1);
        var result = Tokens.stringize(list, where);

        assertThat(result.getType(), is(TokenType.STRING));
        assertThat(result.getValue(), is("\"foo bar\""));
        var expectedLocations = List.of(
                where,
                /* f */ new SourceLocation(1, 7),
                /* o */ new SourceLocation(1, 8),
                /* o */ new SourceLocation(1, 9),
                where,
                /* b */ new SourceLocation(1, 16),
                /* a */ new SourceLocation(1, 17),
                /* r */ new SourceLocation(1, 18),
                where);
        assertSourceLocationList(result.getChars(), expectedLocations);
    }

    @Test
    void stringizePreservesEmbeddedStringAndEscapes() throws IOException {
        var list = newTokenList("""
            x "a \\ b" y
            """);
        var where = new SourceLocation(1, 1);
        var result = Tokens.stringize(list, where);

        assertThat(result.getType(), is(TokenType.STRING));
        assertThat(result.getValue(), is("""
            "x \\"a \\\\ b\\" y"
            """.trim()));
        var expectedLocations = List.of(
                where,
                /* x */ new SourceLocation(1, 1),
                where,
                where,
                /* " */ new SourceLocation(1, 3),
                /* a */ new SourceLocation(1, 4),
                /*   */ new SourceLocation(1, 5),
                where,
                /* \ */ new SourceLocation(1, 6),
                /*   */ new SourceLocation(1, 7),
                /* b */ new SourceLocation(1, 8),
                where,
                /* " */ new SourceLocation(1, 9),
                where,
                /* y */ new SourceLocation(1, 11),
                where);
        assertSourceLocationList(result.getChars(), expectedLocations);
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

    private void assertSourceLocationList(List<SourceChar> chars,
            List<SourceLocation> locations) {
        var size = chars.size();
        assertThat(size, is(locations.size()));
        IntStream.range(0, size).forEach(k -> {
            var actual = chars.get(k).getSpan().getStart();
            var expected = locations.get(k);
            assertSourceLocation(actual, expected);
        });
    }

    private void assertSourceLocation(
            SourceLocation actual, SourceLocation expected) {
        assertThat(actual.getLine(), is(expected.getLine()));
        assertThat(actual.getColumn(), is(expected.getColumn()));
    }
}
