package org.itmo.lab1.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.itmo.lab1.application.port.out.PostRepository;
import org.itmo.lab1.application.port.out.UnknownAuthorException;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.support.PostgresJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PostgresJpaTest
class PostPersistenceTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private PostRepository posts;

    private User author;

    @BeforeEach
    void createAuthor() {
        author = users.create(new Username("author"),
                new PasswordHash("{argon2}$argon2id$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaGhhc2g"));
    }

    @Test
    void storesUserContentVerbatimIncludingMarkupAndUnicode() {
        String content = "<script>alert(1)</script>\nПривет \uD83D\uDE00";

        Post created = posts.create(author.id(), new PostContent(content));

        assertThat(posts.findLatest(1)).singleElement().satisfies(post -> {
            assertThat(post.id()).isEqualTo(created.id());
            assertThat(post.author()).isEqualTo(new Username("author"));
            assertThat(post.content().value()).isEqualTo(content);
        });
    }

    @Test
    void keysetPaginationReturnsNewestFirstWithoutGapsOrOverlaps() {
        List<Post> created = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> posts.create(author.id(), new PostContent("post " + i)))
                .toList();

        List<Post> firstPage = posts.findLatest(2);
        List<Post> secondPage = posts.findOlderThan(firstPage.getLast().id(), 2);
        List<Post> lastPage = posts.findOlderThan(secondPage.getLast().id(), 2);

        assertThat(firstPage).extracting(Post::id).containsExactly(created.get(4).id(), created.get(3).id());
        assertThat(secondPage).extracting(Post::id).containsExactly(created.get(2).id(), created.get(1).id());
        assertThat(lastPage).extracting(Post::id).containsExactly(created.get(0).id());
    }

    @Test
    void authorMustExist() {
        assertThatThrownBy(() -> posts.create(new UserId(Long.MAX_VALUE), new PostContent("orphan")))
                .isInstanceOf(UnknownAuthorException.class);
    }
}
