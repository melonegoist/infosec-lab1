package org.itmo.lab1.infrastructure.web;

import org.owasp.encoder.Encode;

public final class HtmlOutput {

    private HtmlOutput() {
    }

    public static String encode(String untrusted) {
        return Encode.forHtml(untrusted);
    }
}
