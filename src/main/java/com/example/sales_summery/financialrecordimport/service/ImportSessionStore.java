package com.example.sales_summery.financialrecordimport.service;

import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class ImportSessionStore {
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private final Map<String, ImportSession> sessions = new ConcurrentHashMap<>();

    String put(Long userId, java.util.List<ParsedImportFile> files, Map<String, Long> autoCategoryIds,
               Map<String, String> displayHeaders, java.util.Set<String> unmappedHeaders,
               int duplicateFileCount) {
        removeExpired();
        String token = UUID.randomUUID().toString();
        sessions.put(token, new ImportSession(userId, Instant.now().plus(SESSION_TTL), files,
                Map.copyOf(autoCategoryIds), Map.copyOf(displayHeaders), Set.copyOf(unmappedHeaders),
                duplicateFileCount));
        return token;
    }

    ImportSession get(String token, Long userId) {
        removeExpired();
        ImportSession session = sessions.get(token);
        if (session == null || !session.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.IMPORT_SESSION_NOT_FOUND);
        }
        return session;
    }

    void remove(String token) {
        sessions.remove(token);
    }

    private void removeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }
}
