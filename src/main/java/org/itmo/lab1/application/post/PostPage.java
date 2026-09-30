package org.itmo.lab1.application.post;

import java.util.List;
import java.util.Optional;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostId;

public record PostPage(List<Post> items, PostId nextCursor) {

    public PostPage {
        items = List.copyOf(items);
    }

    public Optional<PostId> next() {
        return Optional.ofNullable(nextCursor);
    }
}
