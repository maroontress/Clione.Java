package com.maroontress.clione;

import com.maroontress.clione.impl.SourceChars;
import com.maroontress.clione.impl.TokenBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class TokenTest {

    private Token createToken(String value, TokenType type) {
        var builder = new TokenBuilder();
        var size = value.length();
        for (var k = 0; k < size; ++k) {
            var c = SourceChars.of(value.charAt(k), 1 + k, 1);
            builder.append(c);
        }
        return builder.toToken(type);
    }

    @Test
    void isType() {
        var token = createToken("if", TokenType.RESERVED);
        assertThat(token.isType(TokenType.RESERVED), is(true));
        assertThat(token.isType(TokenType.IDENTIFIER), is(false));
    }

    @ParameterizedTest
    @EnumSource(TokenType.class)
    void isTypeWithAllTypes(TokenType type) {
        var token = createToken("test", type);
        for (var t : TokenType.values()) {
            assertThat(token.isType(t), is(t == type));
        }
    }

    @Test
    void isValue() {
        var token = createToken("if", TokenType.RESERVED);
        assertThat(token.isValue("if"), is(true));
        assertThat(token.isValue("else"), is(false));
    }
}
