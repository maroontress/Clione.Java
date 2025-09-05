package com.maroontress.clione.impl;

import java.io.IOException;
import java.io.Reader;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;

/**
    Provides the stream of the source file.
*/
public interface Source {

    /**
        Closes this source and its upstream {@link Source} or {@link Reader}.

        @throws IOException If an I/O error occurs.
    */
    void close() throws IOException;

    /**
        Returns the filename.

        @return The filename. Or {@code null} if no filename is specified.
    */
    String getFilename();

    /**
        Returns the current location of this source.

        @return The current location.
    */
    SourceLocation getLocation();

    /**
        Returns a new {@link SourceChar} object at the current location of this
        source or EOF.

        <p>This method does not change the current location either if the
        return value represents EOF or is the object that has been pushed
        back. Otherwise, the current location proceeds the next one.</p>

        <p>This method may read two or more characters from the upstream
        {@link Source} or {@link Reader}. It may also replace one or more
        {@link SourceChar} objects with another {@link SourceChar} object and
        return it.</p>

        @return The new {@link SourceChar} object.
        @throws IOException If an I/O error occurs.
        @see SourceChar#isEof()
    */
    SourceChar getChar() throws IOException;

    /**
        Pushes back the specified {@link SourceChar} object.

        <p>The {@link SourceChar} object to push back must not represent
        EOF.</p>

        <p>This method does not change the current location.</p>

        @param c The {@link SourceChar} object to push back.
        @throws IllegalArgumentException If the object represents EOF.
    */
    void ungetChar(SourceChar c);
}
