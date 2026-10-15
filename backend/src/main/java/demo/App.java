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

public class App {
    public static final String[] SKILL_DICTIONARY = {
        "Java", "Spring Boot", "Python", "Docker", "Kubernetes",
        "Jenkins", "CI/CD", "Git", "React", "Vue", "NodeJS",
        "TypeScript", "JavaScript", "SQL", "MongoDB", "PostgreSQL",
        "AWS", "GCP", "REST API", "Microservices", "Linux", "JUnit"
    };

    public static void main(String[] args) throws IOException {
        int port = 8180;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HealthHandler());
        server.createContext("/api/analyze", new AnalyzeHandler());
        server.setExecutor(null);
        System.out.println("Backend started on port " + port);
        server.start();
    }

    public int add(int a, int b) {
        return a + b;
    }

    public static List<String> extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String lower = text.toLowerCase();
        List<String> found = new ArrayList<>();
        for (String skill : SKILL_DICTIONARY) {
            String target = skill.toLowerCase();
            if (lower.contains(target)) {
                found.add(skill);
            }
        }
        return found;
    }

    public static AnalysisResult analyze(String resume, String jd) {
        List<String> resumeSkills = extractSkills(resume);
        List<String> jdSkills = extractSkills(jd);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> additional = new ArrayList<>();

        Set<String> resumeSet = new HashSet<>(resumeSkills);
        Set<String> jdSet = new HashSet<>(jdSkills);

        for (String s : jdSkills) {
            if (resumeSet.contains(s)) {
                matched.add(s);
            } else {
                missing.add(s);
            }
        }

        for (String s : resumeSkills) {
            if (!jdSet.contains(s)) {
                additional.add(s);
            }
        }

        int score;
        if (jdSkills.isEmpty()) {
            score = resumeSkills.isEmpty() ? 50 : 80;
        } else {
            score = (int) Math.round(((double) matched.size() / jdSkills.size()) * 100);
            if (score > 100) score = 100;
        }

        String tier;
        if (score >= 80) {
            tier = "XUẤT SẮC - Ứng viên tiềm năng cao";
        } else if (score >= 60) {
            tier = "PHÙ HỢP - Đạt đa số yêu cầu cốt lõi";
        } else {
            tier = "CẦN BỔ SUNG - Khoảng trống kỹ năng đáng kể";
        }

        String summary = String.format("Ứng viên đáp ứng %d/%d kỹ năng yêu cầu. Điểm số tương thích: %d%%.",
                matched.size(), jdSkills.size(), score);

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

        return new AnalysisResult(score, tier, matched, missing, additional, summary, questions);
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
            exchange.getResponseHeaders().set("Content-Type", "application/json");
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
                exchange.getResponseHeaders().set("Content-Type", "application/json");
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

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private static String extractField(String json, String fieldName) {
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
