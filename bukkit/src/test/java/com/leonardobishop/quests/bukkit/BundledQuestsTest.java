package com.leonardobishop.quests.bukkit;

import com.leonardobishop.quests.bukkit.util.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 同梱クエスト（resources/bukkit/quests/*.yml）の整合性を検査する。
 * クエストIDはファイル名から決まり、BukkitQuestsLoader が StringUtils.isAlphanumeric で検査する。
 * 下線入りの名前は読み込まれないため、その再発を防ぐ。
 */
public final class BundledQuestsTest {

    private static final Path QUESTS_DIR = Paths.get("src", "main", "resources", "resources", "bukkit", "quests");
    private static final Pattern REQUIRES_ITEM = Pattern.compile("^\s+-\s*[\"']?([^\"'\s]+)[\"']?\s*$");

    private static List<Path> questFiles() throws IOException {
        try (Stream<Path> s = Files.list(QUESTS_DIR)) {
            List<Path> list = new ArrayList<>();
            s.filter(p -> p.getFileName().toString().endsWith(".yml")).sorted().forEach(list::add);
            return list;
        }
    }

    private static String idOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".yml".length());
    }

    @Test
    void 全クエストのIDが英数字とハイフンだけである() throws IOException {
        List<Path> files = questFiles();
        assertFalse(files.isEmpty(), "クエストファイルが見つからない: " + QUESTS_DIR.toAbsolutePath());
        List<String> invalid = new ArrayList<>();
        for (Path file : files) {
            if (!StringUtils.isAlphanumeric(idOf(file))) {
                invalid.add(file.getFileName().toString());
            }
        }
        assertTrue(invalid.isEmpty(), "ID が isAlphanumeric を通らない（下線は不可）: " + invalid);
    }

    @Test
    void requiresのIDが実在するクエストファイルを指す() throws IOException {
        Set<String> ids = new TreeSet<>();
        for (Path file : questFiles()) {
            ids.add(idOf(file));
        }
        List<String> missing = new ArrayList<>();
        for (Path file : questFiles()) {
            boolean inRequires = false;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.matches("^\s+requires:\s*$")) {
                    inRequires = true;
                    continue;
                }
                if (inRequires) {
                    Matcher m = REQUIRES_ITEM.matcher(line);
                    if (m.matches()) {
                        if (!ids.contains(m.group(1))) {
                            missing.add(file.getFileName() + " -> " + m.group(1));
                        }
                    } else {
                        inRequires = false;
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "requires が実在しないIDを参照している: " + missing);
    }
}
