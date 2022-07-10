import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Scanner;

public class LogicalExpression<T> {
    public JsonNode jsonTree;

    public LogicalExpression(String jsonString) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        jsonTree= mapper.readTree(jsonString);
    }

    public LogicalExpression(JsonNode jsonTree){
        this.jsonTree = jsonTree;
    }
}
