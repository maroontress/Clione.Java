package com.maroontress.clione.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;
import com.maroontress.clione.SourceSpan;

/**
    The factory of {@link SourceChar} objects.
*/
public final class SourceChars {

    /** Prevents the class from being instantiated. */
    private SourceChars() {
        throw new AssertionError();
    }

    /**
        Returns EOF.

        @param filename The filename.
        @return The new EOF.
    */
    public static SourceChar eof(String filename) {
        return new Eof(filename) {
            @Override
            public SourceSpan getSpan() {
                throw new IllegalStateException();
            }

            @Override
            public List<SourceChar> getChildren() {
                return EMPTY_LIST;
            }
        };
    }

    /**
        Returns a new {@link SourceChar} object representing EOF with the
        specified child characters.

        <p>In some cases, EOF can have its child characters. For example, the
        EOF that follows a backslash followed by a newline character has to
        have them as its child characters, as follows:</p>
        <pre>
        int a = 0;\[NL]
        [EOF]</pre>
        <p>where {@code [NL]} and {@code [EOF]} represent a newline character
        and EOF, respectively.</p>

        <p>The object this method returns behaves as follows:</p>
        <ul>
        <li>The {@link SourceChar#isEof()} method returns {@code true}</li>
        <li>The {@link SourceChar#toChar()} method throw an
        {@link IllegalStateException}</li>
        <li>The {@link SourceChar#getSpan()} method returns the span of the
        specified child characters</li>
        <li>The {@link SourceChar#getChildren()} method returns the specified
        child characters</li>
        </ul>

        <p>Note that the EOF this method returns is an immutable object.</p>

        @param filename The filename.
        @param children The non-empty collection containing the child
            characters.
        @return The new EOF.
        @throws IllegalArgumentException If the {@code children} is empty.
    */
    public static SourceChar eof(String filename,
            Collection<SourceChar> children) {
        if (children.isEmpty()) {
            throw new IllegalArgumentException("children must not be empty");
        }
        var list = List.copyOf(children);
        var start = list.get(0).getSpan();
        var end = list.get(list.size() - 1).getSpan();
        var span = new SourceSpan(start, end);
        return new Eof(filename) {
            @Override
            public SourceSpan getSpan() {
                return span;
            }

            @Override
            public List<SourceChar> getChildren() {
                return list;
            }
        };
    }

    /**
        Returns a new {@link SourceChar} object that has the specified child
        characters.

        <p>Typically, this method is to create the character that follows a
        backslash followed by a newline character.</p>

        <p>Note that the character this method returns is an immutable
        object.</p>

        @param children The non-empty collection containing the child
            characters other than the last child character.
        @param c The character that represents both the last child character
            and the new character itself.
        @return The new {@link SourceChar} object.
        @throws IllegalArgumentException If the {@code children} is empty.
    */
    public static SourceChar of(Collection<SourceChar> children,
                                SourceChar c) {
        if (children.isEmpty()) {
            throw new IllegalArgumentException("children must not be empty");
        }
        var list = Stream.concat(children.stream(), Stream.of(c))
                        .collect(Collectors.toUnmodifiableList());
        var span = new SourceSpan(list.get(0).getSpan(), c.getSpan());
        var filename = c.getFilename();
        return of(c.toChar(), filename, span, list);
    }

    /**
        Returns a new {@link SourceChar} object that has the specified child
        characters.

        <p>Typically, this method is to create the character which is
        substituted for any trigraph sequence.</p>

        <p>Note that the character this method returns is an immutable
        object.</p>

        @param first The first child character.
        @param second The second child character.
        @param third The third child character.
        @param c The character that represents the new character.
        @return The new {@link SourceChar} object.
    */
    public static SourceChar of(SourceChar first, SourceChar second,
                                SourceChar third, char c) {
        var start = first.getSpan().getStart();
        var end = third.getSpan().getEnd();
        var span = new SourceSpan(start, end);
        var filename = first.getFilename();
        return of(c, filename, span, List.of(first, second, third));
    }

    /**
        Returns a new {@link SourceChar} object that has the specified child
        characters.

        <p>Typically, this method is to create the character which is
        substituted for any digraph sequence.</p>

        <p>Note that the character this method returns is an immutable
        object.</p>

        @param first The first child character.
        @param second The second child character.
        @param c The character that represents the new character.
        @return The new {@link SourceChar} object.
    */
    public static SourceChar of(SourceChar first, SourceChar second, char c) {
        var start = first.getSpan().getStart();
        var end = second.getSpan().getEnd();
        var span = new SourceSpan(start, end);
        var filename = first.getFilename();
        return of(c, filename, span, List.of(first, second));
    }

    /**
        Returns a new {@link SourceChar} object that has no child characters
        (that is a leaf character).

        <p>Note that the character this method returns is an immutable
        object.</p>

        @param c The character that represents the new character.
        @param column The column number of the character.
        @param line The line number of the character.
        @return The new {@link SourceChar} object.
    */
    public static SourceChar of(char c, int column, int line) {
        return of(c, null, column, line);
    }

    /**
        Returns a new {@link SourceChar} object that has no child characters
        (that is a leaf character).

        <p>Note that the character this method returns is an immutable
        object.</p>

        @param c The character that represents the new character.
        @param filename The filename.
        @param column The column number of the character.
        @param line The line number of the character.
        @return The new {@link SourceChar} object.
    */
    public static SourceChar of(
            char c, String filename, int column, int line) {
        var w = new SourceLocation(line, column);
        var span = new SourceSpan(w);
        return of(c, filename, span, SourceChar.EMPTY_LIST);
    }

    /**
        Returns a new {@link SourceChar} object that has the specified span
        and the specified child characters.

        <p>Note that the character this method returns is an immutable
        object.</p>

        @param c The character that represents the new character.
        @param filename The filename.
        @param span The span that is the range of the characters in the source
            file.
        @param children The collection containing the child characters, or
            {@link SourceChar#EMPTY_LIST}.
        @return The new {@link SourceChar} object.
    */
    private static SourceChar of(char c, String filename, SourceSpan span,
                                 Collection<SourceChar> children) {
        var list = List.copyOf(children);
        return new SourceChar() {
            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public boolean isEof() {
                return false;
            }

            @Override
            public char toChar() {
                return c;
            }

            @Override
            public SourceSpan getSpan() {
                return span;
            }

            @Override
            public List<SourceChar> getChildren() {
                return list;
            }
        };
    }
}
