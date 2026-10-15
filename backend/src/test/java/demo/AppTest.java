package demo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.List;

class AppTest {
    @Test
    void testAdd() {
        App app = new App();
        assertEquals(5, app.add(2, 3));
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
        assertTrue(result.score > 0 && result.score < 100);
        assertTrue(result.matchedSkills.contains("React"));
        assertTrue(result.missingSkills.contains("Java"));
        assertFalse(result.suggestedQuestions.isEmpty());
    }

    @Test
    void testAnalyzeEmptyJd() {
        String resume = "Experienced Java and Python developer.";
        String jd = "";

        App.AnalysisResult result = App.analyze(resume, jd);
        assertEquals(80, result.score);
        assertNotNull(result.summary);

        App.AnalysisResult emptyResult = App.analyze("", "");
        assertEquals(50, emptyResult.score);
    }
}
