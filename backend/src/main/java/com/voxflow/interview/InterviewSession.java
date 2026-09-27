package com.voxflow.interview;

import com.voxflow.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class InterviewSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private User user;

    @Column(nullable=false)
    private String topic;

    @Column(nullable=false)
    private int currentQuestion = 0;

    @Column(nullable=false)
    private boolean completed = false;

    @Column(nullable=false)
    private double score = 0;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy="session", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<InterviewAnswer> answers = new ArrayList<>();

    protected InterviewSession() {}
    public InterviewSession(User user, String topic){this.user=user;this.topic=topic;}
    public Long getId(){return id;}
    public User getUser(){return user;}
    public String getTopic(){return topic;}
    public int getCurrentQuestion(){return currentQuestion;}
    public boolean isCompleted(){return completed;}
    public double getScore(){return score;}
    public List<InterviewAnswer> getAnswers(){return answers;}
    public void incrementQuestion(){currentQuestion++;}
    public void complete(double score){this.completed=true;this.score=score;}
    public void addAnswer(InterviewAnswer a){answers.add(a);a.setSession(this);}
}
