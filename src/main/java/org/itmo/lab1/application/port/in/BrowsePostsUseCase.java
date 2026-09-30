package org.itmo.lab1.application.port.in;

import org.itmo.lab1.application.post.PageSize;
import org.itmo.lab1.application.post.PostPage;
import org.itmo.lab1.domain.post.PostId;

public interface BrowsePostsUseCase {

    PostPage latest(PageSize size);

    PostPage olderThan(PostId cursor, PageSize size);
}
