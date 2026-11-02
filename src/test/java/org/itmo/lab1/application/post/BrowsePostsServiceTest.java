package org.itmo.lab1.application.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import org.itmo.lab1.application.port.out.PostRepository;
import org.itmo.lab1.domain.DomainValidationException;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.post.PostId;
import org.itmo.lab1.domain.user.Username;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrowsePostsServiceTest {

    @Mock
    private PostRepository posts;

    @Test
    void returnsRequestedSizeAndCursorWhenMorePostsExist() {
        when(posts.findLatest(3)).thenReturn(postsWithIds(10, 9, 8));

        PostPage page = new BrowsePostsService(posts).latest(new PageSize(2));

        assertThat(page.items()).extracting(Post::id).containsExactly(new PostId(10), new PostId(9));
        assertThat(page.next()).contains(new PostId(9));
    }

    @Test
    void lastPageHasNoCursor() {
        when(posts.findOlderThan(new PostId(9), 3)).thenReturn(postsWithIds(8));

        PostPage page = new BrowsePostsService(posts).olderThan(new PostId(9), new PageSize(2));

        assertThat(page.items()).extracting(Post::id).containsExactly(new PostId(8));
        assertThat(page.next()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, PageSize.MAX + 1, Integer.MAX_VALUE})
    void pageSizeIsBounded(int size) {
        assertThatThrownBy(() -> new PageSize(size))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("limit");
    }

    private static List<Post> postsWithIds(long... ids) {
        return LongStream.of(ids)
                .mapToObj(id -> new Post(new PostId(id), new Username("alice"), new PostContent("post " + id),
                        Instant.parse("2026-09-30T12:00:00Z")))
                .toList();
    }
}
