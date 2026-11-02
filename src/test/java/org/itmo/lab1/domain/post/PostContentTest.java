package org.itmo.lab1.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.itmo.lab1.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PostContentTest {

    private static final String EMOJI = "\uD83D\uDE00";

    @Test
    void storesMarkupVerbatimBecauseXssIsHandledByOutputEncoding() {
        String markup = "<script>alert(document.cookie)</script><img src=x onerror=alert(1)>";

        assertThat(new PostContent(markup).value()).isEqualTo(markup);
    }

    @Test
    void acceptsMultilineUnicodeText() {
        String text = "Привет,\tмир!\r\nSecond line " + EMOJI;

        assertThat(new PostContent(text).value()).isEqualTo(text);
    }

    @Test
    void countsLengthInCodePointsLikePostgresVarchar() {
        assertThat(new PostContent(EMOJI.repeat(PostContent.MAX_LENGTH)).value()).hasSize(2 * PostContent.MAX_LENGTH);

        assertThatThrownBy(() -> new PostContent("a".repeat(PostContent.MAX_LENGTH + 1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("at most 2000");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void rejectsBlank(String value) {
        assertThatThrownBy(() -> new PostContent(value)).isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nul\u0000byte",
            "bell\u0007",
            "next\u0085line",
            "evil\u202Etxt.exe",
            "isolate\u2066x\u2069",
            "lone\uD800surrogate"
    })
    void rejectsControlBidiAndInvalidUnicodeWithoutEchoingIt(String value) {
        assertThatThrownBy(() -> new PostContent(value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageNotContaining(value);
    }
}
