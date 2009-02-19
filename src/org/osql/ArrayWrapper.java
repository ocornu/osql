/*
 * Copyright (C) Olivier Cornu 2004-2009 <o.cornu@gmail.com>
 * 
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This file is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.osql;


import java.util.StringTokenizer;


/**
 * @author zit
 *
 */
final class ArrayWrapper {

    
    String type;
    boolean primitive;
    int length;
    String content;
//    Object test = new int[3];
    
    private ArrayWrapper() {}
    
    
    ArrayWrapper (Connection conn, Object a, ObjectRecord pid, byte access)
    throws OSQLException {
//        this.array = a;
        this.length = java.lang.reflect.Array.getLength(a);
        Class c = a.getClass();
        
//System.out.print("Array:  "+t+"  ->  ");
        // Is this an array of objects?
        String cn = c.getName();
        if (cn.charAt(1)=='L') {
            this.primitive = false;
            this.type = cn.substring(2, cn.length()-1);
            
            if (this.type.equals("Byte")
                    || this.type.equals("Short")
                    || this.type.equals("Character")
                    || this.type.equals("Integer")
                    || this.type.equals("Long")
                    || this.type.equals("Float")
                    || this.type.equals("Double")) {
                this.content = primitiveContent(a);
                return;
            }
            
            if (this.type.equals("Boolean")) {
                this.content = booleanContent(a);
                return;
            }

            this.content = objectContent(conn, a, pid, access);
            return;
        }
        
        this.primitive = true;

        if (c==boolean[].class) {
            this.type = "boolean";
            this.content = booleanContent(a);
            return;
        }

        if (c==byte[].class)
            this.type = "byte";
        else if (c==char[].class)
            this.type = "char";
        else if (c==short[].class)
            this.type = "short";
        else if (c==int[].class)
            this.type = "int";
        else if (c==long[].class)
            this.type = "long";
        else if (c==float[].class)
            this.type = "float";
        else if (c==double[].class)
            this.type = "double";
        this.content = primitiveContent(a);
    }
    

    private String booleanContent(Object array) {
        if (length==0)
            return "";
        StringBuffer sb = new StringBuffer();
        boolean b;
        for (int i=0; i<length; i++) {
            b = java.lang.reflect.Array.getBoolean(array, i);
            if (b)
                sb.append('1');
            else
                sb.append('0');
        }
        return sb.toString();
    }
    
    
    private String primitiveContent(Object array) {
        if (length==0)
            return "";
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<length; i++) {
            sb.append(java.lang.reflect.Array.get(array, i));
            sb.append(';');
        }
        return sb.toString();
    }
    

    private String objectContent(Connection conn, Object array, ObjectRecord pid, byte access)
    throws OSQLException {
        if (length==0)
            return "";
        StringBuffer sb = new StringBuffer();
        Object o;
        for (int i=0; i<length; i++) {
            o = java.lang.reflect.Array.get(array, i);
            if (o!=null) 
                sb.append(conn.db.store (conn, o, pid, access));
            sb.append(";");
        }
        return sb.toString();
    }
    
    
    Object dump(Connection conn)
    throws OSQLException {
        Class c;
        try {
             c = Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new OSQLException("Class not found: "+type+"\nFailed to dump array content.", e);
        }
        
        Object a = java.lang.reflect.Array.newInstance(c, length);
        // Array has zero length?
        if (length==0)
            return a;
        
        System.out.println(content);
        StringTokenizer st = new StringTokenizer(this.content, ";");
        // Content size does not match array length?
        if (length!=st.countTokens())
            throw new OSQLException("Array content size does not match expected size:\nExpected size:  "+length+"Content size:   "+st.countTokens());

        // Restore content
        
        // Object array?
        if (!primitive) {
            if (type.equals("Boolean"))
                return restoreBoolean(a, st);
            if (type.equals("Byte"))
                return restoreByte(a, st);
            if (type.equals("Short"))
                return restoreShort(a, st);
            if (type.equals("Character"))
                return restoreChar(a, st);
            if (type.equals("Integer"))
                return restoreInt(a, st);
            if (type.equals("Long"))
                return restoreLong(a, st);
            if (type.equals("Float"))
                return restoreFloat(a, st);
            if (type.equals("Double"))
                return restoreDouble(a, st);
            String id;
            for (int i=0; st.hasMoreTokens(); i++) {
                id = st.nextToken();
                if (id=="") {
                    // NULL value
                    java.lang.reflect.Array.set(a, i, null);
                    continue;
                }
                // get objects from database
                java.lang.reflect.Array.set(a, i, conn.select(c, Integer.parseInt(id)));
            }
            return a;
        }
        
        // "primitive" class array
        if (type.equals("boolean"))
            return restoreBoolean(a, st);
        if (type.equals("byte"))
            return restoreByte(a, st);
        if (type.equals("short"))
            return restoreShort(a, st);
        if (type.equals("char"))
            return restoreChar(a, st);
        if (type.equals("int"))
            return restoreInt(a, st);
        if (type.equals("long"))
            return restoreLong(a, st);
        if (type.equals("float"))
            return restoreFloat(a, st);
        if (type.equals("double"))
            return restoreDouble(a, st);
    
        throw new OSQLException("Unknown type, or type should be primitive.\nType:  "+type+"      Primitive:  "+(primitive ? "yes":"no"));
    }
    
    
    private Object restoreBoolean(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            // False?
            if (b.equals("0")) {
                if (primitive)
                    java.lang.reflect.Array.setBoolean(array, i, false);
                else
                    java.lang.reflect.Array.set(array, i, new Boolean(false));
                continue;
            }
            if (primitive)
                java.lang.reflect.Array.setBoolean(array, i, true);
            else
                java.lang.reflect.Array.set(array, i, new Boolean(true));
        }
        return array;
    }
    

    private Object restoreByte(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            if (primitive)
                java.lang.reflect.Array.setByte(array, i, Byte.parseByte(b));
            else
                java.lang.reflect.Array.set(array, i, new Byte(b));
        }
        return array;
    }
    

    private Object restoreChar(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            if (primitive)
                java.lang.reflect.Array.setChar(array, i, b.charAt(0));
            else
                java.lang.reflect.Array.set(array, i, new Character(b.charAt(0)));
        }
        return array;
    }
    

    private Object restoreShort(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            if (primitive)
                java.lang.reflect.Array.setShort(array, i, Short.parseShort(b));
            else
                java.lang.reflect.Array.set(array, i, new Short(b));
        }
        return array;
    }
    

    private Object restoreInt(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            if (primitive)
                java.lang.reflect.Array.setInt(array, i, Integer.parseInt(b));
            else
                java.lang.reflect.Array.set(array, i, new Integer(b));
        }
        return array;
    }
    

    private Object restoreLong(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            if (primitive)
                java.lang.reflect.Array.setLong(array, i, Long.parseLong(b));
            else
                java.lang.reflect.Array.set(array, i, new Long(b));
        }
        return array;
    }
    

    private Object restoreFloat(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            if (primitive)
                java.lang.reflect.Array.setFloat(array, i, Float.parseFloat(b));
            else
                java.lang.reflect.Array.set(array, i, new Float(b));
        }
        return array;
    }
    

    private Object restoreDouble(Object array, StringTokenizer content) {
        String b;
        for (int i=0; content.hasMoreTokens(); i++) {
            b = content.nextToken();
            if (primitive)
                java.lang.reflect.Array.setDouble(array, i, Double.parseDouble(b));
            else
                java.lang.reflect.Array.set(array, i, new Double(b));
        }
        return array;
    }
    

}
