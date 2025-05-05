package com.lisp.lispaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆实现类，实现了 ChatMemory 接口。
 * 该类使用 Kryo 序列化库将对话消息存储到文件系统中，
 * 可以对不同会话 ID 的对话消息进行添加、获取和清除操作。
 */
public class FileBasedChatMemory implements ChatMemory {

    /**
     * 对话消息文件存储的基础目录。
     */
    private final String BASE_DIR;

    /**
     * Kryo 序列化实例，用于对象的序列化和反序列化操作。
     */
    private static final Kryo kryo = new Kryo();

    static {
        // 关闭类注册要求，允许在序列化和反序列化时动态处理类
        kryo.setRegistrationRequired(false);
        // 设置实例化策略，使用标准实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    /**
     * 构造函数，初始化文件存储的基础目录。
     * 如果指定的目录不存在，会自动创建该目录。
     *
     * @param dir 对话消息文件存储的基础目录路径
     */
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        // 检查目录是否存在，若不存在则创建
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * 向指定会话 ID 的对话中添加消息列表。
     * 首先获取该会话已有的消息列表，然后将新消息添加到列表中，
     * 最后将更新后的消息列表保存到文件中。
     *
     * @param conversationId 会话的唯一标识符
     * @param messages 需要添加的消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }

    /**
     * 获取指定会话 ID 的对话中最近的 N 条消息。
     * 首先获取该会话的所有消息，然后通过流操作截取最近的 N 条消息。
     *
     * @param conversationId 会话的唯一标识符
     * @param lastN 需要获取的最近消息数量
     * @return 包含最近 N 条消息的列表
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> allMessages = getOrCreateConversation(conversationId);
        return allMessages.stream()
                .skip(Math.max(0, allMessages.size() - lastN))
                .toList();
    }

    /**
     * 清除指定会话 ID 的对话消息。
     * 通过删除对应的消息文件来实现清除操作。
     *
     * @param conversationId 会话的唯一标识符
     */
    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        // 检查文件是否存在，若存在则删除
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取指定会话 ID 的对话消息列表。
     * 如果对应的消息文件存在，则从文件中反序列化消息列表；
     * 若文件不存在，则返回一个空的消息列表。
     *
     * @param conversationId 会话的唯一标识符
     * @return 包含该会话消息的列表
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                // 从文件中反序列化消息列表
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    /**
     * 将指定会话 ID 的对话消息列表保存到文件中。
     * 使用 Kryo 序列化库将消息列表序列化到对应的文件中。
     *
     * @param conversationId 会话的唯一标识符
     * @param messages 需要保存的消息列表
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            // 将消息列表序列化到文件中
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定会话 ID 对应的消息文件。
     * 文件路径为基础目录加上会话 ID 拼接的文件名（后缀为 .kryo）。
     *
     * @param conversationId 会话的唯一标识符
     * @return 对应的消息文件对象
     */
    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
