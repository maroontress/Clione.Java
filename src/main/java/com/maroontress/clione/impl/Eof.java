package com.maroontress.clione.impl;

import java.util.List;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceSpan;

/**
    Represents the abstract EOF.
*/
public abstract class Eof implements SourceChar {

    private final String filename;

    /**
     * Creates a new instance.
     *
     * @param filename The filename.
     */
    protected Eof(String filename) {
        this.filename = filename;
    }

    /** {@inheritDoc} */
    @Override
    public final String getFilename() {
        return filename;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isEof() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final char toChar() {
        throw new IllegalStateException();
    }

    /** {@inheritDoc} */
    @Override
    public abstract SourceSpan getSpan();

    /** {@inheritDoc} */
    @Override
    public abstract List<SourceChar> getChildren();
}
