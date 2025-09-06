package com.maroontress.clione.impl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;

/**
    The source that is created from the given character sequence.
*/
public final class ReparseSource implements Source {

    private final Deque<SourceChar> queue;
    private final SourceChar eof;
    private final SourceChar lastChar;

    /**
        Creates a new instance of {@code ReparseSource}.

        @param chars The character sequence.
        @param filename The filename.
    */
    public ReparseSource(Collection<SourceChar> chars, String filename) {
        if (chars.isEmpty()) {
            throw new IllegalArgumentException("chars is empty");
        }
        this.queue = new ArrayDeque<>(chars);
        this.eof = SourceChars.eof(filename);
        this.lastChar = queue.peekLast();
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
    }

    /** {@inheritDoc} */
    @Override
    public SourceLocation getLocation() {
        var c = (queue.isEmpty())
                ? lastChar
                : queue.peekFirst();
        return c.getSpan().getStart();
    }

    /** {@inheritDoc} */
    @Override
    public SourceChar getChar() {
        if (queue.isEmpty()) {
            return eof;
        }
        return queue.removeFirst();
    }

    /** {@inheritDoc} */
    @Override
    public void ungetChar(SourceChar c) {
        if (c.isEof()) {
            throw new IllegalArgumentException("c is EOF");
        }
        queue.addFirst(c);
    }

    @Override
    public String getFilename() {
        return eof.getFilename();
    }
}
