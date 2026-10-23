package com.shoppingmall.repository;

import com.shoppingmall.entity.NumberSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NumberSequence n where n.sequenceName = :name")
    Optional<NumberSequence> findBySequenceName(@Param("name") String name);
}
