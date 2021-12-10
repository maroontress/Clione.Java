package com.maroontress.clione.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.maroontress.clione.TokenType;

/**
    The mapping of one or more characters to a tokenizer.

    <p>Note that the {@link Case} instance is an immutable object.</p>
*/
public final class Case {

    private final Set<Character> charSet;
    private final Tokenizer tokenizer;

    private Case(Set<Character> charSet, Tokenizer tokenizer) {
        this.charSet = Set.copyOf(charSet);
        this.tokenizer = tokenizer;
    }

    /**
        Returns the unmodifiable set of characters that this case maps.

        @return The unmodifiable set of characters.
    */
    public Set<Character> getCharSet() {
        return charSet;
    }

    /**
        Returns the tokenizer that this case maps characters to.

        @return The tokenizer.
    */
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    /**
        Returns a new case that starts with the specified character and has
        the specified tokenizer.

        @param c The character that the case starts with.
        @param tokenizer The tokenizer associated with the case.
        @return The new case.
    */
    public static Case of(char c, Tokenizer tokenizer) {
        return new Case(Set.of(c), tokenizer);
    }

    /**
        Returns a new case that maps the specified character set to the
        specified tokenizer.

        @param set The set of characters to map
        @param tokenizer The tokenizer to map characters to.
        @return The new case.
    */
    public static Case of(Set<Character> set, Tokenizer tokenizer) {
        return new Case(set, tokenizer);
    }

    /**
        Returns a new case that starts with the specified character
        and that one of the specified cases may follow.

        @param c The character that the case starts with.
        @param otherwise The token type that the tokenizer returns when none
            of the specified cases follows the character {@code c}.
        @param cases The cases that may follow the character {@code c}.
        @return The new case.
    */
    public static Case of(char c, TokenType otherwise, Case... cases) {
        var mapper = newMapper(cases);
        return Case.of(c, x -> x.tryReadToken(mapper, otherwise));
    }

    /**
        Returns a new case that starts with the specified character
        and that one of the specified cases may follow.

        @param c The character that the case starts with.
        @param otherwise The tokenizer returns when none of the specified
            cases follows the character {@code c}.
        @param cases The cases that may follow the character {@code c}.
        @return The new case.
    */
    public static Case of(char c, Tokenizer otherwise, Case... cases) {
        var mapper = newMapper(cases);
        return Case.of(c, x -> x.tryReadToken(mapper, otherwise));
    }

    /**
        Returns a new case that starts with one of characters in the
        specified set and that one of the specified cases may follow it.

        @param set The set of characters, one of which the case starts with.
        @param otherwise The token type that the tokenizer returns when none
            of the specified cases follows the one of the characters.
        @param cases The cases that may follow one of the characters.
        @return The new case.
    */
    public static Case of(Set<Character> set, TokenType otherwise,
                          Case... cases) {
        var mapper = newMapper(cases);
        return Case.of(set, x -> x.tryReadToken(mapper, otherwise));
    }

    private static Map<Character, Tokenizer> newMap(List<Case> list) {
        var map = new HashMap<Character, Tokenizer>();
        for (var i : list) {
            var set = i.getCharSet();
            var reader = i.getTokenizer();
            for (var c : set) {
                map.putIfAbsent(c, reader);
            }
        }
        return Map.copyOf(map);
    }

    /**
        Returns a new mapping function that takes a character and returns a
        tokenizer associated with the character with the specified cases.

        @param cases The cases.
        @return The new map.
    */
    public static Mapper newMapper(Case... cases) {
        var map = Case.newMap(List.of(cases));
        return map::get;
    }

    /**
        The function that that takes a character and returns a tokenizer
        associated with the character.
    */
    @FunctionalInterface
    public interface Mapper {
        /**
            Returns the tokenizer with which the specified character is
            associated, or {@code null} if there is no tokenizer associated
            with the character.

            @param c The character.
            @return The tokenizer associated with the character {@code c},
            or {@code null} if there is no tokenizer associated with {@code c}.
        */
        Tokenizer get(char c);
    }
}
