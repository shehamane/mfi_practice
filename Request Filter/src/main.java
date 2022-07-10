
import java.io.File;
import java.io.IOException;
import java.util.*;

public class main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(new File("src/input.json"));
        String inputString = scanner.useDelimiter("\\A").next();
        scanner.close();

        scanner = new Scanner(new File("src/filter.json"));
        String filterString = scanner.useDelimiter("\\A").next();
        scanner.close();


        LogicalExpression<String> exp = new LogicalExpression<>(inputString);
        LogicalExpression<String> filter = new LogicalExpression<>(filterString);

        DNFFilter.modify(exp, filter);
    }
}
