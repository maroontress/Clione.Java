package com.maroontress.clione.impl;

import java.io.IOException;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.TokenType;

/**
    The function that changes the state of the specified {@link Transcriber}
    object with the specified {@link SourceChar} object, lets the
    {@link Transcriber} object read characters from its source and store a new
    token in its builder, and returns the token type of the stored token.

    @see Transcriber#readTokenOtherwise(Case.Mapper, DefaultTokenizer)
    @see Tokenizer
*/
@FunctionalInterface
public interface DefaultTokenizer {
    /**
        Returns the token type of the token composed of the specified
        {@link SourceChar} and if needed the characters supplied from the
        specified {@link Transcriber} object.

        <p>The transcriber may read characters from its source to build a
        new token. It stores the building token in its {@link TokenBuilder}
        object. So use {@link Transcriber#toToken(TokenType)} method to get
        the new token object as follows:</p>

        <pre>
        Token newToken(Transcriber x, SourceChar c,
                       DefaultTokenizer otherwise) throws IOException {
            var type = otherwise.apply(x, c);
            return x.toToken(type);
        }</pre>

        <p>Note that this function does not return {@code null} unlike
        {@link Tokenizer#apply(Transcriber)}.</p>

        @param x The transcriber.
        @param c The first character that the {@link Transcriber} has read from
            its source but has not yet been stored to its builder.
        @return The token type.
        @throws IOException If an I/O error occurs.
    */
    TokenType apply(Transcriber x, SourceChar c) throws IOException;
}
