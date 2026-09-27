package com.voxflow.conversation;

import com.voxflow.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Conversation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private User user;

    @Column(nullable=false)
    private String title;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy="conversation", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    protected Conversation() {}
    public Conversation(User user, String title) { this.user=user; this.title=title; }

    public Long getId(){return id;}
    public User getUser(){return user;}
    public String getTitle(){return title;}
    public List<Message> getMessages(){return messages;}
    public void addMessage(Message m){ messages.add(m); m.setConversation(this); }
}
