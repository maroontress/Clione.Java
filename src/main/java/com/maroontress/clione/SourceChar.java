package com.maroontress.clione;

import java.util.List;
import com.maroontress.clione.impl.Eof;

/**
    A character of the source file or EOF.

    <p>The {@link SourceChar} object represents a character that composes the
    token or EOF. It may also have one or more child characters in some cases.
    For example, it is the case that it represents:</p>

    <ul>
    <li>the character which is substituted for any digraph or trigraph
    sequence</li>
    <li>the character that follows a backslash ({@code \}) at the end of the
    line</li>
    </ul>
*/
public interface SourceChar {

    /** The empty unmodifiable list of {@link SourceChar} objects. */
    List<SourceChar> EMPTY_LIST = List.of();

    /**
        The {@link SourceChar} object representing the end of the source file.

        <p>This object behaves as follows:</p>
        <ul>
        <li>The {@link #isEof()} method returns {@code true}</li>
        <li>The {@link #getFilename()} method returns {@code null}</li>
        <li>The {@link #toChar()} and {@link #getSpan()} methods throw an
        {@link IllegalStateException}</li>
        <li>The {@link #getChildren()} method returns {@link #EMPTY_LIST}</li>
        </ul>

        <p>Do not compare a {@link SourceChar} object with this object to
        determine whether it is EOF or not. Use the {@link #isEof()} method
        instead.</p>

        <p>Note that this is an immutable object.</p>

        @deprecated Not for public use in the future. This field is expected to
        be removed.
    */
    @Deprecated
    SourceChar STATIC_EOF = new Eof(null) {
        @Override
        public SourceSpan getSpan() {
            throw new IllegalStateException();
        }

        @Override
        public List<SourceChar> getChildren() {
            return EMPTY_LIST;
        }
    };

    /**
        Returns the filename.

        @return The filename. Or {@code null} if no filename is specified.
    */
    String getFilename();

    /**
        Returns whether this object represents EOF.

        @return {@code true} if this represents EOF.
    */
    boolean isEof();

    /**
        Returns a {@code char} value corresponding to this object.

        <p>This method throws an {@link IllegalStateException} if this object
        represents EOF.</p>

        @return The {@code char} value.
        @throws IllegalStateException If this object represents EOF.
    */
    char toChar();

    /**
        Returns location of this object in the source file.

        <p>If this object has its child characters, its location depends on
        their location. So, its location may represent a range of the
        characters.</p>

        <p>This method throws an {@link IllegalStateException} if this object
        represents EOF.</p>

        @return The location.
        @throws IllegalStateException If this object represents EOF and has no
        child characters.
    */
    SourceSpan getSpan();

    /**
        Returns the child characters.

        @return The unmodifiable list containing the child characters, or
        {@link #EMPTY_LIST} if this object has no child characters.
    */
    List<SourceChar> getChildren();
}
