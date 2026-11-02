package org.itmo.lab1.infrastructure.web.post;

import java.time.Instant;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.infrastructure.web.HtmlOutput;

public record PostResponse(long id, String author, String content, Instant createdAt) {

    static PostResponse from(Post post) {
        return new PostResponse(
                post.id().value(),
                HtmlOutput.encode(post.author().value()),
                HtmlOutput.encode(post.content().value()),
                post.createdAt());
    }
}
