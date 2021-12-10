package com.maroontress.clione.impl;

import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class UnifiedNewlineReaderTest {

    @Test
    public void mixedNewlines() throws IOException {
        var s = "a\r\r\n\nb";
        var r = new UnifiedNewlineReader(new StringReader(s));
        var array = new char[5];
        var n = r.read(array, 0, array.length);
        assertEquals(array.length, n);
        assertArrayEquals(array, "a\n\n\nb".toCharArray());
        var i = r.read();
        assertEquals(-1, i);
    }
}
