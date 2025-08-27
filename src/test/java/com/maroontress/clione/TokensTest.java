package com.maroontress.clione;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

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

    private Token newToken(String s) throws IOException {
        return LexicalParser.of(new StringReader(s))
                .next()
                .orElseThrow();
    }
}
