package com.maroontress.clione.impl;

import java.io.IOException;
import java.util.Map;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;

/**
    This source reads characters from upstream source, replacing trigraph
    sequences with the character that they represent.
*/
public final class PhaseOneSource implements Source {

    private static final Map<Character, Character> REPLACEMENT_MAP
            = newReplacementMap();

    private final Source source;

    /**
        Creates a source replacing trigraph sequences.

        @param source The reader from which characters will be read.
    */
    public PhaseOneSource(Source source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        source.close();
    }

    /** {@inheritDoc} */
    @Override
    public String getFilename() {
        return source.getFilename();
    }

    /** {@inheritDoc} */
    @Override
    public SourceLocation getLocation() {
        return source.getLocation();
    }

    /** {@inheritDoc} */
    @Override
    public SourceChar getChar() throws IOException {
        var first = read();
        if (first.isEof()) {
            return first;
        }
        if (first.toChar() != '?') {
            return first;
        }
        var second = read();
        if (second.isEof()) {
            return first;
        }
        if (second.toChar() != '?') {
            unread(second);
            return first;
        }
        var third = read();
        if (third.isEof()) {
            unread(second);
            return first;
        }
        var c = REPLACEMENT_MAP.get(third.toChar());
        if (c == null) {
            unread(third);
            unread(second);
            return first;
        }
        return SourceChars.of(first, second, third, c);
    }

    /** {@inheritDoc} */
    @Override
    public void ungetChar(SourceChar c) {
        unread(c);
    }

    private static Map<Character, Character> newReplacementMap() {
        return Map.of(
                '=', '#',
                '/', '\\',
                '\'', '^',
                '(', '[',
                ')', ']',
                '!', '|',
                '<', '{',
                '>', '}',
                '-', '~');
    }

    private SourceChar read() throws IOException {
        return source.getChar();
    }

    private void unread(SourceChar c) {
        source.ungetChar(c);
    }
}
