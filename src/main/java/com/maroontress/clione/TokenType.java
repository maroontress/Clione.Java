package com.maroontress.clione;

import java.io.Reader;
import java.util.Collection;

/**
    The constants representing the token type.
*/
public enum TokenType {

    /**
        The character constant beginning with either {@code "'"}, {@code "u'"},
        {@code "U'"}, or {@code "L'"} and ending with a {@code "'"}.

        <p>The character constant can contain an escape sequence between single
        quotes.</p>
    */
    CHARACTER,

    /**
        The comment beginning with a slash followed by an asterisk
        ({@code /}{@code *}) and ending with an asterisk followed by a slash
        ({@code *}{@code /}) that is not inside a character constant, a string
        literal, or a standard header name.
    */
    COMMENT,

    /**
        The delimiter that is a sequence of the delimiter characters containing
        a space character ({@code ' '}) and a horizontal tab character
        ({@code '\t'}).
    */
    DELIMITER,

    /**
        The directive beginning with a number character ({@code #}) and
        ending with a newline character.
    */
    DIRECTIVE,

    /**
        The directive name that is an identifier followed by the number
        character ({@code #}) that every directive begins with (except
        delimiters).

        <p>The identifier of directive name can be either {@code include},
        {@code define}, {@code if}, {@code ifdef}, {@code ifndef},
        {@code elif}, {@code else}, {@code endif}, {@code line}, {@code error},
        or {@code pragma}.</p>
    */
    DIRECTIVE_NAME,

    /** The end of directive ({@code '\n'}). */
    DIRECTIVE_END,

    /**
        The filename between double quotes ({@code "}) that follows an
        {@code include} directive name.

        <p>Note that this differs from {@link #STRING}.</p>
    */
    FILENAME,

    /**
        The identifier.

        <p>An identifier can contain the following characters:</p>
        <ul>
        <li>an underscore character or an uppercase or lowercase letter
        ({@code [_A-Za-z]})</li>
        <li>a digit ({@code [0-9]})</li>
        <li>universal character names ({@code \}{@code uXXXX} or
        {@code \}{@code UXXXXXXXX},
        where {@code X} is a hexadecimal digit)</li>
        <li>other implementation-defined characters</li>
        </ul>
        <p>However, the first character of an identifier cannot be a digit.</p>
    */
    IDENTIFIER,

    /**
        The number that is an integer constant or a floating-point number
        constant.
    */
    NUMBER,

    /**
        The digits that follow a {@code line} directive name.

        <p>Note that this differs from {@link #NUMBER}.</p>
    */
    DIGITS,

    /**
        The operator.

        <p>The operator is either: {@code +}, {@code -}, {@code *}, {@code /},
        {@code %}, {@code ++}, {@code --}, {@code ==}, {@code !=},
        {@code >}, {@code <}, {@code >=}, {@code <=},
        {@code !}, {@code &&}, {@code ||},
        {@code ~}, {@code &}, {@code |}, {@code ^}, {@code >>}, {@code <<},
        {@code =}, {@code +=}, {@code -=}, {@code *=}, {@code /=}, {@code %=},
        {@code &=}, {@code |=}, {@code ^=},
        {@code <<=}, {@code >>=},
        {@code '.'}, {@code '->'}, or {@code '?'}.</p>
    */
    OPERATOR,

    /**
        The punctuator that is either {@code '['}, {@code ']'}, {@code '('},
        {@code ')'}, <code>'&#123;'</code>, <code>'&#125;'</code>, {@code ','},
        {@code ';'}, {@code ':'}, or {@code '...'}.

        <p>Note that some punctuators may be C operators according to the
        syntactic context.</p>
    */
    PUNCTUATOR,

    /**
        The reserved words that are identifiers but the string collection
        specified with the factory method of
        {@link LexicalParser#of(Reader, Collection)}.
    */
    RESERVED,

    /**
        The standard header name between angle brackets ({@code '<'} and
        {@code '>'}) that follow an {@code include} directive.

        <p>For example: {@code <stdio.h>}, {@code <stdlib.h>}.</p>
    */
    STANDARD_HEADER,

    /**
        The string literal beginning with either {@code '"'}, {@code 'u"'},
        {@code 'U"'}, {@code 'L"'}, or {@code 'u8"'} and ending with a
        {@code '"'}.

        <p>The string literal can contain escape sequences between double
        quotes.</p>
    */
    STRING,

    /** The unknown token that is invalid in the syntax. */
    UNKNOWN,
}
