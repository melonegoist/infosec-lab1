package org.itmo.lab1.infrastructure.persistence.post;

import java.sql.SQLException;
import java.util.List;
import org.itmo.lab1.application.port.out.PostRepository;
import org.itmo.lab1.application.port.out.UnknownAuthorException;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.post.PostId;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.persistence.user.UserJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaPostRepositoryAdapter implements PostRepository {

    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final PostJpaRepository posts;
    private final UserJpaRepository users;

    public JpaPostRepositoryAdapter(PostJpaRepository posts, UserJpaRepository users) {
        this.posts = posts;
        this.users = users;
    }

    @Override
    @Transactional
    public Post create(UserId author, PostContent content) {
        try {
            PostEntity saved = posts.save(new PostEntity(users.getReferenceById(author.value()), content.value()));
            return toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            if (FOREIGN_KEY_VIOLATION.equals(sqlState(e))) {
                throw new UnknownAuthorException();
            }
            throw e;
        }
    }

    private static String sqlState(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findLatest(int limit) {
        return posts.findAllByOrderByIdDesc(Limit.of(limit)).stream().map(JpaPostRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findOlderThan(PostId cursor, int limit) {
        return posts.findByIdLessThanOrderByIdDesc(cursor.value(), Limit.of(limit)).stream()
                .map(JpaPostRepositoryAdapter::toDomain)
                .toList();
    }

    private static Post toDomain(PostEntity entity) {
        return new Post(new PostId(entity.getId()), new Username(entity.getAuthor().getUsername()),
                new PostContent(entity.getContent()), entity.getCreatedAt());
    }
}
