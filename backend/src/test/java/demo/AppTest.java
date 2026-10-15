package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void testAdd() {
        App app = new App();
        assertEquals(5, app.add(2, 3));
    }
}
