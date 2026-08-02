package jude.carrot.infra.repository.post;

import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.repository.post.dto.PostDto;
import jude.carrot.infra.repository.post.jpa.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static jude.carrot.infra.repository.post.dto.PostDto.*;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;

    @Override
    public Optional<Post> fetchJoinByPostId(Long postId) {
        return postJpaRepository.fetchJoinByPostId(postId);
    }

    @Override
    public Page<PostElement> fetchJoinList(Pageable pageable) {
        return postJpaRepository.fetchJoinList(pageable);
    }

    @Override
    public Optional<Post> findById(Long postId) {
        return postJpaRepository.findById(postId);
    }

    @Override
    public void save(Post post) {
        postJpaRepository.save(post);
    }

    @Override
    public void deleteById(Long postId) {
        postJpaRepository.deleteById(postId);
    }
}
