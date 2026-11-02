package org.itmo.lab1.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HtmlOutputTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(document.cookie)</script>",
            "\"><img src=x onerror=alert(1)>",
            "' autofocus onfocus='alert(1)",
            "<svg/onload=alert(1)>",
            "</textarea><script>alert(1)</script>",
            "<a href=\"javascript:alert(1)\">x</a>",
            "&lt;script&gt; (уже закодированное)"
    })
    void noHtmlSyntaxSurvivesEncoding(String payload) {
        String encoded = HtmlOutput.encode(payload);

        assertThat(encoded).doesNotContain("<", ">", "\"", "'");
        assertThat(encoded).doesNotContainPattern("&(?!(lt|gt|amp|#34|#39);)");
    }

    @Test
    void encodesToStandardEntities() {
        assertThat(HtmlOutput.encode("<b onclick=\"x\">'&'</b>"))
                .isEqualTo("&lt;b onclick=&#34;x&#34;&gt;&#39;&amp;&#39;&lt;/b&gt;");
    }

    @Test
    void leavesOrdinaryTextIntact() {
        assertThat(HtmlOutput.encode("Привет, мир! \uD83D\uDE00 2+2=4")).isEqualTo("Привет, мир! \uD83D\uDE00 2+2=4");
    }

    @Test
    void htmlEncodingIsNotAUrlDefense() {
        assertThat(HtmlOutput.encode("javascript:alert(1)")).isEqualTo("javascript:alert(1)");
    }
}
