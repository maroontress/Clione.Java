package com.maroontress.clione;

/**
    The representation of the contiguous range of the source file.

    <p>Note that the {@link SourceSpan} instance is an immutable object.</p>
*/
public final class SourceSpan {

    private final SourceLocation start;
    private final SourceLocation end;

    /**
        Creates a new instance.

        @param start The start location of the range.
        @param end The end location of the range.
    */
    public SourceSpan(SourceLocation start, SourceLocation end) {
        this.start = start;
        this.end = end;
    }

    /**
        Creates a new instance representing the single character.

        @param where The start and end location of the range.
    */
    public SourceSpan(SourceLocation where) {
        this(where, where);
    }

    /**
        Creates a new instance representing the range between the specified
        {@link SourceSpan}s (that includes both of them).

        @param first The start span.
        @param last The end span.
    */
    public SourceSpan(SourceSpan first, SourceSpan last) {
        this(first.start, last.end);
    }

    /**
        Returns the start location of this range.

        @return The start location.
    */
    public SourceLocation getStart() {
        return start;
    }

    /**
        Returns the end location of this range.

        @return The end location.
    */
    public SourceLocation getEnd() {
        return end;
    }

    /**
        Returns a new string representation of this range that is easy for
        a person to read.

        <p>This method returns a string equal to the value of either:</p>
        <ul>
        <li>{@code Ln1:xx--Ln2:xx}</li>
        <li>{@code Ln1:m1--m2}</li>
        <li>{@code Ln1:m1}</li>
        </ul>
        <p>where {@code n1}, {@code n2}, {@code m1}, {@code m2}, and {@code xx}
        are positive integers, {@code n1} &lt; {@code n2}, {@code m1} &lt;
        {@code m2}, {@code n1} and {@code n2} represent the line number,
        and {@code m1}, {@code m2}, and {@code xx} represent the column
        number.</p>

        @return The new string representation of this token.
    */
    @Override
    public String toString() {
        var startLine = start.getLine();
        var endLine = end.getLine();
        if (startLine != endLine) {
            return "L" + startLine
                    + ":" + start.getColumn()
                    + "--L" + endLine
                    + ":" + end.getColumn();
        }
        var startColumn = start.getColumn();
        var endColumn = end.getColumn();
        return (startColumn == endColumn)
                ? "L" + startLine
                    + ":" + startColumn
                : "L" + startLine
                    + ":" + startColumn
                    + "--" + endColumn;
    }
}
