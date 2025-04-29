package com.lisp.lispaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

public class LangChainAiInvoke {
    public static void main(String[] args) {
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();
        String chat = qwenChatModel.chat("你好，我是TImeHacker，我是一名AI爱好者，正在学习AI项目");
        System.out.println(chat);
    }
}
