package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.QcmQuestion;


@Repository
public interface QcmQuestionRepository extends JpaRepository<QcmQuestion, String> {
}