package com.itta.toggleme.item.service;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

// 냉장고 기록의 태그로 비슷한 음식을 AI에게 묻는다. 사진은 보내지 않고 태그만 보낸다
@Component
public class FoodRecommender {

    private static final String PROMPT = """
            사용자가 냉장고에 기록한 음식 사진의 태그다: %s
            이 태그와 비슷한 음식을 3개 추천한다.
            - 음식 이름만 쓴다. 한국어 20자 이내, 설명·#·브랜드명은 쓰지 않는다
            - 태그에 이미 있는 음식과 같은 것은 피하고, 비슷한 다른 메뉴를 고른다
            """;

    private final ChatClient chatClient;

    public FoodRecommender(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<String> recommend(List<String> tags) {
        FoodNames names = chatClient.prompt()
                .user(PROMPT.formatted(String.join(", ", tags.stream().map(tag -> "#" + tag).toList())))
                .call()
                .entity(FoodNames.class);
        return names == null ? List.of() : names.foods();
    }
}
