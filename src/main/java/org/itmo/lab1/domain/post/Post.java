package org.itmo.lab1.domain.post;

import java.time.Instant;
import java.util.Objects;
import org.itmo.lab1.domain.user.Username;

public record Post(PostId id, Username author, PostContent content, Instant createdAt) {

    public Post {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
