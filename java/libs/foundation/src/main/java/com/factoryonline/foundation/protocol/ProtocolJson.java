package com.factoryonline.foundation.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class ProtocolJson {
    private ProtocolJson() {
    }

    static String object(String... fields) {
        StringBuilder builder = new StringBuilder("{");
        for (int index = 0; index < fields.length; index += 1) {
            if (index > 0) {
                builder.append(',');
            }

            builder.append(fields[index]);
        }

        builder.append('}');
        return builder.toString();
    }

    static String stringField(String name, String value) {
        return quote(name) + ':' + quote(value);
    }

    static String intField(String name, int value) {
        return quote(name) + ':' + value;
    }

    static String rawField(String name, String rawJson) {
        return quote(name) + ':' + Objects.requireNonNull(rawJson, "rawJson");
    }

    static Map<String, String> parseObject(String json) {
        String trimmedJson = Objects.requireNonNull(json, "json").trim();
        if (trimmedJson.length() < 2 || trimmedJson.charAt(0) != '{' || trimmedJson.charAt(trimmedJson.length() - 1) != '}') {
            throw new IllegalArgumentException("Expected JSON object: " + trimmedJson);
        }

        Map<String, String> fields = new HashMap<>();
        int index = 1;

        while (index < trimmedJson.length() - 1) {
            index = skipWhitespace(trimmedJson, index);
            if (trimmedJson.charAt(index) == '}') {
                return fields;
            }

            ParsedValue key = parseValue(trimmedJson, index);
            if (key.rawJson.charAt(0) != '"') {
                throw new IllegalArgumentException("Expected string field name in JSON object: " + trimmedJson);
            }

            index = skipWhitespace(trimmedJson, key.nextIndex);
            requireCharacter(trimmedJson, index, ':');
            index = skipWhitespace(trimmedJson, index + 1);

            ParsedValue value = parseValue(trimmedJson, index);
            fields.put(unquote(key.rawJson), value.rawJson);

            index = skipWhitespace(trimmedJson, value.nextIndex);
            char separator = trimmedJson.charAt(index);
            if (separator == ',') {
                index += 1;
                continue;
            }

            if (separator == '}') {
                return fields;
            }

            throw new IllegalArgumentException("Unexpected JSON separator '" + separator + "' in: " + trimmedJson);
        }

        return fields;
    }

    static String requireString(Map<String, String> fields, String fieldName) {
        return unquote(requireRaw(fields, fieldName));
    }

    static int requireInt(Map<String, String> fields, String fieldName) {
        return Integer.parseInt(requireRaw(fields, fieldName));
    }

    static String requireRaw(Map<String, String> fields, String fieldName) {
        String value = fields.get(fieldName);
        if (value == null) {
            throw new IllegalArgumentException("Missing JSON field: " + fieldName);
        }

        return value;
    }

    static String quote(String value) {
        String validatedValue = Objects.requireNonNull(value, "value");
        StringBuilder builder = new StringBuilder(validatedValue.length() + 2);
        builder.append('"');

        for (int index = 0; index < validatedValue.length(); index += 1) {
            char current = validatedValue.charAt(index);
            switch (current) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(current);
                    break;
            }
        }

        builder.append('"');
        return builder.toString();
    }

    private static ParsedValue parseValue(String json, int startIndex) {
        int index = skipWhitespace(json, startIndex);
        char current = json.charAt(index);

        if (current == '"') {
            return parseString(json, index);
        }

        if (current == '{') {
            return parseComposite(json, index, '{', '}');
        }

        if (current == '[') {
            return parseComposite(json, index, '[', ']');
        }

        return parseScalar(json, index);
    }

    private static ParsedValue parseString(String json, int startIndex) {
        for (int index = startIndex + 1; index < json.length(); index += 1) {
            char current = json.charAt(index);
            if (current == '\\') {
                index += 1;
                continue;
            }

            if (current == '"') {
                return new ParsedValue(json.substring(startIndex, index + 1), index + 1);
            }
        }

        throw new IllegalArgumentException("Unterminated JSON string: " + json.substring(startIndex));
    }

    private static ParsedValue parseComposite(String json, int startIndex, char open, char close) {
        int depth = 0;
        boolean inString = false;

        for (int index = startIndex; index < json.length(); index += 1) {
            char current = json.charAt(index);
            if (inString) {
                if (current == '\\') {
                    index += 1;
                    continue;
                }

                if (current == '"') {
                    inString = false;
                }

                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }

            if (current == open) {
                depth += 1;
                continue;
            }

            if (current == close) {
                depth -= 1;
                if (depth == 0) {
                    return new ParsedValue(json.substring(startIndex, index + 1), index + 1);
                }
            }
        }

        throw new IllegalArgumentException("Unterminated JSON value: " + json.substring(startIndex));
    }

    private static ParsedValue parseScalar(String json, int startIndex) {
        int index = startIndex;
        while (index < json.length()) {
            char current = json.charAt(index);
            if (current == ',' || current == '}' || current == ']' || Character.isWhitespace(current)) {
                break;
            }

            index += 1;
        }

        if (index == startIndex) {
            throw new IllegalArgumentException("Expected JSON scalar at index " + startIndex + " in: " + json);
        }

        return new ParsedValue(json.substring(startIndex, index), index);
    }

    private static int skipWhitespace(String json, int startIndex) {
        int index = startIndex;
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index += 1;
        }

        if (index >= json.length()) {
            throw new IllegalArgumentException("Unexpected end of JSON input: " + json);
        }

        return index;
    }

    private static void requireCharacter(String json, int index, char expected) {
        if (index >= json.length() || json.charAt(index) != expected) {
            throw new IllegalArgumentException("Expected '" + expected + "' at index " + index + " in: " + json);
        }
    }

    private static String unquote(String rawJsonString) {
        String validatedString = Objects.requireNonNull(rawJsonString, "rawJsonString");
        if (validatedString.length() < 2 || validatedString.charAt(0) != '"' || validatedString.charAt(validatedString.length() - 1) != '"') {
            throw new IllegalArgumentException("Expected quoted JSON string: " + validatedString);
        }

        StringBuilder builder = new StringBuilder(validatedString.length() - 2);
        for (int index = 1; index < validatedString.length() - 1; index += 1) {
            char current = validatedString.charAt(index);
            if (current != '\\') {
                builder.append(current);
                continue;
            }

            index += 1;
            if (index >= validatedString.length() - 1) {
                throw new IllegalArgumentException("Invalid JSON escape sequence: " + validatedString);
            }

            char escaped = validatedString.charAt(index);
            switch (escaped) {
                case '"':
                    builder.append('"');
                    break;
                case '\\':
                    builder.append('\\');
                    break;
                case '/':
                    builder.append('/');
                    break;
                case 'b':
                    builder.append('\b');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'u':
                    if (index + 4 >= validatedString.length() - 1) {
                        throw new IllegalArgumentException("Invalid unicode escape in JSON string: " + validatedString);
                    }

                    String hex = validatedString.substring(index + 1, index + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    index += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported JSON escape sequence: \\" + escaped);
            }
        }

        return builder.toString();
    }

    private static final class ParsedValue {
        private final String rawJson;
        private final int nextIndex;

        private ParsedValue(String rawJson, int nextIndex) {
            this.rawJson = rawJson;
            this.nextIndex = nextIndex;
        }
    }
}