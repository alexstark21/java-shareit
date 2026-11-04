package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.request.ItemRequestDto;
import ru.practicum.shareit.request.ItemRequestItemDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ServerDtoJsonTest {
    @Autowired
    private JacksonTester<ItemRequestDto> requestJson;

    @Test
    void shouldSerializeRequestCreatedDateAndResponseItems() throws Exception {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(10L);
        dto.setDescription("Need a bicycle");
        dto.setRequestorId(2L);
        dto.setCreated(LocalDateTime.of(2026, 11, 4, 12, 30));
        dto.setItems(List.of(new ItemRequestItemDto(30L, "Bicycle", 7L, 10L)));

        String json = requestJson.write(dto).getJson();

        assertThat(json).contains("\"created\":\"2026-11-04T12:30:00\"");
        assertThat(json).contains("\"ownerId\":7");
        assertThat(json).contains("\"requestId\":10");
        ItemRequestDto restored = requestJson.parseObject(json);
        assertThat(restored.getCreated()).isEqualTo(dto.getCreated());
        assertThat(restored.getItems()).hasSize(1);
        assertThat(restored.getItems().get(0).getName()).isEqualTo("Bicycle");
    }
}
