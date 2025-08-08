package com.maroontress.clione.impl;

import java.io.IOException;
import java.io.Reader;

/**
    An abstract {@link Reader} with the only one abstract method that is
    {@link Reader#read()}.
*/
public abstract class AbstractReader extends Reader {

    /**　Creates a new instance.　*/
    protected AbstractReader() {
    }

    /** {@inheritDoc} */
    @Override
    public final int read(char[] array, int offset, int length)
            throws IOException {
        if (length == 0) {
            return 0;
        }
        if (length < 0 || offset < 0) {
            throw new IndexOutOfBoundsException();
        }
        var n = offset + length;
        if (n < offset || n > array.length) {
            throw new IndexOutOfBoundsException();
        }
        for (var k = offset; k < n; ++k) {
            var i = read();
            if (i == -1) {
                return k - offset;
            }
            array[k] = (char) i;
        }
        return length;
    }
}
