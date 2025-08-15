package com.maroontress.clione;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.maroontress.clione.impl.DefaultLexicalParser;

/**
    The lexical parser.

    <p>The {@link LexicalParser} object creates and returns a token from the
    stream of the source file. It often extracts the ones from the source file,
    but trigraph and digraph substitution and line concatenation may result in
    tokens that are not in the source file. It returns an empty token when it
    finally reaches the end of the source file.</p>

    <p>The {@link Token} objects that the {@link #next()} method returns are
    the preprocessing tokens. So, the evaluation is necessary before using
    their content. In other words, they can be incomplete according to the
    token type. For example, the string literal or comment may not terminate,
    the preprocessing number may not represent valid integer and floating-point
    constants, and so on.</p>
*/
public interface LexicalParser extends AutoCloseable {

    /** {@inheritDoc} */
    @Override
    void close() throws IOException;

    /**
        Returns the character representing EOF.

        <p>Note that there is no need for this method in most cases.
        If you want to detect when the line concatenation (a backslash
        followed by a newline character) is immediately followed by EOF,
        you can do so as follows:</p>
        <pre>
        void parse(LexicalParser parser) {
            for (;;) {
                var maybeToken = parser.next();
                if (maybeToken.isEmpty()) {
                    var maybeEof = parser.getEof();
                    assert(maybeEof.isPresent());
                    var eof = maybeEof.get();
                    assert(eof.isEof());
                    var list = eof.getChildren();
                    if (list.size() > 0) {
                        var m = "backslash-newline at end of file";
                        System.err.println(eof.getSpan() + ": warning: " + m);
                    }
                    break;
                }
                ...
            }
        }</pre>

        @return The character representing EOF. Or {@link Optional#empty()}
            if this parser has not yet reached EOF.
        @throws IOException If an I/O error occurs.
    */
    Optional<SourceChar> getEof() throws IOException;

    /**
        Returns the current location of the source file.

        @return The current location.
    */
    SourceLocation getLocation();

    /**
        Returns the next token.

        @return The next token. Or {@link Optional#empty()} if this parser
            reaches EOF.
        @throws IOException If an I/O error occurs.
    */
    Optional<Token> next() throws IOException;

    /**
        Returns the unmodifiable {@link Set} containing the reserved words
        that this parser uses.

        @return The unmodifiable {@link Set} containing the reserved words.
    */
    Set<String> getReservedWords();

    /**
        Returns a new {@link LexicalParser} object.

        <p>The instance considers {@link Keywords#C11} as reserved
        keywords.</p>

        @param reader The reader that provides the stream of the source file.
        @return The new {@link LexicalParser} object.
    */
    static LexicalParser of(Reader reader) {
        return new DefaultLexicalParser(reader);
    }

    /**
        Returns a new {@link LexicalParser} object with the specified reserved
        words.

        @param reader The reader that provides the stream of the source file.
        @param reservedWords The collection that contains reserved words.
            Note that the constructor copies the collection, so changes to the
            collection do not affect this instance.
        @return The new {@link LexicalParser} object.
    */
    static LexicalParser of(Reader reader, Collection<String> reservedWords) {
        return new DefaultLexicalParser(reader, reservedWords);
    }
}
