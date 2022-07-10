import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DNFFilter {
    private static boolean isConjunctInConjunct(JsonNode l, JsonNode r) throws IOException {
        String lType = l.get("type").toString();
        String rType = r.get("type").toString();
        if (lType.equals("\"AND\"")) {
            if (!rType.equals("\"AND\""))
                return false;

            ObjectMapper mapper = new ObjectMapper();
            ObjectReader reader = mapper.readerFor(new TypeReference<List<JsonNode>>() {});

            ArrayNode lChildren = (ArrayNode) l.get("children");
            ArrayNode rChildren = (ArrayNode) r.get("children");
            List<JsonNode> rChildrenList =  reader.readValue(rChildren);
            boolean flag;
            for (JsonNode lChild : lChildren) {
                flag = false;
                for (int i = 0; i < rChildrenList.size(); ++i)
                    if (DNFNormalizer.isEquals(lChild, rChildrenList.get(i))) {
                        flag = true;
                        rChildrenList.remove(i);
                        break;
                    }
                if (!flag)
                    return false;
            }
            return true;
        } else {
            if (rType.equals("\"AND\"")) {
                ArrayNode rChildren = (ArrayNode) r.get("children");
                for (JsonNode rChild : rChildren)
                    if (DNFNormalizer.isEquals(l, rChild))
                        return true;
                return false;
            } else
                return DNFNormalizer.isEquals(l, r);
        }
    }

    private static boolean isConjunctInDNF(JsonNode conjunct, JsonNode DNF) throws IOException {
        if (!DNF.get("type").toString().equals("\"OR\""))
            return isConjunctInConjunct(conjunct, DNF);
        ArrayNode DNFConjuncts = (ArrayNode) DNF.get("children");
        for (JsonNode DNFConjunct : DNFConjuncts)
            if (isConjunctInConjunct(conjunct, DNFConjunct))
                return true;
        return false;
    }

    private static <T> void filterExpression(LogicalExpression<T> expression, LogicalExpression<T> filter) throws IOException {
        JsonNode exprNode = expression.jsonTree;
        if (!exprNode.get("type").toString().equals("\"OR\"")) {
            if (isConjunctInDNF(exprNode, filter.jsonTree))
                ((ObjectNode)exprNode).removeAll();
        } else {
            ArrayNode exprConjuncts = (ArrayNode) exprNode.get("children");
            for (int i = 0; i<exprConjuncts.size(); ++i)
                if (isConjunctInDNF(exprConjuncts.get(i), filter.jsonTree))
                    exprConjuncts.remove(i--);
            if (!exprNode.get("type").toString().equals("NOT") && exprConjuncts.size() == 1){
                JsonNode child = ((ArrayNode)exprNode.get("children")).get(0);
                ((ObjectNode) exprNode).removeAll();
                for (Iterator<Map.Entry<String, JsonNode>> it = child.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> tmp = it.next();
                    ((ObjectNode) exprNode).put(tmp.getKey(), tmp.getValue());
                }
            }else if (exprConjuncts.size() == 0)
                ((ObjectNode) exprNode).removeAll();
        }
    }

    private static <T> LogicalExpression<T> joinFilters(LogicalExpression<T>... filters) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode overallFilter = mapper.createObjectNode();
        ((ObjectNode) overallFilter).put("type", "OR");
        ((ObjectNode) overallFilter).putArray("children");
        ArrayNode children = (ArrayNode) overallFilter.get("children");
        for (LogicalExpression<T> filter : filters)
            children.add(filter.jsonTree);
        return new LogicalExpression<T>(overallFilter);
    }

    public static <T> LogicalExpression<T> modify(LogicalExpression<T> expression, LogicalExpression<T>... filters) throws IOException {
        LogicalExpression<T> normalizedExpression = DNFNormalizer.normalize(expression);
        LogicalExpression<T> filter = DNFNormalizer.normalize(joinFilters(filters));
        filterExpression(normalizedExpression, filter);
        return normalizedExpression;
    }
}
