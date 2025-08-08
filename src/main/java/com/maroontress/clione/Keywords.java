package com.maroontress.clione;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
    Provides the reserved words of C and the directive names.

    @see <a href="https://en.wikipedia.org/wiki/C_(programming_language)#Reserved_words">
    Wikipedia, Reserved words</a>
*/
public final class Keywords {

    /** The unmodifiable set of keywords defined in C89. */
    public static final Set<String> C89 = C89Keywords.ALL;

    /** The unmodifiable set of keywords defined in C99. */
    public static final Set<String> C99 = C99Keywords.ALL;

    /** The unmodifiable set of keywords defined in C11. */
    public static final Set<String> C11 = C11Keywords.ALL;

    /** The unmodifiable set of preprocessing directive names. */
    public static final Set<String> PP_DIRECTIVE_NAMES = Set.of(
            "include",
            "define",
            "undef",
            "if", "ifdef", "ifndef", "elif", "else", "endif",
            "line",
            "error",
            "pragma");

    /** Prevents the class from being instantiated. */
    private Keywords() {
        throw new AssertionError();
    }

    private static <T> Set<T> union(Set<T> s1, Set<T> s2) {
        return Stream.concat(s1.stream(), s2.stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static final class C89Keywords {
        private static final Set<String> ALL = Set.of("auto", "break", "case",
                "char", "const", "continue", "default", "do", "double", "else",
                "enum", "extern", "float", "for", "goto", "if", "int", "long",
                "register", "return", "short", "signed", "sizeof", "static",
                "struct", "switch", "typedef", "union", "unsigned", "void",
                "volatile", "while");
    }

    private static final class C99Keywords {
        private static final Set<String> ALL = union(C89Keywords.ALL, Set.of(
                "_Bool", "_Complex", "_Imaginary", "inline", "restrict"));
    }

    private static final class C11Keywords {
        private static final Set<String> ALL = union(C99Keywords.ALL, Set.of(
                "_Alignas", "_Alignof", "_Atomic", "_Generic", "_Noreturn",
                "_Static_assert", "_Thread_local"));
    }
}
