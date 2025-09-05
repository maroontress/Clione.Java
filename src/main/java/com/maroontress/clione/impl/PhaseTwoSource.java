package com.maroontress.clione.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;

/**
    This source reads characters from upstream source, splicing lines ended
    with the backslash (\).
*/
public final class PhaseTwoSource implements Source {

    private final Source source;
    private Function<SourceChar, SourceChar> eofIdentity = this::initializeEof;

    /**
        Creates a source splicing lines.

        @param source The upstream source.
    */
    public PhaseTwoSource(Source source) {
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
        var c = read();
        return !c.isEof() ? c : eofIdentity.apply(c);
    }

    /** {@inheritDoc} */
    @Override
    public void ungetChar(SourceChar c) {
        source.ungetChar(c);
    }

    private SourceChar read() throws IOException {
        var list = SourceChar.EMPTY_LIST;
        for (;;) {
            var c = source.getChar();
            if (c.isEof()) {
                return compose(list, c);
            }
            if (c.toChar() != '\\') {
                return compose(list, c);
            }
            var next = source.getChar();
            if (next.isEof()) {
                return compose(list, c);
            }
            if (next.toChar() != '\n') {
                source.ungetChar(next);
                return compose(list, c);
            }
            if (list.isEmpty()) {
                list = new ArrayList<>();
            }
            Collections.addAll(list, c, next);
        }
    }

    private SourceChar compose(List<SourceChar> list, SourceChar c) {
        if (list.isEmpty()) {
            return c;
        }
        if (c.isEof()) {
            return SourceChars.eof(getFilename(), list);
        }
        return SourceChars.of(list, c);
    }

    private SourceChar initializeEof(SourceChar eof) {
        eofIdentity = c -> eof;
        return eof;
    }
}
