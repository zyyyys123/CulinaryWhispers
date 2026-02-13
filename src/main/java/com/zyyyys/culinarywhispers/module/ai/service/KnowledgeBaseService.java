package com.zyyyys.culinarywhispers.module.ai.service;

import com.zyyyys.culinarywhispers.module.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final AiProperties aiProperties;

    private volatile long loadedAtMs = 0;
    private volatile List<KbChunk> chunks = List.of();

    public List<KbChunk> retrieve(String query) {
        ensureLoaded();
        if (query == null || query.isBlank() || chunks.isEmpty()) {
            return List.of();
        }

        Set<String> tokens = buildTokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<ScoredChunk> scored = new ArrayList<>(chunks.size());
        for (KbChunk c : chunks) {
            int score = 0;
            String text = c.textLower;
            for (String t : tokens) {
                int idx = 0;
                while (idx >= 0) {
                    idx = text.indexOf(t, idx);
                    if (idx >= 0) {
                        score += t.length() >= 2 ? 2 : 1;
                        idx += t.length();
                    }
                }
            }
            if (score > 0) {
                scored.add(new ScoredChunk(c, score));
            }
        }

        scored.sort(Comparator.comparingInt(ScoredChunk::score).reversed());
        int k = Math.min(aiProperties.getMaxChunks(), scored.size());
        List<KbChunk> result = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            result.add(scored.get(i).chunk());
        }
        return result;
    }

    public void refresh() {
        loadedAtMs = 0;
        chunks = List.of();
        ensureLoaded();
    }

    private void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (now - loadedAtMs < 10_000 && !chunks.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (now - loadedAtMs < 10_000 && !chunks.isEmpty()) {
                return;
            }
            chunks = loadChunks();
            loadedAtMs = now;
        }
    }

    private List<KbChunk> loadChunks() {
        Path root = Paths.get(aiProperties.getKnowledgePath());
        if (!Files.exists(root)) {
            return List.of();
        }

        List<KbChunk> out = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                String name = p.getFileName().toString().toLowerCase();
                if (!(name.endsWith(".md") || name.endsWith(".txt"))) {
                    return;
                }
                try {
                    String content = Files.readString(p, StandardCharsets.UTF_8);
                    String normalized = normalize(content);
                    for (String part : split(normalized)) {
                        String t = part.trim();
                        if (t.isBlank()) {
                            continue;
                        }
                        out.add(new KbChunk(p.toString(), t));
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
            return List.of();
        }
        return out;
    }

    private String normalize(String s) {
        String v = s.replace("\r\n", "\n");
        v = v.replace("\t", " ");
        return v;
    }

    private List<String> split(String content) {
        List<String> parts = new ArrayList<>();
        String[] blocks = content.split("\n\\s*\n");
        StringBuilder buf = new StringBuilder();
        for (String b : blocks) {
            String t = b.trim();
            if (t.isBlank()) {
                continue;
            }
            if (buf.length() + t.length() + 2 > 900) {
                if (!buf.isEmpty()) {
                    parts.add(buf.toString());
                    buf.setLength(0);
                }
            }
            if (!buf.isEmpty()) {
                buf.append("\n\n");
            }
            buf.append(t);
        }
        if (!buf.isEmpty()) {
            parts.add(buf.toString());
        }
        return parts;
    }

    private Set<String> buildTokens(String query) {
        String q = query.toLowerCase().trim();
        Set<String> tokens = new HashSet<>();
        for (String p : q.split("\\s+")) {
            String t = p.trim();
            if (t.length() >= 2) {
                tokens.add(t);
            }
        }

        String noSpace = q.replaceAll("\\s+", "");
        if (noSpace.length() >= 2) {
            int max = Math.min(24, noSpace.length());
            for (int i = 0; i < max - 1; i++) {
                String bi = noSpace.substring(i, i + 2);
                if (!bi.isBlank()) {
                    tokens.add(bi);
                }
            }
        }
        return tokens;
    }

    public record KbChunk(String source, String text, String textLower) {
        public KbChunk(String source, String text) {
            this(source, text, text.toLowerCase());
        }
    }

    private record ScoredChunk(KbChunk chunk, int score) {
    }
}
