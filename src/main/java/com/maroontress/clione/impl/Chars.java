package com.maroontress.clione.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
    Provides immutable {@link Set} objects of a character and utility methods
    for determining a character's category (that is letter, digit, and so on).
*/
public final class Chars {

    /** A character set containing only digits. */
    public static final Set<Character> DIGIT_SET = Sets.DIGITS;

    /** A character set containing only delimiters inside a directive. */
    public static final Set<Character> DIRECTIVE_DELIMITER_SET
            = Sets.DIRECTIVE_DELIMITERS;

    /** A character set containing only delimiters. */
    public static final Set<Character> DELIMITER_SET = Sets.DELIMITERS;

    /**
        A set of characters which can be the first character of an identifier.
    */
    public static final Set<Character> FIRST_OF_IDENTIFIER_SET = union(
            List.of(Set.of('_'),
                    Sets.UPPER_CASE_LETTERS,
                    Sets.LOWER_CASE_LETTERS));

    /**
        A set of characters which an identifier can contain. Note that this
        does not contain Unicode non-digit characters.
    */
    public static final Set<Character> IDENTIFIER_SET = union(
            List.of(Set.of('_'),
                    Sets.UPPER_CASE_LETTERS,
                    Sets.LOWER_CASE_LETTERS,
                    Sets.DIGITS));

    private static final Set<Character> PP_NUMBER_SET = union(
            List.of(Set.of('.'),
                    Sets.UPPER_CASE_LETTERS,
                    Sets.LOWER_CASE_LETTERS,
                    Sets.DIGITS));

    private static final Set<Character> HEX_DIGIT_SET = union(
            List.of(Sets.DIGITS,
                    Sets.newCharSetWithRange('A', 'F'),
                    Sets.newCharSetWithRange('a', 'f')));

    /** Prevents the class from being instantiated. */
    private Chars() {
        throw new AssertionError();
    }

    /**
        Determines if the specified character is a delimiter inside a
        directive.

        @param c The character to be tested.
        @return {@code true} if the character is a delimiter inside a
            directive.
    */
    public static boolean isDirectiveDelimiter(char c) {
        return DIRECTIVE_DELIMITER_SET.contains(c);
    }

    /**
        Determines if the specified character is a delimiter.

        @param c The character to be tested.
        @return {@code true} if the character is a delimiter.
    */
    public static boolean isDelimiter(char c) {
        return DELIMITER_SET.contains(c);
    }

    /**
        Determines if the specified character composes a preprocessing number
        (except {@code '+'} and {@code '-'} following either {@code e} or
        {@code E}).

        @param c The character to be tested.
        @return {@code true} if the character is composes a preprocessing
            number.
    */
    public static boolean isPreprocessingNumber(char c) {
        return PP_NUMBER_SET.contains(c);
    }

    /**
        Determines if the specified character is a digit ({@code [0-9]}).

        @param c The character to be tested.
        @return {@code true} if the character is a digit.
    */
    public static boolean isDigit(char c) {
        return DIGIT_SET.contains(c);
    }

    /**
        Determines if the specified character may be part of a number suffix.
        ({@code [uUlL]}).

        @param c The character to be tested.
        @return {@code true} if the character be part of a number suffix.
    */
    public static boolean isNumberSuffix(char c) {
        return c == 'u' || c == 'U'
                || c == 'l' || c == 'L';
    }

    /**
        Determines if the specified character is a hexadecimal digit
        ({@code [0-9a-fA-F]}).

        @param c The character to be tested.
        @return {@code true} if the character is a hexadecimal digit.
    */
    public static boolean isHexDigit(char c) {
        return HEX_DIGIT_SET.contains(c);
    }

    /**
        Determines if the specified character is an octal digit
        ({@code [0-7]}).

        @param c The character to be tested.
        @return {@code true} if the character is an octal digit.
    */
    public static boolean isOctalDigit(char c) {
        return c >= '0' && c <= '7';
    }

    /**
        Determines if the specified character may be the first character in an
        identifier (except Universal Character Names and other
        implementation-defined characters).

        @param c The character to be tested.
        @return {@code true} if the character may start an identifier.
    */
    public static boolean isFirstName(char c) {
        return FIRST_OF_IDENTIFIER_SET.contains(c);
    }

    /**
        Determines if the specified character may be part of an identifier as
        other than the first character (except Universal Character Names and
        other implementation-defined characters).

        @param c The character to be tested.
        @return {@code true} if the character be part of an identifier.
    */
    public static boolean isName(char c) {
        return IDENTIFIER_SET.contains(c);
    }

    private static Set<Character> union(List<Set<Character>> all) {
        return all.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static final class Sets {
        private static final Set<Character> UPPER_CASE_LETTERS
                = newCharSetWithRange('A', 'Z');

        private static final Set<Character> LOWER_CASE_LETTERS
                = newCharSetWithRange('a', 'z');

        private static final Set<Character> DIGITS
                = newCharSetWithRange('0', '9');

        private static final Set<Character> DIRECTIVE_DELIMITERS
                = Set.of(' ', '\t');

        private static final Set<Character> DELIMITERS
                = Set.of(' ', '\t', '\n', '\f', '\u000b');

        private static final Set<Character> ESCAPE_SEQUENCE
                = Set.of('a', 'b', 'e', 'f', 'n', 'r', 't', 'v',
                '\\', '\'', '"', '?');

        private static Set<Character> newCharSetWithRange(
                char start, char end) {
            return IntStream.rangeClosed(start, end)
                    .mapToObj(c -> (char) c)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
