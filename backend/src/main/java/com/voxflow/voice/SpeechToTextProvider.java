package com.voxflow.voice;
import org.springframework.web.multipart.MultipartFile;
public interface SpeechToTextProvider { String transcribe(MultipartFile audio); }
