package com.maroontress.clione.impl;

import java.io.IOException;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.TokenType;

/**
    The function that changes the state of the specified {@link Transcriber}
    object, lets it read characters from its source and store a new token in
    its builder, and returns the token type of the stored token.

    @see DefaultTokenizer
*/
@FunctionalInterface
public interface Tokenizer {

    /**
        Returns the token type of the token that the specified
        {@link Transcriber} object reads.

        <p>The transcriber reads characters from its source to build a new
        token. It stores the building token in its {@link TokenBuilder}
        object. So use {@link Transcriber#toToken(TokenType)} method to get
        the new token object as follows:</p>

        <pre>
        Token newToken(Transcriber x, Tokenizer tokenizer) throws IOException {
            var type = tokenizer.apply(x);
            if (type == null) {
                return null;
            }
            return x.toToken(type);
        }</pre>

        <p>Note that this function may return {@code null} unlike
        {@link DefaultTokenizer#apply(Transcriber, SourceChar)}.</p>

        @param x The transcriber.
        @return {@code null} if the transcriber's source has reached EOF.
            Otherwise, the token type.
        @throws IOException If an I/O error occurs.
    */
    TokenType apply(Transcriber x) throws IOException;
}
