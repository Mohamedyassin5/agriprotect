package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "question_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String cropType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_bank_options", joinColumns = @JoinColumn(name = "question_bank_id"))
    private List<String> options;

    @Column(nullable = false)
    private String correctAnswer;
}
