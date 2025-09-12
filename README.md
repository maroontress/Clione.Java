# Clione

Clione is a Java implementation of a lexical parser that tokenizes source code
written in C17 and other C-like programming languages.

The main facility is a tokenization API corresponding to the C preprocessor
layer. It includes the features of trigraph replacement, line splicing, and
tokenization but does not include macro expansion and directive handling.

## Example

[A typical usage example](src/test/java/com/example/TokenDemo.java) would be as
follows:

```java
package com.example;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import com.maroontress.clione.LexicalParser;
import com.maroontress.clione.Token;

public final class TokenDemo {

    public static void main(String[] args) {
        var path = FileSystems.getDefault().getPath(args[0]);
        try (var parser = LexicalParser.of(Files.newBufferedReader(path))) {
            run(parser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void run(LexicalParser parser) throws IOException {
        for (;;) {
            var maybeToken = parser.next();
            if (maybeToken.isEmpty()) {
                break;
            }
            var token = maybeToken.get();
            printToken(token, "");
        }
    }

    public static void printToken(Token token, String indent) {
        var type = token.getType();
        var value = token.getValue();
        var span = token.getSpan();
        var s = switch (type) {
            case DELIMITER, DIRECTIVE_END
                    -> "'" + value.replaceAll("\n", "\\\\n") + "'";
            default -> value;
        };
        System.out.printf("%s%s: %s: %s%n", indent, span, type, s);
        for (var child : token.getChildren()) {
            printToken(child, indent + "| ");
        }
    }
}
```

And [`helloworld.c`](src/test/resources/com/example/helloworld.c) would be as
follows:

```c
#include <stdio.h>

int main(void)
{
    printf("hello world\n");
}
```

In this example, the result of "`java com.example.TokenDemo helloworld.c`" is
as follows:

```plaintext
L1:1--19: DIRECTIVE: #
| L1:2--8: DIRECTIVE_NAME: include
| L1:9: DELIMITER: ' '
| L1:10--18: STANDARD_HEADER: <stdio.h>
| L1:19: DIRECTIVE_END: '\n'
L2:1: DELIMITER: '\n'
L3:1--3: RESERVED: int
L3:4: DELIMITER: ' '
L3:5--8: IDENTIFIER: main
L3:9: PUNCTUATOR: (
L3:10--13: RESERVED: void
L3:14: PUNCTUATOR: )
L3:15: DELIMITER: '\n'
L4:1: PUNCTUATOR: {
L4:2--L5:4: DELIMITER: '\n    '
L5:5--10: IDENTIFIER: printf
L5:11: PUNCTUATOR: (
L5:12--26: STRING: "hello world\n"
L5:27: PUNCTUATOR: )
L5:28: PUNCTUATOR: ;
L5:29: DELIMITER: '\n'
L6:1: PUNCTUATOR: }
L6:2: DELIMITER: '\n'
```

## Tokens

The `LexicalParser` object creates and returns a token from the stream of the
source file. It often extracts the ones from the source file, but trigraph and
digraph substitution and line concatenation may result in tokens that are not
in the source file. It returns an empty token when it finally reaches the end
of the source file.

The `Token` objects that the `next()` method of `LexicalParser` instance
returns are the preprocessing tokens. So, the evaluation is necessary before
using their content. In other words, they can be incomplete according to the
token type. For example, the string literal or comment may not terminate, the
preprocessing number may not represent valid integer and floating-point
constants, and so on.

As in the example above, `Token` objects can have children, which means they
can be in a tree structure. For tokens that the `next()` method returns, tokens
of type `TokenType.DIRECTIVE` only have children.

The `Token` object has its type, span, and characters. The type is one of the
constants defined in `enum TokenType`, the span represents the range of the
source file where the token occurs, and the characters are `SourceChar` objects
that compose it.

## Characters

The `SourceChar` object represents a character that composes the token or EOF.
It may also have one or more child characters in some cases. For example, it is
the case that it represents:

- the character which is substituted for any digraph or trigraph sequence
- the character that follows a backslash (`\`) at the end of the line

[The following code](src/test/java/com/example/SourceCharDemo.java) shows an
example:

```java
package com.example;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;

import com.maroontress.clione.LexicalParser;
import com.maroontress.clione.SourceChar;
import com.maroontress.clione.Token;

public final class SourceCharDemo {

    public static void main(String[] args) {
        var path = FileSystems.getDefault().getPath(args[0]);
        try (var parser = LexicalParser.of(Files.newBufferedReader(path))) {
            run(parser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void run(LexicalParser parser) throws IOException {
        for (;;) {
            var maybeToken = parser.next();
            if (maybeToken.isEmpty()) {
                break;
            }
            printToken(maybeToken.get());
        }
    }

    public static void printToken(Token token) {
        var type = token.getType();
        var value = token.getValue();
        var span = token.getSpan();
        var s = switch (type) {
            case DELIMITER, DIRECTIVE_END
                    -> "'" + value.replaceAll("\n", "\\\\n") + "'";
            default -> value;
        };
        System.out.printf("%s: %s: %s%n", span, type, s);
        printChars(token.getChars(), "  ");
    }

    private static void printChars(List<SourceChar> chars, String indent) {
        for (var c : chars) {
            var span = c.getSpan();
            var value = c.toChar();
            var s = (value == '\n')
                    ? "'\\n'"
                    : Character.isHighSurrogate(value)
                    ? "H(0x" + Integer.toString((int) value, 16) + ")"
                    : Character.isLowSurrogate(value)
                    ? "L(0x" + Integer.toString((int) value, 16) + ")"
                    : String.valueOf(value);
            System.out.printf("%s%s: %s%n", indent, span, s);
            printChars(c.getChildren(), indent + "| ");
        }
    }
}
```

And
[`main.c`](src/test/resources/com/example/main.c) would be as follows:

```c
ma??/
in
```

In this example, the result of "`java com.example.SourceCharDemo main.c`" is as follows:

```plaintext
L1:1--L2:2: IDENTIFIER: main
  L1:1: m
  L1:2: a
  L1:3--L2:1: i
  | L1:3--5: \
  | | L1:3: ?
  | | L1:4: ?
  | | L1:5: /
  | L1:6: '\n'
  | L2:1: i
  L2:2: n
⋮
```

The result illustrates that the character `i` in the identifier `main` has
child characters: a backslash (`\`), a newline (`\n`), and `i`. Furthermore,
the backslash character has child characters: `?`, `?`, and `/`. Of course,
what happens is that the trigraph sequence `??/` is replaced with a backslash
at first, and then the backslash at the end of the line results in the line
concatenation.

## Surrogate pairs

A character corresponds to a column. So, one `char` value often represents one
column. However, in the case of a character represented with a surrogate pair,
the two `char` values in the pair represent one column. Here is an example
[`emojicat.c`](src/test/resources/com/example/emojicat.c):

```c
char *cat = u8"🐱";
```

The result of "`java com.example.SourceCharDemo emojicat.c`" is as follows:

```plaintext
⋮
L1:19--23: STRING: u8"🐱"
  L1:13: u
  L1:14: 8
  L1:15: "
  L1:16: H(0xd83d)
  L1:16: L(0xdc31)
  L1:17: "
⋮
```

This example shows that the high and low surrogate characters are in the same
column.

## Phases of translation

The lexical parser starts tokenization after trigraph replacement and line
splicing, according to the
[_phases of translation_][wikipedia-phases-of-translation].

### Newlines

Before anything else, the lexical parser substitutes `\n` for all newlines,
that is, line feed (LF), carriage return and line feed (CRLF), and carriage
return (CR) in the stream, even if different newlines are mixed in the stream.
It indicates `\n` as a newline (NL) character, regardless of platform.

### Trigraphs

After unifying newline characters, the lexical parser replaces
[trigraph sequences][wikipedia-trigraph] with the new `SourceChar` objects they
represent. The new one becomes the parent of the replaced characters and
represents their equivalent. The following table lists all trigraphs:

| Trigraph  | Equivalent |
| :---:  | :---: |
| `??<`  | `{`   |
| `??>`  | `}`   |
| `??[`  | `(`   |
| `??]`  | `)`   |
| `??=`  | `#`   |
| `??/`  | `\`   |
| `??'`  | `^`   |
| `??!`  | `\|`  |
| `??-`  | `~`   |

### Line splicing

Next to the trigraph replacement, the lexical parser removes the backslash
character at the end of the line. To be more precise, it replaces the
backslash, the newline character, and the next character with a new
`SourceChar` object. The new one becomes the parent of the replaced characters
and represents the character that followed the backslash and newline
characters.

A pair of the backslash and newline characters may appear two or more times
with consecutive occurrences. In that case, the new substituted one becomes the
parent of both their characters and the next character.

### Tokenization

After line splicing, the lexical parser starts to break the `SourceChar` stream
into `Token`s. A `Token` object may be either:

- delimiters (that are sequences of whitespace characters)
- comments
- directives
- preprocessing tokens (that are standard header names, identifiers,
  preprocessing numbers, character constants, string literals, operators and
  punctuators, or unknown token)

## Delimiters

A delimiter is a separator between tokens. Strictly speaking, it is not a
token, but the lexical parser returns the delimiter as a token. Some
applications may completely ignore delimiters (for example, code formatters).

The space, horizontal tab (HT), form feed (FF), vertical tab (VT), and NL
characters are delimiters within any non-directive line. The space and HT
characters are delimiters within any directive lines.

> ☕ By the way, have you seen source code including FF and VT characters? In
> the past, people often printed source code on paper. In the 1980s, I saw some
> source code that included a FF character inserted between functions. It
> resulted in a page break, so each function started at the top of the page. As
> far as a VT character goes, I have never seen it in the source code.

The token type of delimiters is `TokenType.DELIMITER`.

## Comments

A comment also can be a delimiter, because C preprocessors replace each comment
with a space character.

There are two types of comments. The one starts with `/*` and ends with `*/`.
The other starts with `//` and ends with a newline character. No comment can be
inside a character constant, a string literal, a standard header name, or a
filename in either case.

The content of the token can be incomplete. For example, it may not terminate,
and so on.

The token type of comments is `TokenType.COMMENT`.

## Identifiers

An identifier is a preprocessing token.

The first character of an identifier name must be one of:

- an underscore character or an uppercase or lowercase letter (`[_A-Za-z]`)
- universal character names (`\uXXXX` or `\UXXXXXXXX`, `X` is a hexadecimal
  digit)
- other implementation-defined characters

The second and subsequent character must be one of them or a digit (`[0-9]`).

The _other implementation-defined characters_ that `LexicalParser`'s
implementation defines are of
[Unicode Identifier](https://unicode.org/reports/tr31/) that is as follows:

- The first character: a character with which the
  [Character.isUnicodeIdentifierStart(int)][isUnicodeIdentifierStart]
  method returns `true`
- The second and subsequent character: a character with which the
  [Character.isUnicodeIdentifierPart(int)][isUnicodeIdentifierPart]
  method returns `true`

So, the lexical parser can parse the following C code:

```c
char *\U0001f431 = "cat";
```

However, it does NOT support the following code because Unicode Identifier does
not contain the emoji characters such as 🐱:

```c
char *🐱 = "cat";
```

Note that the recent famous C compilers (like GCC, Clang, etc.) can compile the
code where an identifier contains emoji characters like this.

The token type of identifiers is `TokenType.IDENTIFIER`.

## Reserved words

Reserved words are equivalent to identifiers, but they are in the set of
keywords, which you can specify with the factory method of `LexicalParser`.

The token type of reserved words is `TokenType.RESERVED`.

## Character constants

A character constant is a preprocessing token.

It consists of one or more characters enclosed in single quotes. The quotes may
follow a prefix either `L`, `u`, or `U`. It may contain
[escape sequences][wikipedia-escape-character]. It may not contain a newline
character.

The content of the token can be incomplete. For example, it may not terminate,
it may contain no character, two or more characters, or invalid escape
sequences inside the single quotes, and so on.

The token type of character constants is `TokenType.CHARACTER`.

## String literals

A string literal is a preprocessing token.

It consists of zero or more characters enclosed in double quotes. The quotes
may follow a prefix either `L`, `u`, `U`, or `u8`. It may contain
[escape sequences][wikipedia-escape-character]. It may not contain a newline
character.

The content of the token can be incomplete. For example, it may not terminate,
it may contain invalid escape sequences inside the double quotes, and so on.

The token type of string literals is `TokenType.STRING`.

## Preprocessing numbers

A preprocessing number is a preprocessing token.

It includes all integer and floating-point constants but does other forms
except them.

The content of the token can be incomplete. For example, it may not represent
both integer and floating-point constants, and so on.

The token type of preprocessing numbers is `TokenType.NUMBER`.

## Operators and punctuators

Operator or punctuator tokens are preprocessing tokens. The following table
lists valid tokens of which the type is `TokenType.OPERATOR`:

```plaintext
+       -       *       /       %       ++      --      ==      !=
>       <       >=      <=      !       &&      ||      ~       &
|       ^       <<      >>      =       +=      -=      *=      /=
%=      &=      |=      ^=      <<=     >>=     ->      .       ?
```

Note that these are preprocessing tokens, not C operators. For example,
`sizeof` is an operator in C, but a reserved word (or an identifier) as a
preprocessing token.

The following table lists all valid tokens of which the type is
`TokenType.PUNCTUATOR`:

```plaintext
(       )       [       ]       {       }       :
;       ,       ...     <:      :>      <%      %>
```

The lexical parser provides special treatment for four tokens: `#`, `%:`, `##`,
and `%:%:`. Their type is `TokenType.OPERATOR` when they appear in directive
lines. Otherwise, if they are the first token of the line, `#` and `%:` are
classified as `TokenType.DIRECTIVE`, while `##` and `%:%:` are classified as
`TokenType.UNKNOWN`, as shown below:

| Tokens | In directive lines | Otherwise |
|:---:|:---:|:---:|
| `#` `%:`    | `TokenType.OPERATOR` | `TokenType.DIRECTIVE` |
| `##` `%:%:` | `TokenType.OPERATOR` | `TokenType.UNKNOWN`   |

If they are neither in directive lines nor the first token of the line, their
type is `TokenType.PUNCTUATOR`.

The following table lists all tokens that are digraphs:

| Token  | Equivalent |
| :---:  | :---: |
| `<:`   | `[`   |
| `:>`   | `]`   |
| `<%`   | `{`   |
| `%>`   | `}`   |
| `%:`   | `#`   |
| `%:%:` | `##`  |

The lexical parser replaces the digraphs with their equivalents. The
substituted characters have the child characters that represent the replaced
ones.

## Directives

A directive token consists of a number sign (or hash) character (`#`) and the
child tokens. The null directive has no child tokens.

The child tokens must include a directive name, arguments (depending on the
directive name), and the end of the directive (that is a newline character).
They also may include delimiters and comments. The last of them must be the end
of the directive.

The content of the child tokens can be incomplete. For example, they may
represent an invalid directive, they may not end with the end of the directive,
and so on.

The token type of directives is `TokenType.DIRECTIVE`.

The tokens that represent the directive names must have the content which is
either: `define`, `undef`, `include`, `if`, `ifdef`, `ifndef`, `else`, `elif`,
`endif`, `line`, `error`, or `pragma`. Their token type is
`TokenType.DIRECTIVE_NAME`.

The tokens that represents the end of the directive must have a newline
character as the content. Their token type is `TokenType.DIRECTIVE_END`.

### Include directives

When the directive name equals `include`, the argument must be either:

- a standard header name between angle brackets (`<` and `>`)
- a filename between double quotes (`"` and `"`)
- any other form that expands to a standard header name or a filename after
  macro replacement

A standard header name and a filename are preprocessing tokens.

The content of the tokens can be incomplete. For example, they may not
terminate, and so on.

The token types of standard header names and filenames are
`TokenType.STANDARD_HEADER` and `TokenType.FILENAME`, respectively.

### Line directives

When the directive name equals `line`, the arguments must be either:

- a sequence of digits
- a sequence of digits followed by a filename between double quotes (`"` and
  `"`)
- any other form that expands to the above forms after macro replacement

A sequence of digits and a filename are preprocessing tokens.

The content of the tokens can be incomplete. For example, the filename may not
terminate, and so on.

The token types of a sequence of digits and a filename are `TokenType.DIGITS`
and `TokenType.FILENAME`, respectively.

## Unknown tokens

When the lexical parser encounters characters that do not fit the above
description, it returns an unknown token containing them.

The token type of unknown tokens is `TokenType.UNKNOWN`.

## API Reference

- [com.maroontress.clione][apiref-maroontress.clione] module

[isUnicodeIdentifierPart]:
  https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#isUnicodeIdentifierPart(int)
[isUnicodeIdentifierStart]:
  https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#isUnicodeIdentifierStart(int)
[apiref-maroontress.clione]:
  https://maroontress.github.io/Clione-Java/api/latest/html/index.html
[wikipedia-trigraph]:
  https://en.wikipedia.org/wiki/Digraphs_and_trigraphs#C
[wikipedia-escape-character]:
  https://en.wikipedia.org/wiki/Escape_sequences_in_C#Table_of_escape_sequences
[wikipedia-phases-of-translation]:
  https://en.wikipedia.org/wiki/C_preprocessor#Phases
