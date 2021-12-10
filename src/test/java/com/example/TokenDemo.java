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
