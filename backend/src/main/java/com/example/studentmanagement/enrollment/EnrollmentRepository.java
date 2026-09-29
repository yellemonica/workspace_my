package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("""
            select e from Enrollment e
            join fetch e.course
            where e.student.id = :studentId
            order by e.enrolledAt, e.id
            """)
    List<Enrollment> findByStudentIdWithCourse(@Param("studentId") Long studentId);

    @Query("""
            select e.student from Enrollment e
            where e.course.id = :courseId
            order by e.student.lastName, e.student.firstName
            """)
    List<Student> findStudentsByCourseId(@Param("courseId") Long courseId);

    long countByCourseId(Long courseId);

    @Modifying
    @Query("delete from Enrollment e where e.course.id = :courseId")
    int deleteByCourseId(@Param("courseId") Long courseId);
}
