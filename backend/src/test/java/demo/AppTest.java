package demo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

class AppTest {
    @Test
    void testAdd() {
        App app = new App();
        assertEquals(5, app.add(2, 3));
    }

    @Test
    void testSkillDictionary() {
        assertNotNull(App.getSkillDictionary());
        assertFalse(App.getSkillDictionary().isEmpty());
        assertTrue(App.getSkillDictionary().contains("Java"));
    }

    @Test
    void testExtractSkills() {
        String text = "I am skilled in Java, Spring Boot, Docker, and Kubernetes.";
        List<String> skills = App.extractSkills(text);
        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("Spring Boot"));
        assertTrue(skills.contains("Docker"));
        assertTrue(skills.contains("Kubernetes"));
        assertFalse(skills.contains("React"));

        assertTrue(App.extractSkills(null).isEmpty());
        assertTrue(App.extractSkills("   ").isEmpty());
    }

    @Test
    void testAnalyzeHighMatch() {
        String resume = "Senior Java Engineer with 5 years experience in Java, Spring Boot, Docker, CI/CD, Git, REST API.";
        String jd = "Looking for a backend developer proficient in Java, Spring Boot, Docker, Git.";

        App.AnalysisResult result = App.analyze(resume, jd);
        assertEquals(100, result.score);
        assertTrue(result.tier.contains("XUẤT SẮC"));
        assertEquals(4, result.matchedSkills.size());
        assertTrue(result.missingSkills.isEmpty());
        assertTrue(result.additionalSkills.contains("CI/CD"));
        assertNotNull(result.toJson());
        assertTrue(result.toJson().contains("\"score\":100"));
    }

    @Test
    void testAnalyzePartialMatch() {
        String resume = "Frontend developer skilled in React, TypeScript, HTML/CSS, Git.";
        String jd = "Fullstack developer needed: Java, Spring Boot, React, Docker.";

        App.AnalysisResult result = App.analyze(resume, jd);
        assertTrue(result.score >= 0 && result.score < 100);
        assertTrue(result.matchedSkills.contains("React"));
        assertTrue(result.missingSkills.contains("Java"));
        assertFalse(result.suggestedQuestions.isEmpty());
    }

    @Test
    void testAnalyzeEmptyJdAndResume() {
        String resume = "Experienced Java and Python developer.";
        String jd = "";

        App.AnalysisResult result = App.analyze(resume, jd);
        assertEquals(80, result.score);
        assertNotNull(result.summary);

        App.AnalysisResult emptyResult = App.analyze("", "");
        assertEquals(50, emptyResult.score);
        assertTrue(emptyResult.tier.contains("CẦN BỔ SUNG"));
    }

    @Test
    void testAnalyzeScoreTiers() {
        // Medium match (60-79%)
        String resume = "Skilled in Java, Spring Boot, Docker.";
        String jd = "Java, Spring Boot, Docker, Kubernetes, AWS."; // 3/5 = 60%
        App.AnalysisResult medium = App.analyze(resume, jd);
        assertEquals(60, medium.score);
        assertTrue(medium.tier.contains("PHÙ HỢP"));

        // Low match (<60%)
        String jdLow = "Java, Spring Boot, Docker, Kubernetes, AWS, GCP, React, Vue, SQL, MongoDB."; // 3/10 = 30%
        App.AnalysisResult low = App.analyze(resume, jdLow);
        assertEquals(30, low.score);
        assertTrue(low.tier.contains("CẦN BỔ SUNG"));
    }

    @Test
    void testExtractFieldHelper() {
        String json = "{\"resume\":\"John Doe\\nSoftware Engineer\",\"jd\":\"Java\\\"Senior\\\"\"}";
        assertEquals("John Doe\nSoftware Engineer", App.AnalyzeHandler.extractField(json, "resume"));
        assertEquals("Java\"Senior\"", App.AnalyzeHandler.extractField(json, "jd"));
        assertEquals("", App.AnalyzeHandler.extractField(json, "missingKey"));
        assertEquals("", App.AnalyzeHandler.extractField("{}", "resume"));
        assertEquals("", App.AnalyzeHandler.extractField("{\"resume\": }", "resume"));
    }

    @Test
    void testHttpServerFullEndpoints() throws IOException {
        HttpServer server = App.startServer(0);
        int port = server.getAddress().getPort();

        try {
            // 1. GET / (Health check)
            HttpURLConnection connHealth = (HttpURLConnection) URI.create("http://localhost:" + port + "/").toURL().openConnection();
            connHealth.setRequestMethod("GET");
            assertEquals(200, connHealth.getResponseCode());
            String healthBody = readResponseBody(connHealth);
            assertTrue(healthBody.contains("\"status\":\"UP\""));
            connHealth.disconnect();

            // 2. OPTIONS /
            HttpURLConnection connOpt = (HttpURLConnection) URI.create("http://localhost:" + port + "/").toURL().openConnection();
            connOpt.setRequestMethod("OPTIONS");
            assertEquals(204, connOpt.getResponseCode());
            connOpt.disconnect();

            // 3. POST /api/analyze (Valid analysis)
            HttpURLConnection connAnalyze = (HttpURLConnection) URI.create("http://localhost:" + port + "/api/analyze").toURL().openConnection();
            connAnalyze.setRequestMethod("POST");
            connAnalyze.setDoOutput(true);
            connAnalyze.setRequestProperty("Content-Type", "application/json");
            String payload = "{\"resume\":\"Developer skilled in Java, Docker\",\"jd\":\"Java, Docker\"}";
            try (OutputStream os = connAnalyze.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            assertEquals(200, connAnalyze.getResponseCode());
            String analyzeBody = readResponseBody(connAnalyze);
            assertTrue(analyzeBody.contains("\"score\":100"));
            connAnalyze.disconnect();

            // 4. GET /api/analyze (Method not allowed)
            HttpURLConnection connAnalyzeGet = (HttpURLConnection) URI.create("http://localhost:" + port + "/api/analyze").toURL().openConnection();
            connAnalyzeGet.setRequestMethod("GET");
            assertEquals(405, connAnalyzeGet.getResponseCode());
            connAnalyzeGet.disconnect();

            // 5. OPTIONS /api/analyze
            HttpURLConnection connAnalyzeOpt = (HttpURLConnection) URI.create("http://localhost:" + port + "/api/analyze").toURL().openConnection();
            connAnalyzeOpt.setRequestMethod("OPTIONS");
            assertEquals(204, connAnalyzeOpt.getResponseCode());
            connAnalyzeOpt.disconnect();

        } finally {
            App.stopServer(server);
            App.stopServer(null); // safe call
        }
    }

    private static String readResponseBody(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";
        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
