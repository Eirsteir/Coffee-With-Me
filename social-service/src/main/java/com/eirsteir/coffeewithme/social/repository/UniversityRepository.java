package com.eirsteir.coffeewithme.social.repository;

import com.eirsteir.coffeewithme.social.domain.university.University;
import com.eirsteir.coffeewithme.social.dto.NoCampusUniversity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UniversityRepository extends JpaRepository<University, Long> {

    @Query("SELECT university.id as id, university.name as name FROM University university")
    List<NoCampusUniversity> findAllExcludeCampuses();

}
