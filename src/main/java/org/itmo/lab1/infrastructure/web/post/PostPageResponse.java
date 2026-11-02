package org.itmo.lab1.infrastructure.web.post;

import java.util.List;
import org.itmo.lab1.application.post.PostPage;
import org.itmo.lab1.domain.post.PostId;

public record PostPageResponse(List<PostResponse> items, Long nextCursor) {

    public PostPageResponse {
        items = List.copyOf(items);
    }

    static PostPageResponse from(PostPage page) {
        return new PostPageResponse(
                page.items().stream().map(PostResponse::from).toList(),
                page.next().map(PostId::value).orElse(null));
    }
}
