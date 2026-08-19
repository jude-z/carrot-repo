package jude.carrot.infra.repository.recover;

import jude.carrot.infra.entity.recover.RedisRecover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RedisRecoverRepository extends JpaRepository<RedisRecover, Long> {
    @Query("select r from RedisRecover r where r.status = true")
    List<RedisRecover> findAllByStatusTrue();
    @Modifying
    @Query("update RedisRecover r set r.status = false where r.id in :updateIds")
    void bulkUpdate(@Param("updateIds") List<Long> updateIds);
}
