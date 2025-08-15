package com.maroontress.clione;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public final class LexicalParserTest {

    @Test
    public void delimiters() {
        // The escape sequence '\v' in C is not in Java. You can represent a VT
        // character in Java with a backslash followed by 'u000b'.
        var s = "foo bar\tbaz\fbarbaz\u000bqux";
        var list = List.of(pair("foo", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("bar", TokenType.IDENTIFIER),
                pair("\t", TokenType.DELIMITER),
                pair("baz", TokenType.IDENTIFIER),
                pair("\f", TokenType.DELIMITER),
                pair("barbaz", TokenType.IDENTIFIER),
                pair("\u000b", TokenType.DELIMITER),
                pair("qux", TokenType.IDENTIFIER));
        test(s, list);
    }

    @Test
    public void identifiers() {
        var s = """
            foo
            bar
            baz""";
        var list = List.of(pair("foo", TokenType.IDENTIFIER),
                pair("\n", TokenType.DELIMITER),
                pair("bar", TokenType.IDENTIFIER),
                pair("\n", TokenType.DELIMITER),
                pair("baz", TokenType.IDENTIFIER));
        test(s, list);
    }

    @Test
    public void punctuators() {
        test("[ ] ( ) { } , ; : ...", TokenType.PUNCTUATOR);
    }

    @Test
    public void withoutKeywords() {
        var s = "auto";
        test(s, parser -> {
            {
                var maybeToken = parser.next();
                assertThat(maybeToken.isPresent(), is(true));
                var token = maybeToken.get();
                assertThat(token.getType(), is(TokenType.IDENTIFIER));
                assertThat(token.getValue(), is(s));
            }
            {
                var maybeToken = parser.next();
                assertThat(maybeToken.isEmpty(), is(true));
            }
        }, Collections.emptySet());
    }

    @Test
    public void keywords() {
        var s = """
            auto break case char const continue default do double else enum
            extern float for goto if int long register return short signed
            sizeof static struct switch typedef union unsigned void volatile
            while
            _Bool _Complex _Imaginary inline restrict
            _Alignas _Alignof _Atomic _Generic _Noreturn _Static_assert
            _Thread_local
            """.trim();
        test(s.replace('\n', ' '), TokenType.RESERVED);
    }

    @Test
    public void digraphReplacement0() {
        var s = """
            %:
            """;
        var childList = List.of(pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void digraphReplacement1() {
        var s = "<: :> <% %>";
        var list = List.of(pair("[", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("]", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("{", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("}", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void digraphReplacement2() {
        var s = """
            %:define x(a,b) a%:%:b
            """;
        var childList = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("a", TokenType.IDENTIFIER),
                pair("##", TokenType.OPERATOR),
                pair("b", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void digraphReplacement3() {
        var s = """
            %:#
            """;
        var childList = List.of(pair("#", TokenType.OPERATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void digraphReplacement4() {
        var s = """
            #%:
            """;
        var childList = List.of(pair("#", TokenType.OPERATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void trigraphReplacement0() {
        var s = """
            ??=
            """;
        var childList = List.of(pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void trigraphReplacement1() {
        var s = "??' ??! ??-";
        var list = List.of(pair("^", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("|", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("~", TokenType.OPERATOR));
        test(s, list);
    }

    @Test
    public void trigraphReplacement2() {
        var s = "??( ??) ??< ??>";
        var list = List.of(pair("[", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("]", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("{", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("}", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void trigraphReplacement3() {
        var s = """
            ma??/
            in '??/'' \"??/\"""";
        var list = List.of(pair("main", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("'\\''", TokenType.CHARACTER),
                pair(" ", TokenType.DELIMITER),
                pair("\"\\\"", TokenType.STRING));
        test(s, list);
    }

    @Test
    public void unknown() {
        test("$ @", TokenType.UNKNOWN);
    }

    @Test
    public void comment() {
        var s = "/* foo */";
        var list = List.of(pair(s, TokenType.COMMENT));
        test(s, list);
    }

    @Test
    public void singleLineComment1() {
        var s = """
            // bar
            """;
        var list = List.of(
                pair("// bar", TokenType.COMMENT),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void singleLineComment2() {
        var s = "// baz";
        var list = List.of(pair(s, TokenType.COMMENT));
        test(s, list);
    }

    @Test
    public void directive0() {
        var s = """
            #define DEBUG
            """;
        var childList = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("DEBUG", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void directive1() {
        var s = """
            #define square(x)\\
                ((x)*(x))
            """;
        var childList = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("square", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("    ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("*", TokenType.OPERATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void directive2() {
        var s = """
            #if 1
            """;
        var childList = List.of(
                pair("if", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("1", TokenType.NUMBER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void directive3() {
        var s = """
            #define x(a,b) a##b
            """;
        var childList = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("a", TokenType.IDENTIFIER),
                pair("##", TokenType.OPERATOR),
                pair("b", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void sharpAsPunctuator() {
        var s = """
            #define IGNORE(x, y)
            IGNORE(#, ##)
            """;
        var childList = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("IGNORE", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("y", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, childList),
                pair("IGNORE", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("#", TokenType.PUNCTUATOR),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("##", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void sharpAsDirective() {
        var s = """
            #define IGNORE(x)
            IGNORE(
            #
            )
            """;
        var defineIgnore = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("IGNORE", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var emptyChildList = List.of(
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineIgnore),
                pair("IGNORE", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER),
                pair("#", TokenType.DIRECTIVE, emptyChildList),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void includeDirective0() {
        var s = """
            #include/* COMMENT
            */<stdio.h>// COMMENT
            """;
        var childList = List.of(
                pair("include", TokenType.DIRECTIVE_NAME),
                pair("/* COMMENT\n*/", TokenType.COMMENT),
                pair("<stdio.h>", TokenType.STANDARD_HEADER),
                pair("// COMMENT", TokenType.COMMENT),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void includeDirective1() {
        // Don't use Text Blocks here because there is a trailing space.
        var s = "#/* COMMENT */include \"main.h\"/**/ \n";
        var childList = List.of(
                pair("/* COMMENT */", TokenType.COMMENT),
                pair("include", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("\"main.h\"", TokenType.FILENAME),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void includeDirective2() {
        var s = """
            #include <:a:>
            """;
        var childList = List.of(
                pair("include", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("<:a:>", TokenType.STANDARD_HEADER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void includeDirective3() {
        var s = """
            #include <%a%>
            """;
        var childList = List.of(
                pair("include", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("<%a%>", TokenType.STANDARD_HEADER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void includeDirective4() {
        var s = """
            #define X(x) x.h>
            #include X(<stdio)
            """;
        var defineChildList = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("X", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(".", TokenType.OPERATOR),
                pair("h", TokenType.IDENTIFIER),
                pair(">", TokenType.OPERATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var includeChildList = List.of(
                pair("include", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("X", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("<", TokenType.OPERATOR),
                pair("stdio", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineChildList),
                pair("#", TokenType.DIRECTIVE, includeChildList));
        test(s, list);
    }

    @Test
    public void unterminatedStandardHeader() {
        var s = """
            #include <std
            """;
        var childList = List.of(
                pair("include", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("<std", TokenType.STANDARD_HEADER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void unterminatedFilename() {
        var s = """
            #include "std
            """;
        var childList = List.of(
                pair("include", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("\"std", TokenType.FILENAME),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(pair("#", TokenType.DIRECTIVE, childList));
        test(s, list);
    }

    @Test
    public void slashEof() {
        var s = "/";
        var pair = pair("/", TokenType.OPERATOR);
        var list = List.of(pair);
        test(s, list);
    }

    @Test
    public void doubleDot() {
        var s = "..";
        var pair = pair(".", TokenType.OPERATOR);
        var list = List.of(pair, pair);
        test(s, list);
    }

    @Test
    public void operatorTokens() {
        var s = """
            + - * / % ++ --
            == != > < >= <=
            ! && ||
            ~ & | ^ << >>
            = += -= *= /= %= &= |= ^= <<= >>=
            . ->
            ?
            """.trim();
        test(s.replace('\n', ' '), TokenType.OPERATOR);
    }

    @Test
    public void identifierNotPrefix() {
        test("u U L u8", TokenType.IDENTIFIER);
    }

    @Test
    public void lineConcatenates() {
        var s = """
                ma\\
                in""";
        test(s, parser -> {
            var maybeToken = parser.next();
            var where = parser.getLocation();
            assertThat(maybeToken.isPresent(), is(true));
            var token = maybeToken.get();
            assertThat(token.getType(), is(TokenType.IDENTIFIER));
            assertThat(token.getValue(), is("main"));
            assertThat(token.getSpan().toString(), is("L1:1--L2:2"));
            assertThat(where.toString(), is("L2:3"));

            var iList = List.of(
                    pair('\\', "L1:3"),
                    pair('\n', "L1:4"),
                    pair('i', "L2:1"));
            var list = List.of(
                    pair('m', "L1:1"),
                    pair('a', "L1:2"),
                    pair('i', "L1:3--L2:1", iList),
                    pair('n', "L2:2"));
            var chars = token.getChars();
            test(chars, list);
        });
    }

    @Test
    public void lineConcatenatesWithTrigraph() {
        var s = """
            ma??/
            in""";
        //  123456
        test(s, parser -> {
            var maybeToken = parser.next();
            var where = parser.getLocation();
            assertThat(maybeToken.isPresent(), is(true));
            var token = maybeToken.get();
            assertThat(token.getType(), is(TokenType.IDENTIFIER));
            assertThat(token.getValue(), is("main"));
            assertThat(token.getSpan().toString(), is("L1:1--L2:2"));
            assertThat(where.toString(), is("L2:3"));

            var trigraphList = List.of(
                    pair('?', "L1:3"),
                    pair('?', "L1:4"),
                    pair('/', "L1:5"));
            var iList = List.of(
                    pair('\\', "L1:3--5", trigraphList),
                    pair('\n', "L1:6"),
                    pair('i', "L2:1"));
            var list = List.of(
                    pair('m', "L1:1"),
                    pair('a', "L1:2"),
                    pair('i', "L1:3--L2:1", iList),
                    pair('n', "L2:2"));
            var chars = token.getChars();
            test(chars, list);
        });
    }

    @Test
    public void characterConstant() {
        var s = "'c' L'w' u'u' U'U'";
        test(s, TokenType.CHARACTER);
    }

    @Test
    public void unterminatedCharacterConstant() {
        var s = """
            'c
            """;
        var list = List.of(pair("'c", TokenType.CHARACTER),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void stringLiteral() {
        var s = """
            "char" L"wchar_t" u"ucs2" U"ucs4" u8"utf8"
            """.trim();
        test(s, TokenType.STRING);
    }

    @Test
    public void unterminatedStringLiteral() {
        var s = """
            "hello
            """;
        var list = List.of(pair("\"hello", TokenType.STRING),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void escapeSequenceInChar() {
        var s = "'\\u1234' '\\U12345678' '\\1' '\\12' '\\123' '\\x12345' '\\n'";
        test(s, TokenType.CHARACTER);
    }

    @Test
    public void escapeSequenceInString() {
        var s = "\"\\u1234\\U12345678\\1\\12\\123\\x12345\\n\"";
        var list = List.of(pair(s, TokenType.STRING));
        test(s, list);
    }

    @Test
    public void ppNumbers1() {
        /*
            0            .123           3E
            123          123E0F         3e+xy
            123LU        0.123E-005     2for1
        */
        test("0 .123 3E 123 123E0F 3e+xy 123LU 0.123E-005 2for1",
                TokenType.NUMBER);
    }

    @Test
    public void ppNumbers2() {
        /*
            314          3.14           .314E+1
            0xa5         .14E+          1z2z
        */
        test("314 3.14 .314E+1 0xa5 .14E+ 1z2z", TokenType.NUMBER);
    }

    @Test
    public void ppNumbers3() {
        test("0x0e+1", TokenType.NUMBER);
    }

    @Test
    public void ppNumbers4() {
        var s = "0x0f+1";
        var list = List.of(pair("0x0f", TokenType.NUMBER),
                pair("+", TokenType.OPERATOR),
                pair("1", TokenType.NUMBER));
        test(s, list);
    }

    @Test
    public void ppNumbers5() {
        test("0x0.3p10 0x0.3p+10 0x0.3p-10", TokenType.NUMBER);
    }

    @Test
    public void universalCharacterNameUpperUFirst() {
        var s = """
            char *\\U0001f431s = "cats";
            """.trim();
        var list = List.of(pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("\\U0001f431s", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"cats\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void universalCharacterNameUpperU() {
        var s = """
            char *big\\U0001f431s = "bigCats";
            """.trim();
        var list = List.of(pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("big\\U0001f431s", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"bigCats\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void universalCharacterNameLowerUFirst() {
        var s = """
            char *\\u732bs = "cats";
            """.trim();
        var list = List.of(pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("\\u732bs", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"cats\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void universalCharacterNameLowerU() {
        var s = """
            char *big\\u732bs = "bigCats";
            """.trim();
        var list = List.of(pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("big\\u732bs", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"bigCats\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void implementationDefinedCharactersFirst() {
        var s = """
            char *猫s = "cats";
            """.trim();
        var list = List.of(pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("猫s", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"cats\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void implementationDefinedCharacters() {
        var s = """
            char *big猫s = "bigCats";
            """.trim();
        var list = List.of(pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("big猫s", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"bigCats\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR));
        test(s, list);
    }

    @Test
    public void implementationDefinedCharacters0() {
        test("テスト _テスト 𐌀𐌁𐌂 _𐌀𐌁𐌂", TokenType.IDENTIFIER);
    }

    @Test
    public void implementationDefinedCharacters1() {
        var s = "𐌀𐌁𐌂";
        test(s, parser -> {
            {
                var maybeToken = parser.next();
                assertThat(maybeToken.isPresent(), is(true));
                var token = maybeToken.get();
                assertThat(token.getType(), is(TokenType.IDENTIFIER));
                assertThat(token.getValue(), is(s));
                assertThat(token.getSpan().toString(), is("L1:1--3"));
                var chars = token.getChars();
                assertThat(chars.size(), is(6));
                var spanList = chars.stream()
                        .map(c -> c.getSpan().toString())
                        .collect(Collectors.toList());
                var expectedList = List.of(
                        "L1:1", "L1:1", "L1:2", "L1:2", "L1:3", "L1:3");
                assertThat(spanList, is(expectedList));
            }
            {
                var maybeToken = parser.next();
                assertThat(maybeToken.isEmpty(), is(true));
            }
        });
    }

    @Test
    public void concatNewlineFollowedByEof() {
        var s = """
                main\\
                \\
                """;
        test(s, parser -> {
            {
                var maybeToken = parser.next();
                assertThat(maybeToken.isPresent(), is(true));
                var token = maybeToken.get();
                assertThat(token.getType(), is(TokenType.IDENTIFIER));
                assertThat(token.getValue(), is("main"));
            }
            {
                var maybeToken = parser.next();
                assertThat(maybeToken.isEmpty(), is(true));
                var maybeEof = parser.getEof();
                assert maybeEof.isPresent();
                var eof = maybeEof.get();
                assert eof.isEof();
                var list = eof.getChildren();
                assertThat(list.size(), is(4));
                assertThat(eof.getSpan().toString(), is("L1:5--L2:2"));
            }
        });
    }

    private static void test(String s, ParserConsumer consumer) {
        var source = new StringReader(s);
        test(consumer, () -> LexicalParser.of(source));
    }

    private static void test(String s, ParserConsumer consumer,
                             Collection<String> keywords) {
        var source = new StringReader(s);
        test(consumer, () -> LexicalParser.of(source, keywords));
    }

    private static void test(ParserConsumer consumer,
                             Supplier<LexicalParser> supplier) {
        try (var parser = supplier.get()) {
            consumer.accept(parser);
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    private static void test(String s, List<Consumer<Token>> list) {
        test(s, parser -> {
            for (var c : list) {
                var maybeToken = parser.next();
                assertThat(maybeToken.isPresent(), is(true));
                var token = maybeToken.get();
                c.accept(token);
            }
            {
                var maybeToken = parser.next();
                assertThat(maybeToken.isEmpty(), is(true));
            }
        });
    }

    private static void test(String s, TokenType expectedType) {
        var expectedList = Arrays.stream(s.split(" "))
                .collect(Collectors.toList());
        test(s, parser -> {
            var list = new ArrayList<String>();
            for (;;) {
                var maybeToken = parser.next();
                if (maybeToken.isEmpty()) {
                    break;
                }
                var token = maybeToken.get();
                var type = token.getType();
                if (type == TokenType.DELIMITER) {
                    assertThat(token.getValue(), is(" "));
                    continue;
                }
                assertThat(type, is(expectedType));
                list.add(token.getValue());
            }
            assertThat(list, equalTo(expectedList));
        });
    }

    private static void test(Collection<SourceChar> all,
                             List<Consumer<SourceChar>> list) {
        var i = all.iterator();
        for (var c : list) {
            assertThat(i.hasNext(), is(true));
            c.accept(i.next());
        }
        assertThat(i.hasNext(), is(false));
    }

    private static Consumer<Token> pair(String value, TokenType type) {
        return t -> {
            assertThat(t.getValue(), is(value));
            assertThat(t.getType(), is(type));
        };
    }

    private static Consumer<Token> pair(String value, TokenType type,
                                        List<Consumer<Token>> childList) {
        return t -> {
            assertThat(t.getValue(), is(value));
            assertThat(t.getType(), is(type));
            var children = t.getChildren();
            var size = children.size();
            assertThat(size, is(childList.size()));
            for (var k = 0; k < size; ++k) {
                childList.get(k).accept(children.get(k));
            }
        };
    }

    private static Consumer<SourceChar> pair(char value, String span) {
        return c -> {
            assertThat(c.toChar(), is(value));
            assertThat(c.getSpan().toString(), is(span));
            assertThat(c.getChildren().isEmpty(), is(true));
            assertThat(c.isEof(), is(false));
        };
    }

    private static Consumer<SourceChar> pair(char value, String span,
                                             List<Consumer<SourceChar>> list) {
        return c -> {
            assertThat(c.toChar(), is(value));
            assertThat(c.getSpan().toString(), is(span));
            var children = c.getChildren();
            assertThat(children.isEmpty(), is(false));
            test(children, list);
            assertThat(c.isEof(), is(false));
        };
    }

    @FunctionalInterface
    public interface ParserConsumer {
        void accept(LexicalParser parser) throws IOException;
    }
}
