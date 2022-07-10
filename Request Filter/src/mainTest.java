import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class mainTest {

    @org.junit.jupiter.api.Test
    void getGroup() throws FileNotFoundException, JsonProcessingException {
        Scanner scanner;
        File testFolder = new File("test");
        File[] testFolderEntries = testFolder.listFiles();
        for (File test : testFolderEntries) {
            scanner = new Scanner(new File("test/" + test.getName() + "/request.json"));
            String inputString = scanner.useDelimiter("\\A").next();
            scanner.close();

            scanner = new Scanner(new File("test/" + test.getName() + "/request_dnf.json"));
            String correctString = scanner.useDelimiter("\\A").next();
            scanner.close();

            LogicalExpression<String> input = new LogicalExpression<>(inputString);
            LogicalExpression<String> correct = new LogicalExpression<>(correctString);
            try {
                DNFNormalizer.normalize(input);
            }catch (Exception e){
                System.out.println("Error on test: " + test.getName() + "; " + e.getMessage());
            }
            try{
                Assert.assertTrue(DNFNormalizer.isEquals(input.jsonTree, correct.jsonTree));
            }catch (AssertionError | IOException e){
                System.out.println("TEST FAIL: " + test.getName() + ";");
            }
        }
    }

    @org.junit.jupiter.api.Test
    void filterRequest() throws FileNotFoundException, JsonProcessingException {
        Scanner scanner;
        File testFolder = new File("test");
        File[] testFolderEntries = testFolder.listFiles();
        for (File test : testFolderEntries) {
            scanner = new Scanner(new File("test/" + test.getName() + "/request.json"));
            String inputString = scanner.useDelimiter("\\A").next();
            scanner.close();

            scanner = new Scanner(new File("test/" + test.getName() + "/filter.json"));
            String filterString = scanner.useDelimiter("\\A").next();
            scanner.close();

            scanner = new Scanner(new File("test/" + test.getName() + "/answer.json"));
            String answerString = scanner.useDelimiter("\\A").next();
            scanner.close();

            LogicalExpression<String> exp = new LogicalExpression<>(inputString);
            LogicalExpression<String> filter = new LogicalExpression<>(filterString);
            LogicalExpression<String> answer = new LogicalExpression<>(answerString);

            try {
                exp = DNFFilter.modify(exp, filter);
            }catch (Exception e){
                System.out.println("Error on test: " + test.getName() + "; " + e.getMessage());
            }
            try{
                Assert.assertTrue(DNFNormalizer.isEquals(exp.jsonTree, answer.jsonTree));
            }catch (AssertionError | IOException e){
                System.out.println("TEST FAIL: " + test.getName() + ";");
            }
        }
    }
}