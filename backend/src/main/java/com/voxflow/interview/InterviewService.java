package com.voxflow.interview;

import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxflow.common.CurrentUser;
import com.voxflow.llm.LlmProvider;
import com.voxflow.voice.SpeechToTextProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class InterviewService {
    private static final List<String> QUESTIONS=List.of(
        "Explain the four pillars of object oriented programming in Java.",
        "What is the difference between HashMap and ConcurrentHashMap?",
        "Explain how Spring dependency injection works.",
        "What is the difference between checked and unchecked exceptions?",
        "Explain the difference between JDBC, JPA and Spring Data JPA."
    );
    private final InterviewSessionRepository sessions; private final CurrentUser current; private final SpeechToTextProvider stt; private final LlmProvider llm; private final ObjectMapper mapper=new ObjectMapper();
    public InterviewService(InterviewSessionRepository sessions,CurrentUser current,SpeechToTextProvider stt,LlmProvider llm){
        this.sessions=sessions;this.current=current;this.stt=stt;this.llm=llm;
    }

    public Start start(String topic){
        var s=sessions.save(new InterviewSession(current.get(),topic==null||topic.isBlank()?"Java Backend":topic));
        return new Start(s.getId(),QUESTIONS.get(0),QUESTIONS.size());
    }
    @Transactional 
    public AnswerResult answer(Long id, MultipartFile audio){
        var s=sessions.findByIdAndUserId(id,current.get().getId()).orElseThrow();
        if(s.isCompleted()) throw new IllegalStateException("Interview already completed");
        int index=s.getCurrentQuestion();
        if(index>=QUESTIONS.size()) throw new IllegalStateException("No question remaining");
        String answer=stt.transcribe(audio);
        String question=QUESTIONS.get(index);
        String raw=llm.generate(
            "You are a strict but fair Java backend interviewer. Evaluate the candidate answer. Return JSON only with numeric score from 0 to 10 and concise feedback.",
            "Question: "+question+"\\nCandidate answer: "+answer+
            "\\nReturn exactly: {\"score\":7.5,\"feedback\":\"...\"}");
        double score=5; String feedback=raw;
        try{
            JsonNode n=mapper.readTree(extractJson(raw));
            score=n.path("score").asDouble(5); feedback=n.path("feedback").asText(raw);
        }catch(Exception ignored){}
        s.addAnswer(new InterviewAnswer(question,answer,score,feedback)); s.incrementQuestion();
        boolean done=s.getCurrentQuestion()>=QUESTIONS.size();
        if(done){
            double avg=s.getAnswers().stream().mapToDouble(InterviewAnswer::getScore).average().orElse(0);
            s.complete(avg);
        }
        sessions.save(s);
        String next=done?null:QUESTIONS.get(s.getCurrentQuestion());
        return new AnswerResult(s.getId(),answer,score,feedback,next,done,s.getScore());
    }
    @Transactional 
    public SessionResult get(Long id){
        var s=sessions.findByIdAndUserId(id,current.get().getId()).orElseThrow();
        return new SessionResult(s.getId(),s.getTopic(),s.isCompleted(),s.getScore(),
            s.getAnswers().stream().map(a->new AnswerView(a.getQuestion(),a.getAnswer(),a.getScore(),a.getFeedback())).toList());
    }
    private String extractJson(String s){
        int a=s.indexOf('{'),b=s.lastIndexOf('}');
        return a>=0&&b>a?s.substring(a,b+1):s;
    }
    public record Start(Long id,String question,int totalQuestions){}
    public record AnswerResult(Long id,String transcript,double score,String feedback,String nextQuestion,boolean completed,double finalScore){}
    public record SessionResult(Long id,String topic,boolean completed,double score,List<AnswerView> answers){}
    public record AnswerView(String question,String answer,double score,String feedback){}
}
