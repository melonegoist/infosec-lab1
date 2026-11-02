package org.itmo.lab1.infrastructure.persistence.post;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface PostJpaRepository extends Repository<PostEntity, Long> {

    PostEntity save(PostEntity post);

    @EntityGraph(attributePaths = "author")
    List<PostEntity> findAllByOrderByIdDesc(Limit limit);

    @EntityGraph(attributePaths = "author")
    List<PostEntity> findByIdLessThanOrderByIdDesc(long id, Limit limit);
}
