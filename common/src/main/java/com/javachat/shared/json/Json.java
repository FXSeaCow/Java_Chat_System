package com.javachat.shared.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small JSON codec for the local desktop/server API. */
public final class Json {
    private Json() { }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    public static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (parser.position != source.length()) throw new IllegalArgumentException("Trailing JSON data");
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("Expected JSON object");
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object value) {
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Expected JSON array");
        return (List<Object>) list;
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) out.append("null");
        else if (value instanceof String text) {
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                        else out.append(c);
                    }
                }
            }
            out.append('"');
        } else if (value instanceof Number || value instanceof Boolean) out.append(value);
        else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (var entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                write(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof Iterable<?> values) {
            out.append('[');
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                first = false;
                write(item, out);
            }
            out.append(']');
        } else throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    private static final class Parser {
        private final String source;
        private int position;

        Parser(String source) { this.source = source; }

        void space() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
        }

        Object value() {
            space();
            if (position >= source.length()) throw new IllegalArgumentException("Unexpected end of JSON");
            return switch (source.charAt(position)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Object literal(String text, Object value) {
            if (!source.startsWith(text, position)) throw new IllegalArgumentException("Invalid JSON literal");
            position += text.length();
            return value;
        }

        private Map<String, Object> object() {
            position++;
            Map<String, Object> map = new LinkedHashMap<>();
            space();
            if (consume('}')) return map;
            do {
                space();
                if (position >= source.length() || source.charAt(position) != '"')
                    throw new IllegalArgumentException("Expected JSON key");
                String key = string();
                space();
                expect(':');
                map.put(key, value());
                space();
                if (consume('}')) return map;
                expect(',');
            } while (true);
        }

        private List<Object> array() {
            position++;
            List<Object> list = new ArrayList<>();
            space();
            if (consume(']')) return list;
            do {
                list.add(value());
                space();
                if (consume(']')) return list;
                expect(',');
            } while (true);
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (position < source.length()) {
                char c = source.charAt(position++);
                if (c == '"') return out.toString();
                if (c != '\\') {
                    if (c < 0x20) throw new IllegalArgumentException("Invalid JSON control character");
                    out.append(c);
                    continue;
                }
                if (position >= source.length()) throw new IllegalArgumentException("Invalid JSON escape");
                char escaped = source.charAt(position++);
                switch (escaped) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (position + 4 > source.length()) throw new IllegalArgumentException("Invalid Unicode escape");
                        out.append((char) Integer.parseInt(source.substring(position, position + 4), 16));
                        position += 4;
                    }
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private Number number() {
            int start = position;
            if (consume('-')) { /* optional sign */ }
            while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            boolean decimal = false;
            if (consume('.')) {
                decimal = true;
                while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            }
            if (consume('e') || consume('E')) {
                decimal = true;
                if (!consume('+')) consume('-');
                while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            }
            if (start == position) throw new IllegalArgumentException("Expected JSON value");
            String number = source.substring(start, position);
            try {
                return decimal ? Double.valueOf(number) : Long.valueOf(number);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid JSON number", exception);
            }
        }

        private boolean consume(char expected) {
            if (position < source.length() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) throw new IllegalArgumentException("Expected '" + expected + "'");
        }
    }
}
