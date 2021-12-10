package com.maroontress.clione.impl;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import com.maroontress.clione.LexicalParser;

/**
    This reader substitutes {@code '\n'} for all newlines (LF, CRLF, and CR)
    in the stream, even if different newlines are mixed in the stream.

    <p>Note that the {@link LexicalParser} uses {@code '\n'} as the newline
    character.</p>
*/
public final class UnifiedNewlineReader extends AbstractReader {

    private final PushbackReader reader;

    /**
        Creates a reader unifying newlines.

        @param reader The reader from which characters will be read.
    */
    public UnifiedNewlineReader(Reader reader) {
        this.reader = new PushbackReader(reader);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        reader.close();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        var c = reader.read();
        if (c != '\r') {
            return c;
        }
        // replace \r\n and \r with \n
        var next = reader.read();
        if (next != -1 && next != '\n') {
            reader.unread(next);
        }
        return '\n';
    }
}
