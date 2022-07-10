import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;

public class DNFNormalizer {
    private static void mergeLeftToRight(JsonNode l, JsonNode r, int index) {
        ArrayNode lChildren = (ArrayNode) l.get("children");
        ArrayNode rChildren = (ArrayNode) r.get("children");
        for (JsonNode lChild : lChildren)
            rChildren.add(lChild);
        rChildren.remove(index);
    }

    private static void deleteNode(JsonNode el, JsonNode p) {
        JsonNode child = ((ArrayNode) el.get("children")).get(0);
        if (p != null && p.get("type").toString().equals("\"NOT\"")) {
            ((ObjectNode) p).put("child", child);
        } else {
            ((ObjectNode) el).removeAll();
            for (Iterator<Map.Entry<String, JsonNode>> it = child.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> tmp = it.next();
                ((ObjectNode) el).put(tmp.getKey(), tmp.getValue());
            }
        }
    }

    public static boolean isEquals(JsonNode l, JsonNode r) throws IOException {
        if (l.isEmpty() && r.isEmpty())
            return true;
        else if (l.isEmpty() || r.isEmpty())
            return false;
        String type = l.get("type").toString();
        if (type.equals(r.get("type").toString())) {
            if (type.equals("\"OR\"") || type.equals("\"AND\"")) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectReader reader = mapper.readerFor(new TypeReference<List<JsonNode>>() {
                });

                ArrayNode lChildren = (ArrayNode) l.get("children");
                ArrayNode rChildren = (ArrayNode) r.get("children");
                List<JsonNode> rChildrenList = reader.readValue(rChildren);

                boolean flag;
                for (JsonNode lChild : lChildren) {
                    flag = false;
                    for (int i = 0; i < rChildrenList.size(); ++i) {
                        if (isEquals(lChild, rChildrenList.get(i))) {
                            flag = true;
                            rChildrenList.remove(i);
                            break;
                        }
                    }
                    if (!flag)
                        return false;
                }
                return rChildrenList.isEmpty();
            } else if (type.equals("\"NOT\"")) {
                return isEquals(l.get("child"), r.get("child"));
            } else
                return true;
        } else
            return false;
    }

    private static void deleteDuplicateChildren(JsonNode el) throws IOException {
        ArrayNode children = (ArrayNode) el.get("children");
        for (int i = 0; i < children.size(); ++i)
            for (int j = i + 1; j < children.size(); ++j)
                if (isEquals(children.get(i), children.get(j)))
                    children.remove(j--);
    }

    private static JsonNode getConjunctsProduct(JsonNode l, JsonNode r) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode product = mapper.createObjectNode();
        ((ObjectNode) product).put("type", "AND");
        ((ObjectNode) product).put("flag", Boolean.TRUE);
        ((ObjectNode) product).putArray("children");
        ArrayNode children = (ArrayNode) product.get("children");
        if (l.get("type").toString().equals("\"AND\""))
            for (JsonNode lChild : (ArrayNode) l.get("children"))
                children.add(lChild);
        else
            children.add(l);
        if (r.get("type").toString().equals("\"AND\""))
            for (JsonNode rChild : (ArrayNode) r.get("children"))
                children.add(rChild);
        else
            children.add(r);

        deleteDuplicateChildren(product);
        if (children.size() == 1)
            return children.get(0);
        return product;
    }

    private static List<JsonNode> getBracketAndNodeProduct(List<JsonNode> bracket, JsonNode el) throws IOException {
        List<JsonNode> newBracket = new ArrayList<>();
        if (el.get("type").toString().equals("\"OR\"")) {
            ArrayNode elChildren = (ArrayNode) el.get("children");
            for (JsonNode bracketElement : bracket)
                for (JsonNode elChild : elChildren)
                    newBracket.add(getConjunctsProduct(bracketElement, elChild));
        } else {
            for (JsonNode bracketElement : bracket)
                newBracket.add(getConjunctsProduct(bracketElement, el));
        }
        return newBracket;
    }

    private static void expandBrackets(JsonNode el) throws IOException {
        ArrayNode children = (ArrayNode) el.get("children");
        List<JsonNode> currentBracket = new ArrayList<>();

        JsonNode firstChild = children.get(0);
        if (firstChild.get("type").toString().equals("\"OR\"")) {
            ArrayNode firstChildren = (ArrayNode) firstChild.get("children");
            for (JsonNode grandChild : firstChildren)
                currentBracket.add(grandChild);
        } else
            currentBracket.add(firstChild);

        for (int i = 1; i < children.size(); ++i) {
            JsonNode currentChild = children.get(i);
            currentBracket = getBracketAndNodeProduct(currentBracket, currentChild);
        }

        ((ObjectNode) el).put("type", "OR");
        ((ObjectNode) el).putArray("children");
        ((ArrayNode) el.get("children")).addAll(currentBracket);
        ((ObjectNode) el).put("flag", Boolean.TRUE);
    }

    private static JsonNode getNegativeParent(JsonNode el) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode p = mapper.createObjectNode();
        ((ObjectNode) p).put("type", "NOT");
        ((ObjectNode) p).put("flag", Boolean.TRUE);
        ((ObjectNode) p).put("child", el);
        return p;
    }

    private static void fixDeMorgan(JsonNode el) {
        JsonNode child = el.get("child");
        ArrayNode grandChildren = (ArrayNode) child.get("children");

        if (child.get("type").toString().equals("\"AND\""))
            ((ObjectNode) el).put("type", "OR");
        else
            ((ObjectNode) el).put("type", "AND");
        ((ObjectNode) el).putArray("children");
        ((ObjectNode) el).put("flag", Boolean.TRUE);
        ((ObjectNode) el).remove("child");

        for (JsonNode grandChild : grandChildren) {
            JsonNode newChild = getNegativeParent(grandChild);
            if (newChild.get("child").get("type").toString().equals("\"NOT\""))
                fixDoubleNegation(newChild);
            ((ArrayNode) el.get("children")).add(newChild);
        }
    }

    private static void fixDoubleNegation(JsonNode el) {
        JsonNode child = el.get("child");
        JsonNode grandChild = child.get("child");
        ((ObjectNode) el).removeAll();
        for (Iterator<Map.Entry<String, JsonNode>> it = grandChild.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> tmp = it.next();
            ((ObjectNode) el).put(tmp.getKey(), tmp.getValue());
        }
    }

    private static char getGroup(JsonNode el, JsonNode p, int index) throws IOException {
        if (el.has("flag"))
            return 254;
        ((ObjectNode) el).put("flag", Boolean.TRUE);

        String type = el.get("type").toString();
        switch (type) {
            case "\"NOT\"": {
                JsonNode child = el.get("child");
                getGroup(child, el, 0);
                child = el.get("child");
                switch (child.get("type").toString()) {
                    case "\"NOT\"":
                        fixDoubleNegation(el);
                        return 253;
                    case "\"AND\"":
                        fixDeMorgan(el);
                        if (p != null && el.get("type").toString().equals(p.get("type").toString())) {
                            mergeLeftToRight(el, p, index);
                            return 255;
                        }
                        return 3;
                    case "\"OR\"":
                        fixDeMorgan(el);
                        if (p != null && el.get("type").toString().equals(p.get("type").toString())) {
                            mergeLeftToRight(el, p, index);
                            return 255;
                        }
                        return 2;
                }
                return 1;
            }
            case "\"AND\"": {
                boolean isExpand = false;
                ArrayNode children = (ArrayNode) el.get("children");
                for (int i = 0; i < children.size(); ++i) {
                    char group = getGroup(children.get(i), el, i);
                    if (group == 3)
                        isExpand = true;
                    if (group == 255)
                        --i;
                }

                for (int i = 0; i < children.size(); ++i)
                    if (isExpand) {
                        expandBrackets(el);
                        break;
                    }

                deleteDuplicateChildren(el);
                if (children.size() == 1) {
                    deleteNode(el, p);
                    return 252;
                }
                if (p != null && el.get("type").toString().equals(p.get("type").toString())) {
                    mergeLeftToRight(el, p, index);
                    return 255;
                }
                if (isExpand)
                    return 3;
                else
                    return 2;
            }
            case "\"OR\"": {
                ArrayNode children = (ArrayNode) el.get("children");
                for (int i = 0; i < children.size(); ++i) {
                    char group = getGroup(children.get(i), el, i);
                    if (group == 255)
                        --i;
                }

                deleteDuplicateChildren(el);
                if (children.size() == 1) {
                    deleteNode(el, p);
                    return 252;
                }
                if (p != null && el.get("type").toString().equals(p.get("type").toString())) {
                    mergeLeftToRight(el, p, index);
                    return 255;
                }

                return 3;
            }
            default:
                return 0;
        }
    }

    public static <T> LogicalExpression<T> normalize(LogicalExpression<T> expression) throws IOException {
        getGroup(expression.jsonTree, null, -1);
        return expression;
    }
}
