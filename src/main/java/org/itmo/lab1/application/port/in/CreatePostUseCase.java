package org.itmo.lab1.application.port.in;

import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.user.UserId;

public interface CreatePostUseCase {

    Post create(UserId author, PostContent content);
}
