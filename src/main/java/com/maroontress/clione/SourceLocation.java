package com.maroontress.clione;

/**
    The location of the source file.

    <p>Note that the {@link SourceLocation} instance is an immutable
    object.</p>
*/
public final class SourceLocation {

    private final int line;
    private final int column;

    /**
        Creates a new instance.

        @param line The line number.
        @param column The column number.
        @throws IllegalArgumentException If the {@code line} or {@code column}
            is less than or equal to zero.
    */
    public SourceLocation(int line, int column) {
        if (line <= 0) {
            throw new IllegalArgumentException("line must be greater than 0");
        }
        if (column <= 0) {
            throw new IllegalArgumentException("column must be greater than 0");
        }
        this.line = line;
        this.column = column;
    }

    /**
        Returns the line number.

        @return The line number.
    */
    public int getLine() {
        return line;
    }

    /**
        Returns the column number.

        @return The column number.
    */
    public int getColumn() {
        return column;
    }

    /**
        Returns a new string representation of this location that is easy for
        a person to read.

        <p>This method returns a string equal to the value of:</p>
        <pre>Lnn:mm</pre>
        <p>where {@code mm} and {@code nn} are positive integers,
        {@code nn} represents the line number,
        and {@code mm} represents the column number.</p>

        @return The new string representation of this token.
    */
    @Override
    public String toString() {
        return "L" + line + ":" + column;
    }
}
