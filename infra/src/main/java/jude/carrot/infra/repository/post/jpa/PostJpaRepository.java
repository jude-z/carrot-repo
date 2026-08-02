package jude.carrot.infra.repository.post.jpa;


import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.repository.post.dto.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostJpaRepository extends JpaRepository<Post,Long> {
    @Query("select distinct p from Post p " +
            "join fetch p.createdBy " +
            "left join fetch p.thumbnailImage " +
            "left join fetch p.contentImages " +
            "where p.id = :postId")
    Optional<Post> fetchJoinByPostId(@Param("postId") Long postId);

    @Query(value = "select new jude.carrot.infra.repository.post.dto.PostDto$PostElement(" +
            "p.id, p.title, p.price, p.address, p.createdBy.id, p.createdBy.email, p.thumbnailImage.url) " +
            "from Post p",
            countQuery = "select count(p) from Post p")
    Page<PostDto.PostElement> fetchJoinList(Pageable pageable);
}
