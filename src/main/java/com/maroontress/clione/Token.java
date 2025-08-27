package com.maroontress.clione;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

/**
    The preprocessing token.

    <p>{@link Token} objects can have children, which means they can be in a
    tree structure. For tokens that the {@link LexicalParser#next()} method
    returns, tokens of type {@link TokenType#DIRECTIVE} only have children.</p>

    <p>The {@link Token} object has its type, span, and characters. The type is
    one of the constants defined in {@link TokenType}, the span represents the
    range of the source file where the token occurs, and the characters are
    {@link SourceChar} objects that compose it.</p>

    <p>Note that the {@link Token} object is an immutable object.</p>
*/
public interface Token {

    /**
        Returns a new string representing this token.

        <p>The string that this method returns does not have the clue of the
        token type and does not include the content of the child tokens.</p>

        @return The new string representing this token.
    */
    String getValue();

    /**
        Returns a new span representing the range of this token in the source
        file.

        @return A new span representing the range of this token.
    */
    SourceSpan getSpan();

    /**
        Returns the characters that compose this token.

        @return The unmodifiable list containing the characters that compose
        this token.
    */
    List<SourceChar> getChars();

    /**
        Returns the type of this token.

        @return The type of this token.
    */
    TokenType getType();

    /**
        Returns the child tokens.

        <p>This method returns an empty list if this token has no child
        tokens.</p>

        @return The unmodifiable list containing the child tokens.
    */
    List<Token> getChildren();

    /**
        Returns a new token that has the same content of this token but
        has the specified token type.

        @param newType The token type of the new token.
        @return The new token.
    */
    Token withType(TokenType newType);

    /**
        Returns a new token that has the same content of this token but
        has the specified child tokens.

        @param newChildren The child tokens of the new token.
        @return The new token.
    */
    Token withChildren(Collection<Token> newChildren);

    /**
        Returns a new string representation of this token that is easy for
        a person to read.

        <p>This method returns a string equal to the value of:</p>
        <pre>
        "[value=" + getValue() + ", span=" + getSpan() + ", "
            + "chars=" + getChars() + ", type=" + getType() + ", "
            + "children=" + getChildren() + "]";
        </pre>

        @return The new string representation of this token.
    */
    @Override
    String toString();

    /**
        Tests if the characters of this token form the given string.

        <p>Returns true if and only if the given string and the return value
        of {@link #getValue()} are equal.</p>

        <p>The comparison is exact (case-sensitive), does not perform any
        normalization or trimming, and considers only this token's characters
        (child tokens, if any, are not involved).</p>

        @param value the string to compare with the token's characters; must
            not be null.
        @return {@code true} if the token's characters equal the given string;
            {@code false} otherwise.
    */
    default boolean isValue(String value) {
        var chars = getChars();
        var size = chars.size();
        if (value.length() != size) {
            return false;
        }
        return IntStream.range(0, size)
                .allMatch(k -> value.charAt(k) == chars.get(k).toChar());
    }

    /**
        Tests whether this token has the specified token type.

        <p>Returns {@code true} if and only if the specified {@code type} is
        the same enum constant as returned by {@link #getType()}.</p>

        @param type the token type to compare; may be {@code null}
        @return {@code true} if this token's type equals {@code type};
            {@code false} otherwise
    */
    default boolean isType(TokenType type) {
        return getType() == type;
    }
}
