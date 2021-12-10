package com.maroontress.clione.impl;

import java.util.Collection;
import java.util.List;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceSpan;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    The implementation of a preprocessing token.
 */
public final class DefaultToken implements Token {

    private final List<SourceChar> chars;
    private final TokenType type;
    private final List<Token> children;

    /**
        Creates a new instance.

        @param chars The collection of {@link SourceChar} objects that compose
            this token. It must not contain any character representing EOF.
        @param type The token type of this token.
    */
    public DefaultToken(Collection<SourceChar> chars, TokenType type) {
        this(chars, type, List.of());
    }

    private DefaultToken(Collection<SourceChar> chars, TokenType type,
                         Collection<Token> children) {
        this.chars = List.copyOf(chars);
        this.type = type;
        this.children = List.copyOf(children);
    }

    /** {@inheritDoc} */
    @Override
    public String getValue() {
        var size = chars.size();
        var b = new StringBuilder(size);
        for (var c : chars) {
            b.append(c.toChar());
        }
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    public SourceSpan getSpan() {
        var start = chars.get(0).getSpan().getStart();
        if (children.isEmpty()) {
            var last = chars.size() - 1;
            var end = chars.get(last).getSpan().getEnd();
            return new SourceSpan(start, end);
        }
        var end = children.get(children.size() - 1)
                .getSpan()
                .getEnd();
        return new SourceSpan(start, end);
    }

    /** {@inheritDoc} */
    @Override
    public List<SourceChar> getChars() {
        return chars;
    }

    /** {@inheritDoc} */
    @Override
    public TokenType getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public List<Token> getChildren() {
        return children;
    }

    /** {@inheritDoc} */
    @Override
    public Token withType(TokenType newType) {
        return new DefaultToken(chars, newType, children);
    }

    /** {@inheritDoc} */
    @Override
    public Token withChildren(Collection<Token> newChildren) {
        return new DefaultToken(chars, type, newChildren);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "[value=" + getValue() + ", "
                + "span=" + getSpan() + ", "
                + "chars=" + chars + ", "
                + "type=" + type + ", "
                + "children=" + children + "]";
    }
}
