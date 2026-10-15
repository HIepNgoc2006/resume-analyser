package demo;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8";

    private static final List<String> SKILL_DICTIONARY = List.of(
        "Java", "Spring Boot", "Python", "Docker", "Kubernetes",
        "Jenkins", "CI/CD", "Git", "React", "Vue", "NodeJS",
        "TypeScript", "JavaScript", "SQL", "MongoDB", "PostgreSQL",
        "AWS", "GCP", "REST API", "Microservices", "Linux", "JUnit"
    );

    public static List<String> getSkillDictionary() {
        return SKILL_DICTIONARY;
    }

    public static HttpServer startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HealthHandler());
        server.createContext("/api/analyze", new AnalyzeHandler());
        server.setExecutor(null);
        server.start();
        LOGGER.info(() -> "Backend started on port " + server.getAddress().getPort());
        return server;
    }

    public static void stopServer(HttpServer server) {
        if (server != null) {
            server.stop(0);
        }
    }

    public static void main(String[] args) throws IOException {
        startServer(8180);
    }

    public int add(int a, int b) {
        return a + b;
    }

    public static List<String> extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (String skill : SKILL_DICTIONARY) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) {
                found.add(skill);
            }
        }
        return Collections.unmodifiableList(found);
    }

    public static AnalysisResult analyze(String resume, String jd) {
        List<String> resumeSkills = extractSkills(resume);
        List<String> jdSkills = extractSkills(jd);

        Set<String> resumeSet = new HashSet<>(resumeSkills);
        Set<String> jdSet = new HashSet<>(jdSkills);

        List<String> matched = findMatched(jdSkills, resumeSet);
        List<String> missing = findMissing(jdSkills, resumeSet);
        List<String> additional = findAdditional(resumeSkills, jdSet);

        int score = calculateScore(matched.size(), jdSkills.size(), !resumeSkills.isEmpty());
        String tier = determineTier(score);

        String summary = String.format("Ứng viên đáp ứng %d/%d kỹ năng yêu cầu. Điểm số tương thích: %d%%.",
                matched.size(), jdSkills.size(), score);

        List<String> questions = buildQuestions(matched, missing);

        return new AnalysisResult(score, tier, matched, missing, additional, summary, questions);
    }

    private static List<String> findMatched(List<String> jdSkills, Set<String> resumeSet) {
        List<String> matched = new ArrayList<>();
        for (String skill : jdSkills) {
            if (resumeSet.contains(skill)) {
                matched.add(skill);
            }
        }
        return matched;
    }

    private static List<String> findMissing(List<String> jdSkills, Set<String> resumeSet) {
        List<String> missing = new ArrayList<>();
        for (String skill : jdSkills) {
            if (!resumeSet.contains(skill)) {
                missing.add(skill);
            }
        }
        return missing;
    }

    private static List<String> findAdditional(List<String> resumeSkills, Set<String> jdSet) {
        List<String> additional = new ArrayList<>();
        for (String skill : resumeSkills) {
            if (!jdSet.contains(skill)) {
                additional.add(skill);
            }
        }
        return additional;
    }

    private static int calculateScore(int matchedCount, int totalJd, boolean hasResumeSkills) {
        if (totalJd == 0) {
            return hasResumeSkills ? 80 : 50;
        }
        int score = (int) Math.round(((double) matchedCount / totalJd) * 100);
        return Math.min(100, Math.max(0, score));
    }

    private static String determineTier(int score) {
        if (score >= 80) {
            return "XUẤT SẮC - Ứng viên tiềm năng cao";
        }
        if (score >= 60) {
            return "PHÙ HỢP - Đạt đa số yêu cầu cốt lõi";
        }
        return "CẦN BỔ SUNG - Khoảng trống kỹ năng đáng kể";
    }

    private static List<String> buildQuestions(List<String> matched, List<String> missing) {
        List<String> questions = new ArrayList<>();
        if (!matched.isEmpty()) {
            questions.add(String.format("Bạn đã ứng dụng %s trong các dự án thực tế gần nhất như thế nào?", matched.get(0)));
        }
        if (matched.size() > 1) {
            questions.add(String.format("Kinh nghiệm tối ưu hóa và xử lý sự cố với %s của bạn là gì?", matched.get(1)));
        }
        if (!missing.isEmpty()) {
            questions.add(String.format("Dự án yêu cầu %s, bạn dự định làm quen và tiếp cận công nghệ này trong bao lâu?", missing.get(0)));
        } else {
            questions.add("Bạn có kinh nghiệm lãnh đạo nhóm kỹ thuật hoặc chia sẻ kiến thức công nghệ không?");
        }
        return questions;
    }

    public static class AnalysisResult {
        public final int score;
        public final String tier;
        public final List<String> matchedSkills;
        public final List<String> missingSkills;
        public final List<String> additionalSkills;
        public final String summary;
        public final List<String> suggestedQuestions;

        public AnalysisResult(int score, String tier, List<String> matchedSkills,
                              List<String> missingSkills, List<String> additionalSkills,
                              String summary, List<String> suggestedQuestions) {
            this.score = score;
            this.tier = tier;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
            this.additionalSkills = additionalSkills;
            this.summary = summary;
            this.suggestedQuestions = suggestedQuestions;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"score\":").append(score).append(",");
            sb.append("\"tier\":\"").append(escapeJson(tier)).append("\",");
            sb.append("\"summary\":\"").append(escapeJson(summary)).append("\",");
            sb.append("\"matchedSkills\":").append(listToJson(matchedSkills)).append(",");
            sb.append("\"missingSkills\":").append(listToJson(missingSkills)).append(",");
            sb.append("\"additionalSkills\":").append(listToJson(additionalSkills)).append(",");
            sb.append("\"suggestedQuestions\":").append(listToJson(suggestedQuestions));
            sb.append("}");
            return sb.toString();
        }

        private static String listToJson(List<String> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append("\"").append(escapeJson(list.get(i))).append("\"");
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }

        private static String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    public static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String response = "{\"status\":\"UP\",\"app\":\"resume-analyser-backend\",\"version\":\"v1.0\"}";
            exchange.getResponseHeaders().set(HEADER_CONTENT_TYPE, APPLICATION_JSON);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String error = "{\"error\":\"Only POST allowed\"}";
                exchange.getResponseHeaders().set(HEADER_CONTENT_TYPE, APPLICATION_JSON);
                byte[] b = error.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(405, b.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(b);
                }
                return;
            }

            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            String resume = extractField(body, "resume");
            String jd = extractField(body, "jd");

            AnalysisResult result = analyze(resume, jd);
            String jsonResponse = result.toJson();

            exchange.getResponseHeaders().set(HEADER_CONTENT_TYPE, APPLICATION_JSON_UTF8);
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        static String extractField(String json, String fieldName) {
            String key = "\"" + fieldName + "\"";
            int idx = json.indexOf(key);
            if (idx == -1) return "";
            int colon = json.indexOf(":", idx + key.length());
            if (colon == -1) return "";
            int start = json.indexOf("\"", colon + 1);
            if (start == -1) return "";
            int end = json.indexOf("\"", start + 1);
            while (end != -1 && json.charAt(end - 1) == '\\') {
                end = json.indexOf("\"", end + 1);
            }
            if (end == -1) return "";
            return json.substring(start + 1, end).replace("\\n", "\n").replace("\\\"", "\"");
        }
    }
}
