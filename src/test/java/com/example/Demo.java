package com.example;

import com.maroontress.clione.LexicalParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Demo {

    @Test
    public void helloWorld() throws IOException {
        TokenDemo.run(of("helloworld.c"));
    }

    @Test
    public void trigraphAndLineConcat() throws IOException {
        SourceCharDemo.run(of("main.c"));
    }

    @Test
    public void surrogatePair() throws IOException {
        SourceCharDemo.run(of("emojicat.c"));
    }

    private LexicalParser of(String file) {
        var in = getClass().getResourceAsStream(file);
        assert in != null;
        var charSet = StandardCharsets.UTF_8;
        return LexicalParser.of(new InputStreamReader(in, charSet));
    }
}
