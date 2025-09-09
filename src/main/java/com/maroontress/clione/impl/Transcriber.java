package com.maroontress.clione.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.impl.Case.Mapper;

/**
    Transcribes a token read from the {@code Source} object into the
    {@code TokenBuilder} object.
*/
public final class Transcriber {

    private static final Map<Character, Integer> UCN_MAP = Map.of(
            'u', 4,
            'U', 8);

    private static final Set<Character> EXP_CHAR_SET = Set.of(
            'E', 'e', 'P', 'p');

    private static final Set<Character> SIGN_CHAR_SET = Set.of('+', '-');

    private final Source source;
    private final TokenBuilder builder;

    /**
        Creates a new instance.

        @param source The source that provides the stream of the source file.
    */
    public Transcriber(Source source) {
        this.source = source;
        this.builder = new TokenBuilder();
    }

    /**
        Returns the source.

        @return The source.
    */
    public Source getSource() {
        return source;
    }

    /**
        Returns the token builder.

        @return The token builder.
    */
    public TokenBuilder getBuilder() {
        return builder;
    }

    /**
        Returns a new token with the specified token type.

        <p>Invocation of this method is equivalent to:</p>
        <pre>getBuilder().toToken(type)</pre>

        @param type The token type.
        @return The new token.
    */
    public Token toToken(TokenType type) {
        return builder.toToken(type);
    }

    /**
        Reads a comment from the source.

        <p>The token builder must have stored the slash and asterisk.</p>

        <p>This method will return when it reads the end of the comment
        (asterisk and slash) or reaches EOF.</p>

        @throws IOException If an I/O error occurs.
    */
    public void readComment() throws IOException {
        var s = source;
        var b = builder;
        for (;;) {
            var i = s.getChar();
            if (i.isEof()) {
                return;
            }
            b.append(i);
            var c = i.toChar();
            if (c != '*') {
                continue;
            }
            var next = s.getChar();
            if (next.isEof()) {
                return;
            }
            var n = next.toChar();
            if (n == '/') {
                b.append(next);
                return;
            }
            s.ungetChar(next);
        }
    }

    /**
        Reads characters from the source until just before a newline
        character.

        <p>This method will return when it reads characters up to just before
        the newline character or reaches EOF.</p>

        @throws IOException If an I/O error occurs.
    */
    public void readSingleLine() throws IOException {
        var s = source;
        var b = builder;
        for (;;) {
            var i = s.getChar();
            if (i.isEof()) {
                return;
            }
            var c = i.toChar();
            if (c == '\n') {
                s.ungetChar(i);
                return;
            }
            b.append(i);
        }
    }

    /**
        Reads characters from the source up to the specified terminator
        character (including the terminator character).

        <p>This method will return when it reads the specified terminator
        character or reaches EOF.</p>

        @param terminator The character that terminates the filename.
        @throws IOException If an I/O error occurs.
    */
    public void readFilename(char terminator) throws IOException {
        var s = source;
        var b = builder;
        for (;;) {
            var i = s.getChar();
            if (i.isEof()) {
                return;
            }
            var c = i.toChar();
            if (c == '\n') {
                s.ungetChar(i);
                return;
            }
            b.append(i);
            if (c == terminator) {
                return;
            }
        }
    }

    /**
        Reads characters from the source up tp the specified terminator
        character (including the terminator character).

        <p>The token builder must have stored either single- or double-quote
        character.</p>

        <p>This method takes escape sequences into account.</p>

        <p>This method will return when it reads the specified terminator
        character (except within an escape sequence) or reaches EOF.</p>

        <p>When this method reaches a newline character, it will return
        without reading it.</p>

        @param terminator The terminator character.
        @throws IOException If an I/O error occurs.
    */
    public void readStringOrCharacter(char terminator) throws IOException {
        var s = source;
        var b = builder;
        for (;;) {
            var i = s.getChar();
            if (i.isEof()) {
                return;
            }
            var c = i.toChar();
            if (c == '\n') {
                s.ungetChar(i);
                return;
            }
            b.append(i);
            if (c == terminator) {
                return;
            }
            if (c == '\\') {
                readEscapeSequence();
            }
        }
    }

    private void readEscapeSequence() throws IOException {
        var i = source.getChar();
        if (i.isEof()) {
            return;
        }
        builder.append(i);
        var c = i.toChar();
        if (Chars.isOctalDigit(c)) {
            readMax(2, Chars::isOctalDigit);
            return;
        }
        if (c == 'x') {
            readZeroOrMoreChars(Chars::isHexDigit);
            return;
        }
        if (c == 'u') {
            readMax(4, Chars::isHexDigit);
            return;
        }
        if (c == 'U') {
            readMax(8, Chars::isHexDigit);
            return;
        }
        assert c != '\n';
    }

    /**
        Reads at most the specified number of characters while the specified
        predicate with the character returns {@code true}.

        <p>This method will return when it reaches EOF.</p>

        @param max The maximum number of characters.
        @param accepts The predicate that returns {@code true} if the specified
            character is accepted.
        @return The number of characters actually read.
        @throws IOException If an I/O error occurs.
    */
    public int readMax(int max, Predicate<Character> accepts)
            throws IOException {
        var k = 0;
        for (; k < max && readZeroOrOneChar(accepts) != null; ++k) {
            continue;
        }
        return k;
    }

    /**
        Reads an identifier.

        <p>The token builder must have stored the first character of an
        identifier. It may also have stored the second and subsequent
        characters of an identifier.</p>

        <p>The second and subsequent character of an identifier must be
        either:</p>
        <ul>
        <li>An underscore character, an uppercase or lowercase letter, or a
        digit ({@code [_A-Za-z0-9]})</li>
        <li>Universal character names ({@code \}{@code uXXXX} or
        {@code \}{@code UXXXXXXXX}, {@code X} is a hexadecimal digit)</li>
        <li>Other implementation-defined characters</li>
        </ul>

        <p>The <i>other implementation-defined characters</i> are as
        follows:</p>
        <ul>
        <li>The first character: a character with which the
        {@link Character#isUnicodeIdentifierStart(int)} method returns
        {@code true}</li>
        <li>The second and subsequent character: a character with which the
        {@link Character#isUnicodeIdentifierPart(int)}
        method returns {@code true}</li>
        </ul>

        <p>This method will return when it reaches EOF.</p>

        <p>When this method reaches a character that is not an identifier,
        it will return without reading it.</p>

        @throws IOException If an I/O error occurs.
    */
    public void readIdentifier() throws IOException {
        var s = source;
        var b = builder;
        for (;;) {
            var first = s.getChar();
            if (first.isEof()) {
                return;
            }
            var c = first.toChar();
            if (Chars.isName(c)
                || Character.isUnicodeIdentifierPart(c)) {
                b.append(first);
                continue;
            }
            if (c == '\\') {
                if (!tryReadUcn(first)) {
                    return;
                }
                continue;
            }
            if (Character.isHighSurrogate(c)) {
                if (!tryReadSurrogatePair(first)) {
                    return;
                }
                continue;
            }
            s.ungetChar(first);
            return;
        }
    }

    /**
        Reads zero or more characters while the specified predicate with the
        character returns {@code true}.

        <p>This method will return when it reaches EOF.</p>

        @param accepts The predicate that returns {@code true} if the specified
            character is accepted.
        @throws IOException If an I/O error occurs.
    */
    public void readZeroOrMoreChars(Predicate<Character> accepts)
            throws IOException {
        var s = source;
        var b = builder;
        for (;;) {
            var i = s.getChar();
            if (i.isEof()) {
                return;
            }
            var c = i.toChar();
            if (!accepts.test(c)) {
                s.ungetChar(i);
                return;
            }
            b.append(i);
        }
    }

    /**
        Reads at most one character with which the specified predicate
        returns {@code true}.

        <p>This method will return when it reaches EOF.</p>

        @param accepts The predicate that returns {@code true} if the specified
            character is accepted.
        @return The character to have read, or {@code null} if no character
            has been read.
        @throws IOException If an I/O error occurs.
    */
    public SourceChar readZeroOrOneChar(Predicate<Character> accepts)
            throws IOException {
        var s = source;
        var b = builder;
        var i = s.getChar();
        if (i.isEof()) {
            return null;
        }
        var c = i.toChar();
        if (accepts.test(c)) {
            b.append(i);
            return i;
        }
        s.ungetChar(i);
        return null;
    }

    /**
        Reads a preprocessing number.

        <p>The token builder must have stored the first character of a
        preprocessing number. It may also have stored the second and subsequent
        characters of a preprocessing number.</p>

        <p>This method will return when it reaches EOF.</p>

        <p>When this method reaches a character that is not a preprocessing
        number, it will return without reading it.</p>

        @throws IOException If an I/O error occurs.
    */
    public void readNumber() throws IOException {
        var s = source;
        var b = builder;
        var last = b.getLast();
        for (;;) {
            var i = s.getChar();
            if (i.isEof()) {
                return;
            }
            var c = i.toChar();
            if (Chars.isPreprocessingNumber(c)) {
                last = i;
                b.append(i);
                continue;
            }
            var prev = last.toChar();
            if (EXP_CHAR_SET.contains(prev)
                    && SIGN_CHAR_SET.contains(c)) {
                last = i;
                b.append(i);
                continue;
            }
            s.ungetChar(i);
            return;
        }
    }

    /**
        Reads characters according to the specified mapper.

        <p>This method reads the character that the mapper maps to the
        {@link Tokenizer} object and then invokes the tokenizer with
        {@code this} transcriber.</p>

        <p>When this method reaches a character and the mapper does not map
        the character or reaches EOF, it returns the specified token type
        {@code otherwise} without reading any character.</p>

        @param mapper The mapper.
        @param otherwise The token type that this method returns either
            if it reaches a character that {@code mapper} does not map,
            or if it reaches EOF.
        @return The token type that the tokenizer returns if the {@code mapper}
            maps the character to the tokenizer. Otherwise, {@code otherwise}.
        @throws IOException If an I/O error occurs.
    */
    public TokenType tryReadToken(Mapper mapper, TokenType otherwise)
            throws IOException {
        var s = source;
        var b = builder;
        var i = s.getChar();
        if (i.isEof()) {
            return otherwise;
        }
        var c = i.toChar();
        var a = mapper.get(c);
        if (a == null) {
            s.ungetChar(i);
            return otherwise;
        }
        b.append(i);
        return a.apply(this);
    }

    /**
        Reads characters according to the specified mapper.

        <p>This method reads the character that the mapper maps to the
        {@link Tokenizer} object and then invokes the tokenizer with
        {@code this} transcriber.</p>

        <p>When this method reaches a character and the mapper does not map
        the character or reaches EOF, it invokes the tokenizer
        {@code otherwise} without reading any character.</p>

        @param mapper The mapper.
        @param otherwise The tokenizer that this method invokes either
            if it reaches a character that {@code mapper} does not map,
            or if it reaches EOF.
        @return The token type that the tokenizer returns if the {@code mapper}
            maps the character to the tokenizer. Otherwise, the token type
            that the tokenizer {@code otherwise} returns.
        @throws IOException If an I/O error occurs.
    */
    public TokenType tryReadToken(Mapper mapper, Tokenizer otherwise)
            throws IOException {
        var s = source;
        var b = builder;
        var i = s.getChar();
        if (i.isEof()) {
            return otherwise.apply(this);
        }
        var c = i.toChar();
        var a = mapper.get(c);
        if (a == null) {
            s.ungetChar(i);
            return otherwise.apply(this);
        }
        b.append(i);
        return a.apply(this);
    }

    /**
        Reads a token from the source in the default context.

        @return The token type of the token to have read.
        @throws IOException If an I/O error occurs.
    */
    public TokenType readToken() throws IOException {
        return readTokenOtherwise(Switches.DEFAULT, Transcriber::readSymbol);
    }

    /**
        Reads a token from the source in the context of a preprocessing
        directive.

        @return The token type of the token to have read.
        @throws IOException If an I/O error occurs.
    */
    public TokenType readDirectiveToken() throws IOException {
        return readTokenOtherwise(Switches.DIRECTIVE, Transcriber::readSymbol);
    }

    /**
        Reads a token from the source in the context of a preprocessing
        {@code #include} directive.

        @return The token type of the token to have read.
        @throws IOException If an I/O error occurs.
    */
    public TokenType readIncludeDirectiveToken() throws IOException {
        return readTokenOtherwise(Switches.INCLUDE_DIRECTIVE,
                Transcriber::readSymbol);
    }

    /**
        Reads a token from the source in the context of a preprocessing
        {@code #line} directive.

        @return The token type of the token to have read.
        @throws IOException If an I/O error occurs.
    */
    public TokenType readLineDirectiveToken() throws IOException {
        return readTokenOtherwise(Switches.LINE_DIRECTIVE,
                Transcriber::readSymbol);
    }

    /**
        Reads characters according to the specified mapper.

        <p>This method reads the character that the mapper maps to the
        {@link Tokenizer} object and then invokes the tokenizer with
        {@code this} transcriber.</p>

        <p>When this method reads a character and the mapper does not map
        the character, it invokes the default tokenizer {@code otherwise}
        with it.</p>

        <p>When this method reaches EOF, it returns {@code null} without
        reading any character.</p>

        @param mapper The mapper.
        @param otherwise The default tokenizer that this method invokes
            if it has read a character that {@code mapper} does not map.
        @return The token type that the tokenizer returns if the {@code mapper}
            maps the character to the tokenizer. The token type
            that the default tokenizer {@code otherwise} returns if the
            {@code mapper} does not map the character.
            {@code null} if this method reaches EOF.
        @throws IOException If an I/O error occurs.
    */
    private TokenType readTokenOtherwise(
            Mapper mapper, DefaultTokenizer otherwise) throws IOException {
        var s = source;
        var b = builder;
        var i = s.getChar();
        if (i.isEof()) {
            return null;
        }
        var c = i.toChar();
        var a = mapper.get(c);
        if (a == null) {
            return otherwise.apply(this, i);
        }
        b.append(i);
        return a.apply(this);
    }

    private static TokenType readSymbol(Transcriber x, SourceChar i)
            throws IOException {
        var s = x.getSource();
        var b = x.getBuilder();
        var c = i.toChar();
        if (Character.isHighSurrogate(c)) {
            var j = s.getChar();
            if (j.isEof()) {
                b.append(i);
                return TokenType.UNKNOWN;
            }
            var n = j.toChar();
            if (!Character.isLowSurrogate(n)) {
                s.ungetChar(j);
                b.append(i);
                return TokenType.UNKNOWN;
            }
            b.append(i);
            b.append(j);
            var u = Character.toCodePoint(c, n);
            if (!Character.isUnicodeIdentifierStart(u)) {
                return TokenType.UNKNOWN;
            }
            x.readIdentifier();
            return TokenType.IDENTIFIER;
        }
        if (Character.isUnicodeIdentifierStart(c)) {
            b.append(i);
            x.readIdentifier();
            return TokenType.IDENTIFIER;
        }
        b.append(i);
        return TokenType.UNKNOWN;
    }

    private boolean tryReadUcn(SourceChar first) throws IOException {
        var s = source;
        var second = s.getChar();
        if (second.isEof()) {
            s.ungetChar(first);
            return false;
        }
        var u = second.toChar();
        var count = UCN_MAP.get(u);
        if (count == null) {
            s.ungetChar(second);
            s.ungetChar(first);
            return false;
        }
        var b = builder;
        b.append(first);
        b.append(second);
        var n = readMax(count, Chars::isHexDigit);
        if (n < count) {
            rollback(n + 2);
            return false;
        }
        return true;
    }

    private boolean tryReadSurrogatePair(SourceChar first) throws IOException {
        var s = source;
        var c = first.toChar();
        var second = s.getChar();
        if (second.isEof()) {
            s.ungetChar(first);
            return false;
        }
        var n = second.toChar();
        if (!Character.isLowSurrogate(n)
                || !Character.isUnicodeIdentifierPart(
                Character.toCodePoint(c, n))) {
            s.ungetChar(first);
            s.ungetChar(second);
            return false;
        }
        var b = builder;
        b.append(first);
        b.append(second);
        return true;
    }

    private void rollback(int m) {
        var s = source;
        var b = builder;
        for (var k = 0; k < m; ++k) {
            s.ungetChar(b.removeLast());
        }
    }
}
