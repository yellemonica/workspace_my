package com.example.studentmanagement.student;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("""
            select s from Student s
            where lower(s.firstName) like :pattern escape '\\'
               or lower(s.lastName) like :pattern escape '\\'
               or lower(concat(s.firstName, ' ', s.lastName)) like :pattern escape '\\'
               or lower(s.email) like :pattern escape '\\'
            """)
    Page<Student> search(@Param("pattern") String pattern, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Student s where s.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
