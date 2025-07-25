package com.lisp.lispaiagent.agent.model;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类  
 */  
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {  
  
    // 可用的工具  
    private final ToolCallback[] availableTools;
  
    // 保存了工具调用信息的响应  
    private ChatResponse toolCallChatResponse;
  
    // 工具调用管理者  
    private final ToolCallingManager toolCallingManager;
  
    // 禁用内置的工具调用机制，自己维护上下文  
    private final ChatOptions chatOptions;
  
    public ToolCallAgent(ToolCallback[] availableTools) {  
        super();  
        this.availableTools = availableTools;  
        this.toolCallingManager = ToolCallingManager.builder().build();  
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文  
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)  
                .build();  
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 检查下一步提示信息是否存在且不为空
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            // 若提示信息存在，创建一个用户消息对象
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            // 将用户消息添加到消息列表中
            getMessageList().add(userMessage);
        }
        // 获取当前的消息列表
        List<Message> messageList = getMessageList();
        // 使用消息列表和聊天选项创建一个提示对象
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            // 调用聊天客户端，根据提示对象发起请求，设置系统提示和可用工具，获取聊天响应
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            // 记录带工具调用信息的聊天响应，供 act 方法使用
            this.toolCallChatResponse = chatResponse;
            // 从聊天响应中获取助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 提取助手消息的文本内容
            String result = assistantMessage.getText();
            // 提取助手消息中的工具调用列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 记录当前代理的思考结果
            log.info(getName() + "的思考: " + result);
            // 记录当前代理选择使用的工具数量
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            // 拼接每个工具的名称和参数信息
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(),
                            toolCall.arguments())
                    )
                    .collect(Collectors.joining("\n"));
            // 记录每个工具的详细信息
            log.info(toolCallInfo);
            // 判断工具调用列表是否为空
            if (toolCallList.isEmpty()) {
                // 若列表为空，说明不调用工具，将助手消息添加到消息列表中
                getMessageList().add(assistantMessage);
                // 返回 false，表示不需要执行行动
                return false;
            } else {
                // 若列表不为空，说明需要调用工具，由于调用工具时会自动记录，此处不添加助手消息
                // 返回 true，表示需要执行行动
                return true;
            }
        } catch (Exception e) {
            // 若思考过程中出现异常，记录错误信息
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());
            // 将包含错误信息的助手消息添加到消息列表中
            getMessageList().add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            // 返回 false，表示不需要执行行动
            return false;
        }
    }


    /**
     * 执行工具调用并处理结果
     *
     * @return 执行工具调用后的结果字符串
     */
    @Override
    public String act() {
        // 检查聊天响应中是否包含工具调用信息
        if (!toolCallChatResponse.hasToolCalls()) {
            // 若不包含工具调用信息，直接返回提示信息
            return "没有工具调用";
        }
        // 基于当前消息列表和聊天选项创建提示对象
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        // 调用工具调用管理器执行工具调用，并获取工具执行结果
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 更新消息列表，将工具执行结果中的对话历史设置为当前消息列表
        setMessageList(toolExecutionResult.conversationHistory());
        // 从工具执行结果的对话历史中获取最后一条消息，即工具响应消息
        // 遍历工具响应消息中的所有响应，拼接每个工具的执行结果信息
        // 当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }
        log.info(results);
        return results;

    }

}
