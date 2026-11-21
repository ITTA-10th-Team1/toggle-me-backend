package com.itta.toggleme.item.service;

import com.itta.toggleme.item.repository.TasteTagCount;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

// 취향 카드의 태그로 유형명과 한 줄 설명을 AI에게 묻는다. 사진·메모는 보내지 않고 태그와 횟수만 보낸다
@Component
public class TasteProfileSummarizer {

    private static final String PROMPT = """
            사용자가 최근 한 달 동안 기록한 사진의 해시태그와 나온 횟수다: %s
            이 사람이 어떤 사람인지 재미있는 유형명과 한 줄 설명을 만든다.
            - typeName: 한국어 12자 이내, 재치 있는 별명 형태, #·이모지·따옴표는 쓰지 않는다
            - description: 반드시 공백 포함 50자 이하의 짧은 한 문장(예: 추리소설과 담백한 일상을 좋아하는 호기심 많은 사람). 태그에서 알 수 있는 취향만 쓰고 개인 정보는 추측하지 않는다
            """;

    private final ChatClient chatClient;

    public TasteProfileSummarizer(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public TasteProfileSummary summarize(List<TasteTagCount> tags) {
        String joined = String.join(", ", tags.stream().map(tag -> "#" + tag.name() + "(" + tag.count() + ")").toList());
        return chatClient.prompt()
                .user(PROMPT.formatted(joined))
                .call()
                .entity(TasteProfileSummary.class);
    }
}
