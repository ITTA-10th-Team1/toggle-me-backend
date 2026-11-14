package com.itta.toggleme.item.service;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

// 책장 기록의 태그로 비슷한 책을 AI에게 묻는다. 사진은 보내지 않고 태그만 보낸다
@Component
public class BookRecommender {

    private static final String PROMPT = """
            사용자가 책장에 기록한 책 사진의 태그다: %s
            이 태그와 분위기나 장르가 비슷한 실제로 출간된 한국어 책을 5권 추천한다.
            - 책 제목만 쓴다. 40자 이내, 저자·출판사·설명·#은 쓰지 않는다
            - 확실히 존재하는 책만 쓰고, 태그에 이미 제목이 있는 책은 피한다
            """;

    private final ChatClient chatClient;

    public BookRecommender(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<String> recommend(List<String> tags) {
        BookTitles titles = chatClient.prompt()
                .user(PROMPT.formatted(String.join(", ", tags.stream().map(tag -> "#" + tag).toList())))
                .call()
                .entity(BookTitles.class);
        return titles == null ? List.of() : titles.books();
    }
}
