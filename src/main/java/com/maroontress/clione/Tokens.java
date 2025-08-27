package com.maroontress.clione;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.maroontress.clione.impl.ReaderSource;
import com.maroontress.clione.impl.TokenBuilder;
import com.maroontress.clione.impl.Transcriber;

/**
    The utility class for operations on token sequence.
*/
public final class Tokens {

    private Tokens() {
    }

    /**
        Concatenates the two tokens and returns a newly created token.

        <p>The characters of {@code left} and {@code right} are appended in
        that order to form a single token text. The resulting token's type is
        determined as follows:</p>

        <ol>
        <li>If the concatenated token text is equal to one of the strings in
            {@code reservedWords}, the resulting token type is {@link
            TokenType#RESERVED}.</li>
        <li>Otherwise, the method attempts to parse the concatenated text as a
            single token. If parsing succeeds and yields exactly one token,
            the resulting token type is set to that parsed type.</li>
        <li>If parsing produces more than one token, or parsing fails, the
            resulting token type is {@link TokenType#UNKNOWN}.</li>
        </ol>

        @param left The left token.
        @param right The right token.
        @param reservedWords A set of the reserved words.
        @return The new token.
    */
    public static Token concatenate(Token left, Token right,
            Set<String> reservedWords) {
        var builder = new TokenBuilder();
        Stream.of(left, right)
                .flatMap(t -> t.getChars().stream())
                .forEach(builder::append);
        var tokenString = builder.toTokenString();
        if (reservedWords.contains(tokenString)) {
            return builder.toToken(TokenType.RESERVED);
        }
        return getTokenType(tokenString)
                .map(builder::toToken)
                .orElseGet(() -> builder.toToken(TokenType.UNKNOWN));
    }

    private static Optional<TokenType> getTokenType(String tokenString) {
        var source = new ReaderSource(new StringReader(tokenString));
        var x = new Transcriber(source);
        try {
            var type = x.readToken();
            /*
                TokenBuilder#toTokenString() never returns an empty string,
                so the　null-check below is unnecessary:

                    if (type == null) {
                        return Optional.empty();
                    }

                We therefore assume 'type' is non-null here.
            */
            var nextType = x.readToken();
            if (nextType != null) {
                // This means the concatenated string represents more than one
                // tokens.
                return Optional.empty();
            }
            return Optional.of(type);
        } catch (IOException e) {
            // This should not happen with StringReader.
            return Optional.empty();
        }
    }
}
