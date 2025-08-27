package com.maroontress.clione;

import com.maroontress.clione.impl.DefaultToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public final class TokenTest {

    private Token createToken(String value, TokenType type) {
        var chars = value.chars()
                .mapToObj(c -> new TestSourceChar((char) c))
                .collect(Collectors.<SourceChar>toList());
        return new DefaultToken(chars, type);
    }

    @Test
    void isType() {
        var token = createToken("if", TokenType.RESERVED);
        assertTrue(token.isType(TokenType.RESERVED));
        assertFalse(token.isType(TokenType.IDENTIFIER));
    }

    @ParameterizedTest
    @EnumSource(TokenType.class)
    void isTypeWithAllTypes(TokenType type) {
        var token = createToken("test", type);
        for (var t : TokenType.values()) {
            assertEquals(t == type, token.isType(t));
        }
    }

    @Test
    void isValue() {
        var token = createToken("if", TokenType.RESERVED);
        assertTrue(token.isValue("if"));
        assertFalse(token.isValue("else"));
    }

    // A simple implementation of SourceChar for testing.
    private record TestSourceChar(char c) implements SourceChar {

        @Override
        public char toChar() {
            return c;
        }

        @Override
        public SourceSpan getSpan() {
            // Not needed for these tests.
            return null;
        }

        @Override
        public List<SourceChar> getChildren() {
            return List.of();
        }

        @Override
        public boolean isEof() {
            return false;
        }
    }
}
