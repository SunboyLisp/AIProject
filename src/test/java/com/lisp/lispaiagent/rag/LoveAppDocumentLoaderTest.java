package com.lisp.lispaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppDocumentLoaderTest {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;
    @Test
    void loadDocuments() {
        try {
            loveAppDocumentLoader.loadMarkdowns();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}