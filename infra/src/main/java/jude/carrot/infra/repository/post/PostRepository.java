package jude.carrot.infra.repository.post;


import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.repository.post.dto.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

import static jude.carrot.infra.repository.post.dto.PostDto.*;

public interface PostRepository {

    Optional<Post> fetchJoinByPostId(Long postId);

    Page<PostElement> fetchJoinList(Pageable pageable);

    Optional<Post> findById(Long postId);

    void save(Post post);

    void deleteById(Long postId);
}
