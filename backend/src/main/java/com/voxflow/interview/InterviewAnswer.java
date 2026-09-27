package com.voxflow.interview;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class InterviewAnswer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private InterviewSession session;

    @Column(nullable=false, columnDefinition="TEXT")
    private String question;

    @Column(nullable=false, columnDefinition="TEXT")
    private String answer;

    @Column(nullable=false)
    private double score;

    @Column(nullable=false, columnDefinition="TEXT")
    private String feedback;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    protected InterviewAnswer(){}
    public InterviewAnswer(String question,String answer,double score,String feedback){
        this.question=question;this.answer=answer;this.score=score;this.feedback=feedback;
    }
    public Long getId(){return id;}
    public String getQuestion(){return question;}
    public String getAnswer(){return answer;}
    public double getScore(){return score;}
    public String getFeedback(){return feedback;}
    public Instant getCreatedAt(){return createdAt;}
    void setSession(InterviewSession s){session=s;}
}
