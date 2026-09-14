package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du sérialiseur JSON des dates (format dd-MM-yyyy, utilisé
 * sur les champs issueDate / dueDate / returnDate de Borrow).
 */
class JsonDataSerializerTest {

    @Test
    void serialize_renvoieLaDateAuFormatJJMMAAAA() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Date.class, new JsonDataSerializer());
        mapper.registerModule(module);

        Date date = new GregorianCalendar(2026, Calendar.SEPTEMBER, 11, 15, 42).getTime();
        Map<String, Date> source = new HashMap<>();
        source.put("dueDate", date);

        String json = mapper.writeValueAsString(source);

        assertThat(json).isEqualTo("{\"dueDate\":\"11-09-2026\"}");
    }
}
