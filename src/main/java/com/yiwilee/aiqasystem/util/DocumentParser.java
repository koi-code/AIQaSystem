package com.yiwilee.aiqasystem.util;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DocumentParser {

    private static final int MAX_CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;

    /**
     * 使用 Apache Tika 统一解析文档
     */
    public ParseResult parseWithPages(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        List<PageContent> pages = new ArrayList<>();
        StringBuilder fullContent = new StringBuilder();

        try (InputStream is = new FileInputStream(file)) {
            // 1. 初始化 Tika 解析器
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            // 2. 特殊配置：针对 PDF 开启分页标记
            PDFParserConfig pdfConfig = new PDFParserConfig();
            // 确保 Tika 会在页与页之间触发特定的 SAX 事件或插入换页符
            context.set(PDFParserConfig.class, pdfConfig);

            // 3. 使用自定义 Handler 捕获文本并识别“页”
            // 注意：对于非 PDF 文件，Tika 通常将其视为单一页（Page 1）
            PageAwareHandler handler = new PageAwareHandler();
            parser.parse(is, handler, metadata, context);

            // 4. 获取解析后的文本列表（按页拆分）
            List<String> rawPages = handler.getPageTexts();
            for (int i = 0; i < rawPages.size(); i++) {
                String pageText = rawPages.get(i).trim();
                if (StrUtil.isNotBlank(pageText)) {
                    int pageNum = i + 1;
                    fullContent.append(pageText).append("\n");

                    // 执行 RAG 切片逻辑
                    List<String> chunks = splitText(pageText);
                    for (String chunk : chunks) {
                        pages.add(new PageContent(pageNum, chunk));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Tika 解析文档失败: {}", filePath, e);
            throw new RuntimeException("文档解析失败", e);
        }

        return new ParseResult(fullContent.toString(), pages);
    }

    /**
     * 内部类：监听 Tika 的 SAX 事件来捕获分页内容
     * Tika 在解析 PDF 时，每一页通常包裹在 <div class="page"> 中
     */
    private static class PageAwareHandler extends DefaultHandler {
        private final List<StringBuilder> pages = new ArrayList<>();
        private StringBuilder currentPage = new StringBuilder();
        private boolean isInsidePage = false;

        public PageAwareHandler() {
            pages.add(currentPage); // 默认初始化第一页
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("div".equalsIgnoreCase(qName) && "page".equalsIgnoreCase(attributes.getValue("class"))) {
                if (currentPage.length() > 0) {
                    currentPage = new StringBuilder();
                    pages.add(currentPage);
                }
                isInsidePage = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            currentPage.append(ch, start, length);
        }

        public List<String> getPageTexts() {
            List<String> result = new ArrayList<>();
            for (StringBuilder sb : pages) {
                result.add(sb.toString());
            }
            return result;
        }
    }

    /**
     * 核心算法：带重叠的文本切片 (保持不变)
     */
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastPunctuation = Math.max(
                        text.lastIndexOf("。", end),
                        Math.max(text.lastIndexOf("\n", end), text.lastIndexOf(".", end))
                );
                if (lastPunctuation > start + MAX_CHUNK_SIZE - CHUNK_OVERLAP) {
                    end = lastPunctuation + 1;
                }
            }
            chunks.add(text.substring(start, end));
            if (end >= text.length()) break;
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    @Data
    public static class ParseResult {
        private String fullContent;
        private List<PageContent> pages;
        public ParseResult(String fullContent, List<PageContent> pages) {
            this.fullContent = fullContent;
            this.pages = pages;
        }
    }

    @Data
    public static class PageContent {
        private int pageNum;
        private String content;
        public PageContent(int pageNum, String content) {
            this.pageNum = pageNum;
            this.content = content;
        }
    }
}