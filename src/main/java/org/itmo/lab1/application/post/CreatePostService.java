package org.itmo.lab1.application.post;

import org.itmo.lab1.application.port.in.CreatePostUseCase;
import org.itmo.lab1.application.port.out.PostRepository;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreatePostService implements CreatePostUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePostService.class);

    private final PostRepository posts;

    public CreatePostService(PostRepository posts) {
        this.posts = posts;
    }

    @Override
    public Post create(UserId author, PostContent content) {
        Post post = posts.create(author, content);
        log.info("Post created: postId={}, authorId={}", post.id().value(), author.value());
        return post;
    }
}
