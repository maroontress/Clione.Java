package com.maroontress.clione.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;

/**
    This source reads characters from upstream reader, counting the line and
    column number.
*/
public final class ReaderSource implements Source {

    private final UnifiedNewlineReader reader;
    private final Deque<SourceChar> stack;
    private int line = 1;
    private int column = 1;

    /**
        Creates a new source.

        @param reader The reader from which characters will be read.
    */
    public ReaderSource(Reader reader) {
        this.reader = new UnifiedNewlineReader(reader);
        stack = new ArrayDeque<>();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        reader.close();
    }

    /** {@inheritDoc} */
    @Override
    public SourceLocation getLocation() {
        return new SourceLocation(line, column);
    }

    /** {@inheritDoc} */
    @Override
    public SourceChar getChar() throws IOException {
        {
            var c = stack.pollFirst();
            if (c != null) {
                return c;
            }
        }
        var i = reader.read();
        if (i == -1) {
            return SourceChars.eof();
        }
        var c = SourceChars.of((char) i, column, line);
        if (i == '\n') {
            column = 1;
            ++line;
            return c;
        }
        aidSurrogatePair(i);
        ++column;
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public void ungetChar(SourceChar c) {
        if (c.isEof()) {
            throw new IllegalArgumentException("c is EOF");
        }
        stack.addFirst(c);
    }

    private void aidSurrogatePair(int i) throws IOException {
        if (!Character.isHighSurrogate((char) i)) {
            return;
        }
        var next = reader.read();
        if (next == -1) {
            return;
        }
        var nextColumn = (Character.isLowSurrogate((char) next))
                ? column : column + 1;
        stack.addFirst(SourceChars.of((char) next, nextColumn, line));
    }
}
