package com.voxflow.llm;

public interface LlmProvider {
    String generate(String systemInstruction, String input);
}
