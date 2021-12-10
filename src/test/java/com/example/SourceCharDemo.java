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
