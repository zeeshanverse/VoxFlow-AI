package com.voxflow.conversation;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Message {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private Conversation conversation;

    @Column(nullable=false)
    private String role;

    @Lob
    @Column(nullable=false, columnDefinition="TEXT")
    private String content;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    protected Message() {}
    public Message(String role, String content) { this.role=role; this.content=content; }

    public Long getId(){return id;}
    public String getRole(){return role;}
    public String getContent(){return content;}
    public Instant getCreatedAt(){return createdAt;}
    void setConversation(Conversation conversation){this.conversation=conversation;}
}
