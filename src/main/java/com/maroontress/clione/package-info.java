/**
    This package provides an API of a lexical parser that tokenizes source code
    written in C17 and other C-like programming languages.

    <p>The main facility is a tokenization API corresponding to the C
    preprocessor layer. It includes the features of trigraph replacement, line
    splicing, and tokenization but does not include macro expansion and
    directive handling.</p>

    <p>A typical usage example would be as follows:</p>
    <pre>
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
    }</pre>

    <p>And {@code helloworld.c} would be as follows:</p>
    <pre>
    #include &lt;stdio.h&gt;

    int main(void)
    {
        printf("hello world\n");
    }</pre>

    <p>In this example, the result of
    "{@code java com.example.TokenDemo helloworld.c}" is as follows:</p>

    <pre>
    L1:1--19: DIRECTIVE: #
    | L1:2--8: DIRECTIVE_NAME: include
    | L1:9: DELIMITER: ' '
    | L1:10--18: STANDARD_HEADER: &lt;stdio.h&gt;
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
    L6:2: DELIMITER: '\n'</pre>

    @see com.maroontress.clione.LexicalParser
    @see com.maroontress.clione.Token
*/
package com.maroontress.clione;
