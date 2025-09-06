package com.maroontress.clione.impl;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class DirectiveParseKitTest {

    private static Consumer<Token> pair(String value, TokenType type) {
        return t -> {
            assertThat(t.getValue(), is(value));
            assertThat(t.getType(), is(type));
        };
    }

    private void check(String s, List<Consumer<Token>> expected,
             Function<DirectiveParseKit, TokenListConsumer> consumerProvider)
             throws IOException {
        var reader = new StringReader(s);
        var source = new ReaderSource(reader, null);
        var kit = new DirectiveParseKit(source, new HashSet<>());
        var actual = new ArrayList<Token>();
        consumerProvider.apply(kit).accept(actual);

        assertThat(actual.size(), is(expected.size()));
        for (int i = 0; i < actual.size(); i++) {
            expected.get(i).accept(actual.get(i));
        }
    }

    private void test(String s, List<Consumer<Token>> expected)
            throws IOException {
        check(s, expected, kit -> kit::addDirectiveTokens);
    }

    private void testInclude(String s, List<Consumer<Token>> expected)
            throws IOException {
        check(s, expected, kit -> kit::addIncludeDirectiveTokens);
    }

    @Test
    public void addDirectiveTokensWithEmpty() throws IOException {
        test("", List.of());
    }

    @Test
    public void addDirectiveTokensWithDirectiveEnd() throws IOException {
        var expected = List.of(
            pair("\n", TokenType.DIRECTIVE_END));
        test("\n", expected);
    }

    @Test
    public void addDirectiveTokens() throws IOException {
        var s = """
            FOO 123
            """;
        var expected = List.of(
                pair("FOO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair("\n", TokenType.DIRECTIVE_END));
        test(s, expected);
    }

    @Test
    public void addDirectiveTokensWithComment() throws IOException {
        var s = """
            FOO /* comment */ BAR
            """;
        var expected = List.of(
            pair("FOO", TokenType.IDENTIFIER),
            pair(" ", TokenType.DELIMITER),
            pair("/* comment */", TokenType.COMMENT),
            pair(" ", TokenType.DELIMITER),
            pair("BAR", TokenType.IDENTIFIER),
            pair("\n", TokenType.DIRECTIVE_END));
        test(s, expected);
    }

    @Test
    public void addIncludeDirectiveTokensWithEmpty() throws IOException {
        testInclude("", List.of());
    }

    @Test
    public void addIncludeDirectiveTokensWithDirectiveEnd() throws IOException {
        var expected = List.of(
            pair("\n", TokenType.DIRECTIVE_END));
        testInclude("\n", expected);
    }

    @Test
    public void addIncludeDirectiveTokensWithSystemHeader() throws IOException {
        var s = """
            <stdio.h>
            """;
        var expected = List.of(
            pair("<stdio.h>", TokenType.STANDARD_HEADER),
            pair("\n", TokenType.DIRECTIVE_END));
        testInclude(s, expected);
    }

    @Test
    public void addIncludeDirectiveTokensWithUserHeader() throws IOException {
        var s = """
            "myheader.h"
            """;
        var expected = List.of(
            pair("\"myheader.h\"", TokenType.FILENAME),
            pair("\n", TokenType.DIRECTIVE_END));
        testInclude(s, expected);
    }

    @FunctionalInterface
    private interface TokenListConsumer {
        void accept(List<Token> tokens) throws IOException;
    }
}
