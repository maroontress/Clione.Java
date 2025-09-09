package com.maroontress.clione;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class TokensTest {

    @Test
    public void concatenateReserved() {
        var left = newToken("in");
        var right = newToken("t");
        var t = Tokens.concatenate(left, right, Set.of("int"));
        assertThat(t.getType(), is(TokenType.RESERVED));
        assertThat(t.getValue(), is("int"));
    }

    @Test
    public void concatenateIdentifier() {
        var left = newToken("int");
        var right = newToken("32");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.IDENTIFIER));
        assertThat(t.getValue(), is("int32"));
    }

    @Test
    public void concatenatePunctuator() {
        var left = newToken("#");
        var right = newToken("#");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.PUNCTUATOR));
        assertThat(t.getValue(), is("##"));
    }

    @Test
    public void concatenateOperator() {
        var left = newToken("+");
        var right = newToken("+");
        var t = Tokens.concatenate(left, right, Set.of());
        // "++" is an operator and should parse as a single token
        assertThat(t.getType(), is(TokenType.OPERATOR));
        assertThat(t.getValue(), is("++"));
    }

    @Test
    public void concatenateNumber() {
        var left = newToken("0");
        var right = newToken("x");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.NUMBER));
        assertThat(t.getValue(), is("0x"));
    }

    @Test
    public void concatenateUnknown() {
        var left = newToken("+");
        var right = newToken("-");
        var t = Tokens.concatenate(left, right, Set.of());
        assertThat(t.getType(), is(TokenType.UNKNOWN));
        assertThat(t.getValue(), is("+-"));
    }

    @Test
    void stringizeEmpty() {
        var where = new SourceLocation(1, 1);
        var result = Tokens.stringize(List.of(), where);

        assertThat(result.getType(), is(TokenType.STRING));
        assertThat(result.getValue(), is("\"\""));
        assertSourceLocationList(result.getChars(), List.of(where, where));
    }

    @Test
    void stringizeTrimsAndCollapsesWhitespace() {
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
    void stringizePreservesEmbeddedStringAndEscapes() {
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

    @Test
    void reparseIncludeFilenameAngle() {
        var tokens = newTokenList("<stdio.h>");
        var result = Tokens.reparseIncludeFilename(tokens, "test.c", Set.of());
        var list = List.of(
                pair("<stdio.h>", TokenType.STANDARD_HEADER));
        test(result, list);
    }

    @Test
    void reparseIncludeFilenameQuote() {
        var tokens = newTokenList("\"my/file.h\"");
        var result = Tokens.reparseIncludeFilename(tokens, "test.c", Set.of());
        var list = List.of(
                pair("\"my/file.h\"", TokenType.FILENAME));
        test(result, list);
    }

    @Test
    void reparseIncludeFilenameWithWhitespace() {
        var tokens = newTokenList("  <stdio.h>");
        var result = Tokens.reparseIncludeFilename(tokens, "test.c", Set.of());
        var list = List.of(
                pair("  ", TokenType.DELIMITER),
                pair("<stdio.h>", TokenType.STANDARD_HEADER));
        test(result, list);
    }

    @Test
    void reparseIncludeFilenameAngleFollowedByAngle() {
        var tokens = newTokenList("<stdio.h> <stdlib.h>\n");
        var result = Tokens.reparseIncludeFilename(tokens, "test.c", Set.of());
        var list = List.of(
                pair("<stdio.h>", TokenType.STANDARD_HEADER),
                pair(" ", TokenType.DELIMITER),
                pair("<", TokenType.OPERATOR),
                pair("stdlib", TokenType.IDENTIFIER),
                pair(".", TokenType.OPERATOR),
                pair("h", TokenType.IDENTIFIER),
                pair(">", TokenType.OPERATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        test(result, list);
    }

    @Test
    void reparseIncludeFilenameInvalid() {
        var tokens = newTokenList("stdio.h");
        var result = Tokens.reparseIncludeFilename(tokens, "test.c", Set.of());
        var list = List.of(
                pair("stdio", TokenType.IDENTIFIER),
                pair(".", TokenType.OPERATOR),
                pair("h", TokenType.IDENTIFIER));
        test(result, list);
    }

    @Test
    void isKeywordOrIdentifier() {
        assertThat(Tokens.isKeywordOrIdentifier(newToken("foo")), is(true));
        assertThat(Tokens.isKeywordOrIdentifier(newToken("int")), is(true));
        assertThat(Tokens.isKeywordOrIdentifier(newToken("42")), is(false));
        assertThat(Tokens.isKeywordOrIdentifier(newToken(";")), is(false));
    }

    private static void test(List<Token> actual, List<Consumer<Token>> list) {
        var size = actual.size();
        assertThat(size, is(list.size()));
        for (var k = 0; k < size; ++k) {
            list.get(k).accept(actual.get(k));
        }
    }

    private static Consumer<Token> pair(String value, TokenType type) {
        return t -> {
            assertThat(t.getValue(), is(value));
            assertThat(t.getType(), is(type));
        };
    }

    private Token newToken(String s) {
        try {
            return LexicalParser.of(new StringReader(s))
                    .next()
                    .orElseThrow();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Token> newTokenList(String s) {
        var parser = LexicalParser.of(new StringReader(s));
        return Stream.generate(() -> getToken(parser))
                .takeWhile(t -> t.isPresent())
                .map(t -> t.get())
                .toList();
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
