package ca.uwaterloo.ece.bicer;
import org.junit.Test;
import static org.junit.Assert.*;

/*
 * This Java source file was auto generated by running 'gradle init --type java-library'
 * by 'j22nam' at '27/05/16 5:45 PM' with Gradle 2.13
 *
 * @author j22nam, @date 27/05/16 5:45 PM
 */
public class NoiseFilterRunnerTest {
    @Test public void testSomeLibraryMethod() {
        
    	NoiseFilterRunner runner = new NoiseFilterRunner();
    	
    	//String [] args ={"-d","data/exampleBIChanges.txt", "-g", System.getProperty("user.home") + "/git/BICER"};
    	String [] args ={"-d",System.getProperty("user.home") + "/Documents/ODP/projects/lucene/biChangesSanitized.txt", "-g", System.getProperty("user.home") + "/Documents/ODP/projects/lucene/git"};
    	
    	runner.run(args);
    	
    	//assertTrue("someLibraryMethod should return 'true'", classUnderTest.someLibraryMethod());
    }
}
