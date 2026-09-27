package com.voxflow.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;
import java.util.Map;

@Service
public class AssemblyAISpeechToTextProvider implements SpeechToTextProvider {
    private final WebClient client; private final String key; private final ObjectMapper mapper=new ObjectMapper();

    public AssemblyAISpeechToTextProvider(WebClient client,@Value("${assemblyai.api-key:}") String key){this.client=client;this.key=key;}

    public String transcribe(MultipartFile audio){
        if(key==null||key.isBlank()) throw new IllegalStateException("ASSEMBLYAI_API_KEY is not configured");
        try{
            String upload=client.post().uri("https://api.assemblyai.com/v2/upload").header("authorization",key)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).bodyValue(audio.getBytes())
                    .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(60));
            String uploadUrl=mapper.readTree(upload).path("upload_url").asText();

            String job=client.post().uri("https://api.assemblyai.com/v2/transcript").header("authorization",key)
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("audio_url",uploadUrl))
                    .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(60));
            String id=mapper.readTree(job).path("id").asText();

            for(int i=0;i<120;i++){
                String result=client.get().uri("https://api.assemblyai.com/v2/transcript/"+id).header("authorization",key)
                        .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(60));
                JsonNode node=mapper.readTree(result);
                String status=node.path("status").asText();
                if("completed".equals(status)) return node.path("text").asText("");
                if("error".equals(status)) throw new IllegalStateException(node.path("error").asText("AssemblyAI transcription failed"));
                Thread.sleep(1000);
            }
            throw new IllegalStateException("AssemblyAI transcription timed out");
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Transcription interrupted",e);}
        catch(Exception e){throw new IllegalStateException("Could not transcribe audio",e);}
    }
}
