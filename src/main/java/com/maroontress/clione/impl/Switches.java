package com.maroontress.clione.impl;

import java.util.Set;
import com.maroontress.clione.impl.Case.Mapper;
import com.maroontress.clione.TokenType;

/**
    Provides mappers that associate a character with a tokenizer.
*/
public final class Switches {

    /** The default mapper. */
    public static final Mapper DEFAULT = Case.newMapper(
            // ' ', '\t', or '\n'
            Cases.DELIMITER,
            // u
            LowerU.CASE,
            // [LU]
            UpperLOrU.CASE,
            // "
            Cases.STRING_LITERAL,
            // '
            Cases.CHARACTER_CONSTANT,
            // /
            Slash.CASE,
            // [_A-Za-z] (except LUu so that the order is important)
            Cases.IDENTIFIER,
            // '\\'
            Backslash.CASE,
            // [0-9]
            Cases.NUMBER,
            // .
            Dot.CASE,
            // #
            Sharp.CASE,
            // %
            Percent.CASE,
            // :
            Colon.CASE,
            // -
            Cases.MINUS,
            // ?
            Cases.QUESTION,
            // [](){},;
            Cases.PUNCTUATOR,
            // >
            Cases.GREATER_THAN,
            // >
            Cases.LESS_THAN,
            // *^!~
            Cases.OPERATOR_FOLLOWED_BY_EQUAL,
            // +
            Cases.PLUS,
            // &
            Cases.AND,
            // |
            Cases.OR);

    /** The mapper used inside preprocessing directives. */
    public static final Mapper DIRECTIVE = Case.newMapper(
            // '\n'
            Cases.DIRECTIVE_END,
            // ' ' or '\t'
            Cases.DIRECTIVE_DELIMITER,
            // u
            LowerU.CASE,
            // [LU]
            UpperLOrU.CASE,
            // "
            Cases.STRING_LITERAL,
            // '
            Cases.CHARACTER_CONSTANT,
            // /
            Slash.CASE,
            // [_A-Za-z] (except LUu so that the order is important)
            Cases.IDENTIFIER,
            // '\\'
            Backslash.CASE,
            // [0-9]
            Cases.NUMBER,
            // .
            Dot.CASE,
            // #
            SharpInsideDirective.CASE,
            // %
            PercentInsideDirective.CASE,
            // :
            Colon.CASE,
            // -
            Cases.MINUS,
            // ?
            Cases.QUESTION,
            // [](){},;
            Cases.PUNCTUATOR,
            // >
            Cases.GREATER_THAN,
            // >
            Cases.LESS_THAN,
            // *^!~
            Cases.OPERATOR_FOLLOWED_BY_EQUAL,
            // +
            Cases.PLUS,
            // &
            Cases.AND,
            // |
            Cases.OR);

    /** The mapper used inside preprocessing {@code include} directives. */
    public static final Mapper INCLUDE_DIRECTIVE = Case.newMapper(
            // '\n'
            Cases.DIRECTIVE_END,
            // ' ' or '\t'
            Cases.DIRECTIVE_DELIMITER,
            // u
            LowerU.CASE,
            // [LU]
            UpperLOrU.CASE,
            // "
            Cases.FILENAME,
            // <
            Cases.STANDARD_HEADER,
            // '
            Cases.CHARACTER_CONSTANT,
            // /
            Slash.CASE,
            // [_A-Za-z] (except LUu so that the order is important)
            Cases.IDENTIFIER,
            // '\\'
            Backslash.CASE,
            // [0-9]
            Cases.NUMBER,
            // .
            Dot.CASE,
            // #
            SharpInsideDirective.CASE,
            // %
            PercentInsideDirective.CASE,
            // :
            Colon.CASE,
            // -
            Cases.MINUS,
            // ?
            Cases.QUESTION,
            // [](){},;
            Cases.PUNCTUATOR,
            // >
            Cases.GREATER_THAN,
            // >
            Cases.LESS_THAN,
            // *^!~
            Cases.OPERATOR_FOLLOWED_BY_EQUAL,
            // +
            Cases.PLUS,
            // &
            Cases.AND,
            // |
            Cases.OR);

    /** Prevents the class from being instantiated. */
    private Switches() {
        throw new AssertionError();
    }

    private static Tokenizer newLessOrGreaterThanTokenizer(char first) {
        return x -> {
            x.readZeroOrOneChar(c -> c == first);
            x.readZeroOrOneChar(c -> c == '=');
            return TokenType.OPERATOR;
        };
    }

    private static Case newFollowingSelfOrEqualCase(char first) {
        return Case.of(first, x -> {
            x.readZeroOrOneChar(c -> c == first || c == '=');
            return TokenType.OPERATOR;
        });
    }

    private static Tokenizer newUniversalCharacterNameCase(int m) {
        return x -> {
            var n = x.readMax(m, Chars::isHexDigit);
            if (n < m) {
                return TokenType.UNKNOWN;
            }
            x.readIdentifier();
            return TokenType.IDENTIFIER;
        };
    }

    private static class Colon {
        public static final Case CASE = Case.of(
                ':', TokenType.PUNCTUATOR,
                // >
                Case.of('>', Digraphs::toRightBracket));
    }

    private static class Sharp {
        public static final Case CASE = Case.of(
                '#', TokenType.DIRECTIVE,
                // #
                Case.of('#', x -> TokenType.UNKNOWN));
    }

    private static class Percent {
        public static final Case CASE = Case.of(
                '%', TokenType.OPERATOR,
                // :
                ColonAfterPercent.CASE,
                // >
                Cases.GREATER_THAN_FOLLOWING_PERCENT,
                // =
                Case.of('=', x -> TokenType.OPERATOR));
    }

    private static class ColonAfterPercent {
        public static final Case CASE = Case.of(
                ':', Digraphs::toDirective,
                // %
                PercentAfterPercentColon.CASE);
    }

    private static class PercentAfterPercentColon {
        public static final Case CASE = Case.of(
                '%', TokenType.UNKNOWN,
                // :
                Case.of(':', Digraphs::toUnknownDoubleNumberSign));
    }

    private static class SharpInsideDirective {
        public static final Case CASE = Case.of(
                '#', TokenType.OPERATOR,
                // #
                Case.of('#', x -> TokenType.OPERATOR));
    }

    private static class PercentInsideDirective {
        public static final Case CASE = Case.of(
                '%', TokenType.OPERATOR,
                // :
                ColonAfterPercentInsideDirective.CASE,
                // >
                Cases.GREATER_THAN_FOLLOWING_PERCENT,
                // =
                Case.of('=', x -> TokenType.OPERATOR));
    }

    private static class ColonAfterPercentInsideDirective {
        public static final Case CASE = Case.of(
                ':', Digraphs::toStringificationOperator,
                // %
                PercentAfterPercentColonInsideDirective.CASE);
    }

    private static class PercentAfterPercentColonInsideDirective {
        public static final Case CASE = Case.of(
                '%', TokenType.UNKNOWN,
                // :
                Case.of(':', Digraphs::toTokenPastingOperator));
    }

    private static class Backslash {
        // Unicode non-digit character
        public static final Case CASE = Case.of(
                '\\', TokenType.UNKNOWN,
                // u
                Cases.LOWER_U_AFTER_BACKSLASH,
                // U
                Cases.UPPER_U_AFTER_BACKSLASH);
    }

    private static class Slash {
        // Comment (/*...*/ //...) or operator (/ /=)
        public static final Case CASE = Case.of(
                '/', TokenType.OPERATOR,
                // *
                Cases.ASTERISK_AFTER_SLASH,
                // /
                Cases.DOUBLE_SLASH,
                // =
                Case.of('=', x -> TokenType.OPERATOR));
    }

    private static class EightAfterLowerU {
        // UTF-8 string literal or identifier
        public static final Case CASE = Case.of(
                '8', TokenType.IDENTIFIER,
                // [_A-Za-z0-9]
                Cases.IDENTIFIER_AFTER_PREFIX,
                // "
                Cases.STRING_LITERAL);
    }

    private static class LowerU {
        // Prefix (u8 u) or identifier
        public static final Case CASE = Case.of(
                'u', TokenType.IDENTIFIER,
                // 8
                EightAfterLowerU.CASE,
                // [_A-Za-z0-9] (except 8)
                Cases.IDENTIFIER_AFTER_PREFIX,
                // "
                Cases.STRING_LITERAL,
                // '
                Cases.CHARACTER_CONSTANT);
    }

    private static class UpperLOrU {
        // Prefix (L U) or identifier
        public static final Case CASE = Case.of(
                Set.of('L', 'U'), TokenType.IDENTIFIER,
                // [_A-Za-z0-9]
                Cases.IDENTIFIER_AFTER_PREFIX,
                // "
                Cases.STRING_LITERAL,
                // '
                Cases.CHARACTER_CONSTANT);
    }

    private static class Dot {
        // Preprocessing number starting with a dot (.[0-9]+) or operator
        public static final Case CASE = Case.of(
                '.', TokenType.OPERATOR,
                // 0-9
                Cases.NUMBER,
                // .
                Cases.DOUBLE_DOT);
    }

    /** Leaf cases. */
    private static class Cases {
        public static final Case GREATER_THAN_FOLLOWING_PERCENT = Case.of(
                '>', Digraphs::toRightBrace);

        // + ++ +=
        public static final Case PLUS = newFollowingSelfOrEqualCase('+');

        // - -- -=
        public static final Case AND = newFollowingSelfOrEqualCase('&');

        // & && &=
        public static final Case OR = newFollowingSelfOrEqualCase('|');

        // X X=
        public static final Case OPERATOR_FOLLOWED_BY_EQUAL = Case.of(
                Set.of('*', '^', '!', '~', '='), x -> {
                    x.readZeroOrOneChar(c -> c == '=');
                    return TokenType.OPERATOR;
                });

        // < << <= <<= <: <%
        public static final Case LESS_THAN = Case.of(
                '<', newLessOrGreaterThanTokenizer('<'),
                // :
                Case.of(':', Digraphs::toLeftBracket),
                // %
                Case.of('%', Digraphs::toLeftBrace));

        // > >> >= >>=
        public static final Case GREATER_THAN = Case.of(
                '>', newLessOrGreaterThanTokenizer('>'));

        // [ ] ( ) { } , ;
        public static final Case PUNCTUATOR = Case.of(
                Set.of('[', ']', '(', ')', '{', '}', ',', ';'),
                TokenType.PUNCTUATOR);

        // ?
        public static final Case QUESTION = Case.of('?', TokenType.OPERATOR);

        // - -> -- -=
        public static final Case MINUS = Case.of(
                '-', TokenType.OPERATOR,
                // > - =
                Case.of(Set.of('>', '-', '='), x -> TokenType.OPERATOR));

        public static final Case DELIMITER = Case.of(
                Chars.DELIMITER_SET, x -> {
                    x.readZeroOrMoreChars(Chars::isDelimiter);
                    return TokenType.DELIMITER;
                });

        public static final Case NUMBER = Case.of(
                Chars.DIGIT_SET, x -> {
                    x.readNumber();
                    return TokenType.NUMBER;
                });

        public static final Case IDENTIFIER = Case.of(
                Chars.FIRST_OF_IDENTIFIER_SET, x -> {
                    x.readIdentifier();
                    return TokenType.IDENTIFIER;
                });

        public static final Case CHARACTER_CONSTANT = Case.of(
                '\'', x -> {
                    x.readStringOrCharacter('\'');
                    return TokenType.CHARACTER;
                });

        public static final Case STRING_LITERAL = Case.of(
                '"', x -> {
                    x.readStringOrCharacter('"');
                    return TokenType.STRING;
                });

        public static final Case DIRECTIVE_DELIMITER = Case.of(
                Chars.DIRECTIVE_DELIMITER_SET, x -> {
                    x.readZeroOrMoreChars(Chars::isDirectiveDelimiter);
                    return TokenType.DELIMITER;
                });

        public static final Case DIRECTIVE_END = Case.of(
                '\n', x -> TokenType.DIRECTIVE_END);

        public static final Case STANDARD_HEADER = Case.of(
                '<', x -> {
                    x.readFilename('>');
                    return TokenType.STANDARD_HEADER;
                });

        public static final Case FILENAME = Case.of(
                '\"', x -> {
                    x.readFilename('\"');
                    return TokenType.FILENAME;
                });

        public static final Case DOUBLE_DOT = Case.of(
                '.', x -> {
                    if (x.readZeroOrOneChar(c -> c == '.') == null) {
                        // .
                        var s = x.getSource();
                        var b = x.getBuilder();
                        s.ungetChar(b.removeLast());
                        return TokenType.OPERATOR;
                    }
                    // ...
                    return TokenType.PUNCTUATOR;
                });

        public static final Case ASTERISK_AFTER_SLASH = Case.of(
                '*', x -> {
                    x.readComment();
                    return TokenType.COMMENT;
                });

        public static final Case DOUBLE_SLASH = Case.of(
                '/', x -> {
                    x.readSingleLine();
                    return TokenType.COMMENT;
                });

        public static final Case IDENTIFIER_AFTER_PREFIX = Case.of(
                Chars.IDENTIFIER_SET, x -> {
                    x.readIdentifier();
                    return TokenType.IDENTIFIER;
                });

        public static final Case LOWER_U_AFTER_BACKSLASH = Case.of(
                'u', newUniversalCharacterNameCase(4));

        public static final Case UPPER_U_AFTER_BACKSLASH = Case.of(
                'U', newUniversalCharacterNameCase(8));
    }
}
