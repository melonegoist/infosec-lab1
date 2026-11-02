package org.itmo.lab1.infrastructure.web.post;

import org.itmo.lab1.application.port.in.BrowsePostsUseCase;
import org.itmo.lab1.application.port.in.CreatePostUseCase;
import org.itmo.lab1.application.post.PageSize;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.post.PostId;
import org.itmo.lab1.infrastructure.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PostController {

    private final CreatePostUseCase createPost;
    private final BrowsePostsUseCase browsePosts;

    public PostController(CreatePostUseCase createPost, BrowsePostsUseCase browsePosts) {
        this.createPost = createPost;
        this.browsePosts = browsePosts;
    }

    @PostMapping(path = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@AuthenticationPrincipal(errorOnInvalidType = true) AuthenticatedUser user,
                               @RequestBody CreatePostRequest request) {
        return PostResponse.from(createPost.create(user.id(), new PostContent(request.content())));
    }

    @GetMapping(path = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public PostPageResponse list(@RequestParam(required = false) Integer limit,
                                 @RequestParam(required = false) Long before) {
        PageSize size = limit == null ? PageSize.defaultSize() : new PageSize(limit);
        return PostPageResponse.from(before == null
                ? browsePosts.latest(size)
                : browsePosts.olderThan(new PostId(before), size));
    }
}
