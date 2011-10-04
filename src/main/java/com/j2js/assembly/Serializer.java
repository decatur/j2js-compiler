package com.j2js.assembly;

import java.util.List;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Serializer {
    private Writer writer;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy");
    
    /** Creates a new instance of Serializer */
    public Serializer(Writer theWriter) {
        writer = theWriter;
    }
   
    public void serialize(Object object) throws NotSerializableException {
        try {
            this.serializeInternal(object);
        } catch (IOException e) {
            throw new NotSerializableException(e.getMessage());
        }
    }
    
    private void serializeInternal(Object object) throws IOException {
        if (object==null) {
            writer.write("null");
        } else if (object instanceof String) {
            quote((String)object);
        } else if (object instanceof Number) {
            numberToString((Number) object);
        } else if (object instanceof Map) {
            serializeMap((Map) object);
        } else if (object instanceof List) {
            serializeArray((List) object);
        } else if (object.getClass().isArray()) {
            serializeArray((Object[]) object);// && object.getClass().getComponentType().equals(Double.TYPE) double[]) object);
        } else if (object instanceof Boolean) {
            writer.write(((Boolean)object).toString()); 
        } else if (object instanceof Date) {
            quote(dateFormatter.format((Date) object)); 
        } else {
            throw serializeError("Unknown object type " + object.getClass().toString());
        }
    }

    private void serializeArray(List list) throws IOException {
        String sep = "";
    
        writer.write('[');
        for (Object o : list) {
            writer.write(sep);
            sep = ",";
            this.serializeInternal(o);
        }
        writer.write(']');
    }
    
    private void serializeArray(Object[] array) throws IOException {
        String sep = "";
    
        writer.write('[');
        for (int i=0; i<array.length; i++) {
            writer.write(sep);
            sep = ",";
            serializeInternal(array[i]);
        }
        writer.write(']');
    }
    
    private void serializeMap(Map<String, ?> map) throws IOException {
        String sep = "";
    
        writer.write('{');
        for (String key : map.keySet()) {
            writer.write(sep);
            sep = ",";
            Object value = map.get(key);
            if (key.matches("\\w*")) {
                writer.write(key);
            } else {
                writer.write("\"" + key + "\"");
            }
            
            writer.write(':');
            this.serializeInternal(value);
        }
        writer.write('}');
    }
    
    /**
     * Stream a numeric value.
     * @exception NotSerializableException If number is infinite or not a number.
     * @param  n A Number
     * @return A String.
     */
    private void numberToString(Number n) throws IOException {
        if (n instanceof Double && (((Double) n).isInfinite() || ((Double) n).isNaN())) {
            throw serializeError("Can only serialize finite numbers");
        }
        if (n instanceof Float && (((Float) n).isInfinite() || ((Float) n).isNaN())) {
            throw serializeError("Can only serialize finite numbers");
        }

        writer.write(n.toString().toLowerCase());
    }
    
    private void numberToString(double n) throws IOException {
        writer.write(String.valueOf(n));
    }

    /**
     * Stream a string in double quotes " with the following backslash replacements:
     * " -> \"
     * \ -> \\
     * @param string A String
     */
    private void quote(String s) throws IOException {
        if (s == null) {
            writer.write("null");
            return;
        }
        writer.write('"');
        int l = s.length();
        for (int i=0; i<l; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    writer.write("\\\"");
                    break;
                case '\\':
                    writer.write("\\\\");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                default:
                    writer.write(c);
            }
        }
        writer.write('"');
    }
    
    private NotSerializableException serializeError(String message) {
        return new NotSerializableException(message);
    }
}
