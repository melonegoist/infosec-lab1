package org.itmo.lab1.application.post;

import java.util.List;
import org.itmo.lab1.application.port.in.BrowsePostsUseCase;
import org.itmo.lab1.application.port.out.PostRepository;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostId;

public final class BrowsePostsService implements BrowsePostsUseCase {

    private final PostRepository posts;

    public BrowsePostsService(PostRepository posts) {
        this.posts = posts;
    }

    @Override
    public PostPage latest(PageSize size) {
        return page(posts.findLatest(size.value() + 1), size);
    }

    @Override
    public PostPage olderThan(PostId cursor, PageSize size) {
        return page(posts.findOlderThan(cursor, size.value() + 1), size);
    }

    private static PostPage page(List<Post> fetched, PageSize size) {
        if (fetched.size() <= size.value()) {
            return new PostPage(fetched, null);
        }
        List<Post> items = fetched.subList(0, size.value());
        return new PostPage(items, items.getLast().id());
    }
}
