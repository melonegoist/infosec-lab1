package org.itmo.lab1.application.port.out;

import java.util.List;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.post.PostId;
import org.itmo.lab1.domain.user.UserId;

public interface PostRepository {

    Post create(UserId author, PostContent content);

    List<Post> findLatest(int limit);

    List<Post> findOlderThan(PostId cursor, int limit);
}
