package com.lisp.lispaiagent.app;


import com.lisp.lispaiagent.advisor.MyLoggerAdvisor;
import com.lisp.lispaiagent.advisor.ReReadingAdvisor;
import com.lisp.lispaiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * LoveApp 类用于提供恋爱咨询相关的聊天服务，借助 Spring AI 框架与用户进行交互。
 */
@Component
@Slf4j
public class LoveApp {

    /**
     * 聊天客户端，用于与用户进行对话交互。
     * 该客户端基于传入的聊天模型构建，包含系统提示和会话记忆功能。
     */
    private final ChatClient chatClient;

    /**
     * 系统提示信息，定义了聊天机器人的角色和引导用户的提问内容。
     * 机器人将扮演深耕恋爱心理领域的专家，根据用户不同的情感状态进行针对性提问。
     */
    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";


    /**
     * LoveReport 是一个记录类，用于封装恋爱报告的信息。
     * 记录类是 Java 14 引入的一种特殊类，用于不可变的数据载体，
     * 该类自动生成构造函数、getter 方法、equals、hashCode 和 toString 方法。
     * 此类包含恋爱报告的标题和建议列表。
     */
    record LoveReport(
        /**
         * 恋爱报告的标题，通常包含用户相关信息，如 "{用户名}的恋爱报告"。
         */
        String title,
        /**
         * 恋爱建议列表，包含一系列针对用户恋爱情况给出的建议。
         */
        List<String> suggestions
    ) {
    }

    /**
     * 构造函数，初始化 LoveApp 实例。
     *
     * @param dashboardChatModel 用于构建聊天客户端的聊天模型
     */

    public LoveApp(ChatModel dashboardChatModel) {
        //初始化基于文件的会话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的会话记忆，用于存储聊天过程中的对话信息
        //ChatMemory chatMemory = new InMemoryChatMemory();
        // 使用传入的聊天模型构建聊天客户端，设置默认的系统提示信息和会话记忆顾问
        chatClient = ChatClient.builder(dashboardChatModel)
                // 设置默认的系统提示信息，明确聊天机器人的角色和引导提问内容
                .defaultSystem(SYSTEM_PROMPT)
                // 设置默认的顾问，使用消息聊天记忆顾问来管理会话记忆
                .defaultAdvisors(
                        // 初始化消息聊天记忆顾问，传入之前创建的会话记忆对象
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                        //new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * 执行聊天请求
     *
     * 该方法用于与指定的聊天机器人进行交互，发送用户消息并获取回复
     * 它不仅处理用户当前的输入信息，还通过聊天ID检索历史对话，以提供更连贯的对话体验
     *
     * @param message 用户输入的消息，用于聊天机器人的理解和回复生成
     * @param chatId 聊天会话的唯一标识符，用于跟踪和检索对话历史
     * @return 聊天机器人的回复内容
     */
    public String doChat(String message, String chatId) {
        // 构建聊天响应通过聊天客户端发送请求
        ChatResponse response = chatClient
                .prompt()  // 准备聊天提示
                .user(message)  // 设置用户输入的消息
                // 配置聊天顾问参数，包括聊天记忆的会话ID和检索的历史对话数量
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()  // 发起聊天请求
                .chatResponse();  // 获取聊天响应
        // 提取聊天结果中的文本内容
        String content = response.getResult().getOutput().getText();
        // 记录聊天内容日志
        log.info("content: {}", content);
        // 返回聊天内容
        return content;
    }

    /**
     * 根据用户消息和聊天ID生成恋爱报告
     * 该方法通过调用聊天客户端，根据给定的消息和聊天ID生成个性化的恋爱建议报告
     * 它配置了系统提示词，用户消息，并指定记忆参数以获 取相关的对话历史
     * 最后，它解析并返回一个LoveReport对象，其中包含为用户量身定制的恋爱建议
     *
     * @param message 用户的消息，用于生成报告的基础
     * @param chatId 聊天的唯一标识符，用于检索对话历史
     * @return LoveReport 包含用户恋爱建议的报告对象
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        // 调用聊天客户端的prompt方法开始构建提示词
        // 设置系统提示词，包括指示AI在每次对话后生成恋爱结果，以及报告的结构
        // 提交用户消息作为输入
        // 配置顾问参数，指定聊天记忆的对话ID和检索的对话历史数量
        // 执行调用并解析返回的实体为LoveReport类
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);

        // 记录生成的恋爱报告信息
        log.info("loveReport: {}", loveReport);

        // 返回生成的恋爱报告
        return loveReport;
    }

    @Resource
    private VectorStore loveAppVectorStore;

    /*public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }*/

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用增强检索服务（云知识库服务）
                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }



}