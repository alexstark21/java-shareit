package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class GatewayDtoJsonTest {
    @Autowired
    private JacksonTester<BookingCreateDto> bookingJson;

    @Autowired
    private JacksonTester<ItemCreateDto> itemJson;

    @Test
    void shouldSerializeAndDeserializeBookingDatesAsIsoLocalDateTime() throws Exception {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(12L);
        dto.setStart(LocalDateTime.of(2026, 11, 5, 10, 15));
        dto.setEnd(LocalDateTime.of(2026, 11, 6, 11, 30));

        String json = bookingJson.write(dto).getJson();

        assertThat(json).contains("\"start\":\"2026-11-05T10:15:00\"");
        assertThat(json).contains("\"end\":\"2026-11-06T11:30:00\"");
        assertThat(bookingJson.parseObject(json).getStart()).isEqualTo(dto.getStart());
        assertThat(bookingJson.parseObject(json).getEnd()).isEqualTo(dto.getEnd());
    }

    @Test
    void shouldKeepOptionalRequestIdNullWhenItIsOmitted() throws Exception {
        ItemCreateDto dto = itemJson.parseObject("{\"name\":\"Drill\","
                + "\"description\":\"Cordless\",\"available\":true}");

        assertThat(dto.getRequestId()).isNull();
    }

    @Test
    void shouldSerializeOptionalRequestIdWhenProvided() throws Exception {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Drill");
        dto.setDescription("Cordless");
        dto.setAvailable(true);
        dto.setRequestId(77L);

        assertThat(itemJson.write(dto).getJson()).contains("\"requestId\":77");
    }
}
