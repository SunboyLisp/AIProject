package com.lisp.lispaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class OllamaAInvoke implements CommandLineRunner {

    //@Resource
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = ollamaChatModel.call(new Prompt("你好，我是TimeHacker，我是一名AI爱好者，正在学习AI项目"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());

    }
}
