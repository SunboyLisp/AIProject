package com.lisp.lispaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

/**
 * 自定义日志 Advisor
 * 用于在 AI 请求和响应过程中打印 info 级别日志，仅输出单次用户提示词和 AI 回复的文本。
 * 实现了 CallAroundAdvisor 和 StreamAroundAdvisor 接口，分别处理普通调用和流式调用的日志记录。
 */
@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    PromptTemplate promptTemplate;
    /**
     * 获取当前 Advisor 的名称，使用类的简单名称作为标识。
     *
     * @return 当前类的简单名称
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取当前 Advisor 的执行顺序，数值越小优先级越高。
     *
     * @return 执行顺序，此处设置为 0
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 在请求处理前执行的方法，用于记录用户的提示词日志。
     *
     * @param request 包含用户提示词的请求对象
     * @return 原始请求对象
     */
    private AdvisedRequest before(AdvisedRequest request) {
        log.info("AI Request: {}", request.userText());
        return request;
    }

    /**
     * 在请求处理后执行的方法，用于记录 AI 回复的文本日志。
     *
     * @param advisedResponse 包含 AI 响应信息的对象
     */
    private void observeAfter(AdvisedResponse advisedResponse) {
        log.info("AI Response: {}", advisedResponse.response().getResult().getOutput().getText());
    }

    /**
     * 拦截普通调用请求，在请求处理前后记录日志。
     *
     * @param advisedRequest 包含用户提示词的请求对象
     * @param chain 用于调用下一个 Advisor 的链对象
     * @return 包含 AI 响应信息的对象
     */
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 在请求处理前记录用户提示词日志
        advisedRequest = this.before(advisedRequest);
        // 调用链中的下一个 Advisor 处理请求
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        // 在请求处理后记录 AI 回复日志
        this.observeAfter(advisedResponse);
        return advisedResponse;
    }

    /**
     * 拦截流式调用请求，在请求处理前后记录日志。
     *
     * @param advisedRequest 包含用户提示词的请求对象
     * @param chain 用于调用下一个 Advisor 的链对象
     * @return 包含 AI 响应信息的响应流
     */
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 在请求处理前记录用户提示词日志
        advisedRequest = this.before(advisedRequest);
        // 调用链中的下一个 Advisor 处理流式请求
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);
        // 使用 MessageAggregator 聚合响应流，并在聚合过程中记录 AI 回复日志
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}
