package com.maroontress.clione.impl;

import com.maroontress.clione.TokenType;

/**
    Provides the facility of digraph substitution.

    <p>In general, digraphs are sequences of two characters that appear in
    source code and should be treated as if they were single characters. In
    the C programming language, any digraph must always represent a full token
    by itself. The following table lists all valid operator or punctuator
    tokens represented with digraphs:</p>

    <table border="1" style="border-collapse: collapse;">
    <caption>Tokens represented with digraphs.</caption>
    <tbody>
    <tr><th>Token</th><th>Equivalent</th></tr>
    <tr><td><code>&lt;:</code></td><td><code>[</code></td></tr>
    <tr><td><code>:&gt;</code></td><td><code>]</code></td></tr>
    <tr><td><code>&lt;%</code></td><td><code>{</code></td></tr>
    <tr><td><code>%&gt;</code></td><td><code>}</code></td></tr>
    <tr><td><code>%:</code></td><td><code>#</code></td></tr>
    <tr><td><code>%:%:</code></td><td><code>##</code></td></tr>
    </tbody>
    </table>

    <p>Note that both {@code %:#} and {@code #%:} are not equivalent to
    token {@code ##} or {@code %:%:}.</p>

    @see <a href="https://en.wikipedia.org/wiki/Digraphs_and_trigraphs#C">
    Wikipedia, Digraphs and trigraphs</a>
*/
public final class Digraphs {

    /** Prevents the class from being instantiated. */
    private Digraphs() {
        throw new AssertionError();
    }

    /**
        Substitutes a number sign (with which the preprocessing directive
        starts) for the character sequence that the specified transcriber
        stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#DIRECTIVE}).
    */
    public static TokenType toDirective(Transcriber x) {
        x.getBuilder().replaceDigraph('#');
        return TokenType.DIRECTIVE;
    }

    /**
        Substitutes a double number sign (that appears other than in the macro
        declaration) for the character sequence that the specified transcriber
        stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#UNKNOWN}).
    */
    public static TokenType toUnknownDoubleNumberSign(Transcriber x) {
        x.getBuilder().replaceDigraph('#', '#');
        return TokenType.UNKNOWN;
    }

    /**
        Substitutes a number sign (a preprocessing stringification operator
        {@code #}) for the character sequence that the specified transcriber
        stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#OPERATOR}).
    */
    public static TokenType toStringificationOperator(Transcriber x) {
        x.getBuilder().replaceDigraph('#');
        return TokenType.OPERATOR;
    }

    /**
        Substitutes a double number sign (a preprocessing token-pasting
        operator {@code ##}) for the character sequence that the specified
        transcriber stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#OPERATOR}).
    */
    public static TokenType toTokenPastingOperator(Transcriber x) {
        x.getBuilder().replaceDigraph('#', '#');
        return TokenType.OPERATOR;
    }

    /**
        Substitutes a right bracket for the character sequence that the
        specified transcriber stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#PUNCTUATOR}).
    */
    public static TokenType toRightBracket(Transcriber x) {
        return toPunctuator(x, ']');
    }

    /**
        Substitutes a left bracket for the character sequence that the
        specified transcriber stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#PUNCTUATOR}).
    */
    public static TokenType toLeftBracket(Transcriber x) {
        return toPunctuator(x, '[');
    }

    /**
        Substitutes a left brace for the character sequence that the
        specified transcriber stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#PUNCTUATOR}).
    */
    public static TokenType toLeftBrace(Transcriber x) {
        return toPunctuator(x, '{');
    }

    /**
        Substitutes a right brace for the character sequence that the
        specified transcriber stores in its builder.

        @param x The transcriber.
        @return The token type ({@link TokenType#PUNCTUATOR}).
    */
    public static TokenType toRightBrace(Transcriber x) {
        return toPunctuator(x, '}');
    }

    private static TokenType toPunctuator(Transcriber x, char c) {
        x.getBuilder().replaceDigraph(c);
        return TokenType.PUNCTUATOR;
    }
}
