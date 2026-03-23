package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.ChatSession;
import com.yiwilee.aiqasystem.model.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatSessionRepo extends JpaRepository<ChatSession, Long> {
    @Query("SELECT s FROM ChatSession as s WHERE s.user.id = :userId")
    List<ChatSession> findAllByUserId(Long userId);

    List<ChatSession> findAllByUserIdOrderByUpdateTimeDesc(Long userId);

    @Modifying
    @Query("DELETE FROM ChatSession s WHERE s.user.id = :userId")
    int bulkDeleteByUserId(@Param("userId") Long userId);
}
