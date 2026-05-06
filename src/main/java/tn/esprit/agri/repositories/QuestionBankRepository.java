package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.QuestionBank;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, String> {
    
    long countByCropType(String cropType);

    // Fetch random questions for a specific crop type
    @Query(value = "SELECT * FROM question_bank WHERE crop_type = :cropType ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuestionBank> findRandomByCropType(String cropType, int limit);
}
