package tian.example;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    // @Test
    // public void shouldAnswerWithTrue()
    // {
    //     assertTrue(true);
    // }
    
    @Test
    public void testApp() {
        // App app = new App();
        // assertTrue(app instanceof App);

        Boolean thrown = false;
        try {
            App.main(new String[] {"src/main/resources/example.csv"});
        } catch (Exception e) {
            e.printStackTrace();
            thrown = true;
        }
        assertTrue(!thrown);
    }

    @Test
    public void testApp_missing_file() {
        // App app = new App();
        // assertTrue(app instanceof App);

        Boolean thrown = false;
        try {
            App.main(new String[] {"src/main/resources/junk.csv"});
        } catch (Exception e) {
            e.printStackTrace();
            thrown = true;
        }
        assertTrue(thrown);
    }
}
