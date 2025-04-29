package com.lisp.lispaiagent.demo.invoke;


import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * SpringAi框架调用AI
 * 实现这个接口CommandLineRunner，可以实现一个单词执行的方法
 */
//@Component
public class SpringAiInvoke implements CommandLineRunner {

    //@Resource
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好，我是TImeHacker，我是一名AI爱好者，正在学习AI项目"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());

    }
}
