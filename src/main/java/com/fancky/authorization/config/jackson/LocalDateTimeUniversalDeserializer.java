package com.fancky.authorization.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeUniversalDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMAT_1 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter FORMAT_2 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMAT_3 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMAT_ISO_Z =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter FORMAT_ISO_Z2 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        // ======================
        // 1. 数字：timestamp 处理
        // ======================
        if (p.currentToken().isNumeric()) {
            long value = p.getLongValue();
            int len = String.valueOf(value).length();

            // 纳秒级（19位）：    1748217600000000000
            if (len >= 16) {
                return LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(0, value),
                        ZoneId.systemDefault());
            }

            // 毫秒级（13位）
            if (len == 13) {
                return LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(value),
                        ZoneId.systemDefault());
            }

            // 秒级（10位）
            if (len == 10) {
                return LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(value),
                        ZoneId.systemDefault());
            }
        }

        // ==============
        // 2. 字符串解析
        // ==============
        String text = p.getText().trim();
        if (text.isEmpty()) {
            return null;
        }

        // ISO8601 Z（你指定的格式）
        if (text.endsWith("Z")) {
            try { return LocalDateTime.parse(text, FORMAT_ISO_Z); } catch (Exception ignore) {}
            try { return LocalDateTime.parse(text, FORMAT_ISO_Z2); } catch (Exception ignore) {}
            try { return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault()); } catch (Exception ignore) {}
        }

        // 标准格式
        try { return LocalDateTime.parse(text, FORMAT_1); } catch (Exception ignore) {}
        try { return LocalDateTime.parse(text, FORMAT_2); } catch (Exception ignore) {}
        try {
            return LocalDate.parse(text, FORMAT_3)
                    .atStartOfDay();
        } catch (Exception ignore) {}

        // 兜底用 ISO
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignore) {}

        // 全部失败 → 报错
        throw new IOException("Unsupported LocalDateTime format: " + text);
    }
}

