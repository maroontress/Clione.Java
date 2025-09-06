package com.maroontress.clione;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.maroontress.clione.impl.DirectiveParseKit;
import com.maroontress.clione.impl.ReaderSource;
import com.maroontress.clione.impl.ReparseSource;
import com.maroontress.clione.impl.SourceChars;
import com.maroontress.clione.impl.TokenBuilder;
import com.maroontress.clione.impl.Transcriber;

/**
    The utility class for operations on token sequence.
*/
public final class Tokens {

    private Tokens() {
    }

    /**
        Reparses the given tokens as an include directive.

        @param tokens The tokens.
        @param filename The filename.
        @param reservedWords The set of the reserved words.
        @return The list of the new tokens.
    */
    public static List<Token> reparseIncludeFilename(
            Collection<Token> tokens,
            String filename,
            Set<String> reservedWords) {
        var charList = tokens.stream()
                .flatMap(t -> t.getChars().stream())
                .toList();
        var source = new ReparseSource(charList, filename);
        var list = new ArrayList<Token>();
        var kit = new DirectiveParseKit(source, reservedWords);
        try {
            kit.addIncludeDirectiveTokens(list);
            return list;
        } catch (IOException e) {
            // This should not happen with ReparseSource.
            return null;
        }
    }

    /**
        Converts the given tokens into a string literal token.

        <p>This method implements the behavior of the C preprocessor
        stringizing operation with the following precise rules:</p>

        <ul>
        <li>Tokens whose type is {@link TokenType#DELIMITER} or
            {@link TokenType#COMMENT} are treated as whitespace for the
            purpose of stringizing.</li>
        <li>All leading and trailing whitespace tokens are removed.</li>
        <li>Any sequence of one or more whitespace tokens between
            non-whitespace tokens is collapsed to a single ASCII space
            (U+0020).</li>
        <li>Whitespace that belongs to embedded string-literal tokens (i.e.,
            tokens that are not whitespace) is preserved and is not collapsed.
            </li>
        </ul>

        <p>After the above trimming/collapsing, the remaining token texts are
        concatenated in token order. The concatenated text is then escaped:
        every backslash ('\') and double quote ('"') in the text is prefixed
        with a backslash, and the whole result is enclosed in double quotes to
        produce a C-style string literal.</p>

        <p>Newly created characters of the produced string literal token is
        assigned the provided {@link SourceLocation} (the same location is
        used for the characters).</p>

        @param tokens The immutable list of tokens to be stringized.
        @param where The source location to assign to newly created characters
            of the resulting string literal token.
        @return A new {@link Token} of type {@link TokenType#STRING} that
            represents the stringized form of the input tokens.
    */
    public static Token stringize(List<Token> tokens, SourceLocation where) {
        /*
            https://cppreference.com/w/c/preprocessor/replace.html

            > All leading and trailing whitespace is removed, and any sequence
            > of whitespace in the middle of the text (but not inside embedded
            > string literals) is collapsed to a single space.
        */
        var queue = new ArrayDeque<>(tokens);
        trimLeading(queue);
        trimTrailing(queue);

        var doubleQuote = newSourceChar('"', where);
        var singleSpace = newSourceChar(' ', where);
        var backslash = newSourceChar('\\', where);
        var builder = new TokenBuilder();
        builder.append(doubleQuote);
        for (;;) {
            if (queue.isEmpty()) {
                break;
            }
            var token = queue.removeFirst();
            if (isDelimiterOrComment(token)) {
                trimLeading(queue);
                builder.append(singleSpace);
                continue;
            }
            token.getChars().forEach(c -> {
                var raw = c.toChar();
                if (raw == '\\' || raw == '"') {
                    builder.append(backslash);
                }
                builder.append(c);
            });
        }
        builder.append(doubleQuote);
        return builder.toToken(TokenType.STRING);
    }

    /**
        Concatenates the two tokens and returns a newly created token.

        <p>The characters of {@code left} and {@code right} are appended in
        that order to form a single token text. The resulting token's type is
        determined as follows:</p>

        <ol>
        <li>If the concatenated token text is equal to one of the strings in
            {@code reservedWords}, the resulting token type is {@link
            TokenType#RESERVED}.</li>
        <li>Otherwise, the method attempts to parse the concatenated text as a
            single token. If parsing succeeds and yields exactly one token,
            the resulting token type is set to that parsed type.</li>
        <li>If parsing produces more than one token, or parsing fails, the
            resulting token type is {@link TokenType#UNKNOWN}.</li>
        </ol>

        @param left The left token.
        @param right The right token.
        @param reservedWords A set of the reserved words.
        @return The new token.
    */
    public static Token concatenate(Token left, Token right,
            Set<String> reservedWords) {
        var builder = new TokenBuilder();
        Stream.of(left, right)
                .flatMap(t -> t.getChars().stream())
                .forEach(builder::append);
        var tokenString = builder.toTokenString();
        // Note: tokenString must not be empty.
        if (reservedWords.contains(tokenString)) {
            return builder.toToken(TokenType.RESERVED);
        }
        return getTokenType(tokenString)
                .map(builder::toToken)
                .orElseGet(() -> builder.toToken(TokenType.UNKNOWN));
    }

    private static Optional<TokenType> getTokenType(String tokenString) {
        var source = new ReaderSource(new StringReader(tokenString), null);
        var x = new Transcriber(source);
        try {
            var type = x.readToken();
            /*
                TokenBuilder#toTokenString() never returns an empty string,
                so the null-check below is unnecessary:

                    if (type == null) {
                        return Optional.empty();
                    }

                We therefore assume 'type' is non-null here.
            */
            var nextType = x.readToken();
            if (nextType != null) {
                // This means the concatenated string represents more than one
                // tokens.
                return Optional.empty();
            }
            return Optional.of(type);
        } catch (IOException e) {
            // This should not happen with StringReader.
            return Optional.empty();
        }
    }

    private static void trimLeading(Deque<Token> queue) {
        while (!queue.isEmpty() && isDelimiterOrComment(queue.peekFirst())) {
            queue.removeFirst();
        }
    }

    private static void trimTrailing(Deque<Token> queue) {
        while (!queue.isEmpty() && isDelimiterOrComment(queue.peekLast())) {
            queue.removeLast();
        }
    }

    private static SourceChar newSourceChar(char c, SourceLocation location) {
        return SourceChars.of(c, location.getColumn(), location.getLine());
    }

    /**
        Checks whether the given token is a delimiter or a comment.

        @param token The token to be checked.
        @return {@code true} if the token is a delimiter or a comment,
            otherwise {@code false}.
    */
    public static boolean isDelimiterOrComment(Token token) {
        var type = token.getType();
        return type == TokenType.DELIMITER
                || type == TokenType.COMMENT;
    }

    /**
        Normalizes the given token.

        <p>If the token is an identifier and its value is in the given
        set of the reserved words, this method returns a new token that
        is the same as the given token except that its type is
        {@code TokenType.RESERVED}.</p>

        <p>Otherwise, this method returns the given token with no
        modifications.</p>

        @param token The token.
        @param reservedWords The set of the reserved words.
        @return The new token.
    */
    public static Token normalizeToken(Token token, Set<String> reservedWords) {
        return (token.isType(TokenType.IDENTIFIER)
                && reservedWords.contains(token.getValue()))
            ? token.withType(TokenType.RESERVED)
            : token;
    }
}
