package com.lisp.lispaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;

public class HttpAiInvoke {

    public static void main(String[] args) {
        // API Key
        String apiKey = TestApiKey.API_KEY; // 替换为实际的 API Key

        // 请求 URL
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        // 构造请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-plus");
        requestBody.put("messages", new JSONObject[] {
            new JSONObject().put("role", "system").put("content", "You are a helpful assistant."),
            new JSONObject().put("role", "user").put("content", "你是谁？")
        });

        // 发送 POST 请求
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .execute();

        // 输出响应结果
        System.out.println(response.body());
    }
}