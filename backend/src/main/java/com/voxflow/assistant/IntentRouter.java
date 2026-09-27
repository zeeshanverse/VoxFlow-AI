package com.voxflow.assistant;
import org.springframework.stereotype.Service;
@Service
public class IntentRouter {
    public String route(String text){
        String v=text==null?"":text.toLowerCase();
        if(v.contains("start interview")||v.contains("begin interview")) return "START_INTERVIEW";
        if(v.contains("history")||v.contains("previous conversation")) return "SHOW_HISTORY";
        if(v.contains("what time")||v.contains("current time")) return "CURRENT_TIME";
        return "GENERAL_QUERY";
    }
}
