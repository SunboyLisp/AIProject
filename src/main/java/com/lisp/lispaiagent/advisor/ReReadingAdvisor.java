package com.lisp.lispaiagent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力。通过在请求中添加重新阅读问题的提示，
 * 引导模型更加仔细地理解输入问题，从而提升推理的准确性。
 * 该类实现了 CallAroundAdvisor 和 StreamAroundAdvisor 接口，
 * 分别用于拦截普通调用和流式调用的请求。
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    /**
     * 在请求处理前对请求进行修改，添加重新阅读问题的提示。
     * 将原始用户文本存储到用户参数中，并修改用户文本以包含重新阅读问题的提示。
     *
     * @param advisedRequest 原始的请求对象，包含用户提示词和用户参数
     * @return 修改后的请求对象
     */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {
        // 复制原始的用户参数，避免修改原始对象
        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        // 将原始用户文本存储到用户参数中，键为 "re2_input_query"
        advisedUserParams.put("re2_input_query", advisedRequest.userText());

        // 基于原始请求对象创建新的请求对象，修改用户文本以包含重新阅读问题的提示
        return AdvisedRequest.from(advisedRequest)
                .userText("""
                        {re2_input_query}
                        Read the question again: {re2_input_query}
                        """)
                .userParams(advisedUserParams)
                .build();
    }

    /**
     * 拦截普通调用请求，在请求处理前对请求进行修改。
     *
     * @param advisedRequest 包含用户提示词的请求对象
     * @param chain 用于调用下一个 Advisor 的链对象
     * @return 包含 AI 响应信息的对象
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
    /**
     * 获取当前 Advisor 的名称，使用类的简单名称作为标识。
     *
     * @return 当前类的简单名称
     */
    /**
     * 获取当前 Advisor 的执行顺序，数值越小优先级越高。
     *
     * @return 执行顺序，此处设置为 0
     */
        // 在请求处理前对请求进行修改
    /**
     * 拦截流式调用请求，在请求处理前对请求进行修改。
     *
     * @param advisedRequest 包含用户提示词的请求对象
     * @param chain 用于调用下一个 Advisor 的链对象
     * @return 包含 AI 响应信息的响应流
     */
        // 在请求处理前对请求进行修改
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
