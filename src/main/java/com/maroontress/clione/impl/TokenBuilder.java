package com.maroontress.clione.impl;

import java.util.ArrayDeque;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    The builder of {@link Token} objects that has a mutable sequence of
    characters.
*/
public final class TokenBuilder {

    private final ArrayDeque<SourceChar> queue;

    /**
        Creates a new instance.
    */
    public TokenBuilder() {
        queue = new ArrayDeque<>();
    }

    /**
        Returns the number of characters in this builder.

        <p>Note that this method does not change this builder.</p>

        @return The number of characters in this builder.
    */
    public int size() {
        return queue.size();
    }

    /**
        Appends the specified character to this builder.

        @param c The character to append.
    */
    public void append(SourceChar c) {
        queue.addLast(c);
    }

    /**
        Removes the last character in this builder and returns the character.

        @return The character that was the last one in this builder.
    */
    public SourceChar removeLast() {
        return queue.removeLast();
    }

    /**
        Returns the last character in this builder.

        <p>Note that this method does not change this builder.</p>

        @return The last character in this builder.
    */
    public SourceChar getLast() {
        return queue.getLast();
    }

    /**
        Replace the two characters in this builder with the new character
        representing the specified {@code char} value.

        <p>Note that this builder must have just two characters to be
        replaced. They are assumed to represent any digraph.</p>

        <p>The new substituted character has two child characters. They
        correspond with the characters that were in this builder in the
        same order.</p>

        @param c The character that is substituted for the two characters
        in this builder.
        @throws IllegalStateException If the number of characters in this
        builder is not two.
    */
    public void replaceDigraph(char c) {
        if (queue.size() != 2) {
            throw new IllegalStateException();
        }
        var second = queue.removeLast();
        var first = queue.removeLast();
        queue.addLast(SourceChars.of(first, second, c));
    }

    /**
        Replace the four characters in this builder with the new two
        characters representing the specified {@code char} values.

        <p>Note that this builder must have just four characters to be
        replaced. They are assumed to represent the digraph
        '{@code %:%:}'.</p>

        <p>Each character that is substituted has two child characters.
        The child characters of the first substituted character correspond
        with the first two characters that were in this builder in the same
        order. Likewise, the child characters of the second substituted
        character correspond with the second two characters that were in this
        builder in the same order.</p>

        @param c1 The first character that is substituted for the first two
        characters in this builder.
        @param c2 The second character that is substituted for the second two
        characters in this builder.
        @throws IllegalStateException If the number of characters in this
        builder is not four.
    */
    public void replaceDigraph(char c1, char c2) {
        if (queue.size() != 4) {
            throw new IllegalStateException();
        }
        var fourth = queue.removeLast();
        var third = queue.removeLast();
        var second = queue.removeLast();
        var first = queue.removeLast();
        queue.addLast(SourceChars.of(first, second, c1));
        queue.addLast(SourceChars.of(third, fourth, c2));
    }

    /**
        Returns a new token that represents the characters in this builder
        with the specified token type.

        <p>Note that this method does not change this builder.</p>

        @param type The token type.
        @return The new token.
        @throws IllegalStateException If this builder is empty.
    */
    public Token toToken(TokenType type) {
        if (queue.isEmpty()) {
            throw new IllegalStateException();
        }
        return new DefaultToken(queue, type);
    }

    /**
        Returns a new string that represents the characters in this builder.

        <p>Note that this method does not change this builder.</p>

        @return The new string.
        @throws IllegalStateException If this builder is empty.
    */
    public String toTokenString() {
        if (queue.isEmpty()) {
            throw new IllegalStateException();
        }
        var size = queue.size();
        var b = new StringBuilder(size);
        for (var c : queue) {
            b.append(c.toChar());
        }
        return b.toString();
    }
}
