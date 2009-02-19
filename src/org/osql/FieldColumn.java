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


import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;


class FieldColumn {


    // SQL column types
    public static final String BOOLEAN  = "BOOLEAN";
    public static final String TINYINT  = "TINYINT";
    public static final String SMALLINT = "SMALLINT";
    public static final String CHAR     = "CHAR";
    public static final String INTEGER  = "INTEGER";
    public static final String BIGINT   = "BIGINT";
    public static final String FLOAT    = "FLOAT";
    public static final String VARCHAR  = "VARCHAR";
    public static final String DATE 	= "DATETIME";
    
    
    // Column definition
    public final Field      field;
    public final String     sqlType;
    public final int        type;
    private FieldColumn     next;

    
    private FieldColumn (Field field, String sqlType, int type) {
        // Keep an accessible reference to this Field
        field.setAccessible(true);
        this.field = field;
        // Keep types
        this.sqlType = sqlType;
        this.type = type;
        // Next column
        this.next = null;
    }
    
    
    final int store (Connection conn, ObjectRecord r, byte access, PreparedStatement row, int pos)
    throws OSQLException, SQLException {
        try {
            set (conn, r, access, row, pos);
        } catch (IllegalAccessException e) {
            // Should never be thrown, cause we jump over VM access restrictions
            throw new OSQLException("/!\\ ERROR:  Object field is not accessible !!!"+
                    "\nCould not READ field '"+field.getName()+
                    "' from object "+r.object()+" #"+r.id, e);
        }
        pos++;
        if (next!=null)
            return next.store (conn, r, access, row, pos);
        return pos;
    }
    
    
    final int restore (Connection conn, Object o, ResultSet row, int pos)
    throws OSQLException, SQLException {
        try {
            get(conn, o, row, pos);
        } catch (IllegalAccessException e) {
            // Should never be thrown, cause we jump over VM access restrictions
            throw new OSQLException("/!\\ ERROR:  Object field is not accessible !!!"+
                    "\nCould not SET field '"+field.getName()+
                    "' from object "+o, e);
        }
        pos++;
        if (next!=null)
            return next.restore(conn, o, row, pos);
        return pos;
    }
    
    
    protected void set (Connection conn, ObjectRecord r, byte access, PreparedStatement row, int pos)
    throws OSQLException, SQLException, IllegalAccessException {
    	if (r.object()==null)
    		System.out.println("Record object: "+r.object());
        row.setObject(pos, field.get(r.object()), type);
    }
    
    
    protected void get (Connection conn, Object o, ResultSet row, int i)
    throws OSQLException, SQLException, IllegalAccessException {
        field.set(o, row.getObject(i));
    }
    
    
    final FieldColumn next() {
        return next;
    }
    
    final void hash(Object o, StringBuffer buf)
    throws IllegalAccessException {
    	buf.append('|');
    	buf.append(field.get(o));
    	if (next!=null)
    		next.hash(o, buf);
    }
    
    
    /**
     * Public Column factory.
     * 
     * @param field Field to create a column for.
     * @return
     */
    private static FieldColumn newInstance(Field field) {
        Class c = field.getType();
        
        // Primitive types
        if (c==boolean.class)
            return new FieldColumn(field, BOOLEAN, Types.BOOLEAN);
//            return new _boolean(field);
        if (c==byte.class)
//            return new Column(field, TINYINT, Types.TINYINT);
            return new _byte(field);
        if (c==short.class)
//            return new Column(field, SMALLINT, Types.SMALLINT);
            return new _short(field);
        if (c==char.class)
//            return new Column(field, CHAR, Types.CHAR);
            return new _char(field);
        if (c==int.class)
            //return new FieldColumn(field, INTEGER, Types.INTEGER);
            return new _int(field);
        if (c==long.class)
//            return new FieldColumn(field, BIGINT, Types.BIGINT);
            return new _long(field);
        if (c==float.class)
//            return new Column(field, FLOAT, Types.FLOAT);
            return new _float(field);
        if (c==double.class)
            return new FieldColumn(field, FLOAT, Types.FLOAT);
//            return new _double(field);
        
        // Primitive wrappers
        if (c==Boolean.class)
            return new FieldColumn(field, BOOLEAN, Types.BOOLEAN);
//            return new _Boolean(field);
        if (c==Byte.class)
            return new FieldColumn(field, TINYINT, Types.TINYINT);
//            return new _Byte(field);
        if (c==Short.class)
            return new FieldColumn(field, SMALLINT, Types.SMALLINT);
//            return new _Short(field);
        if (c==Character.class)
          return new FieldColumn(field, CHAR, Types.CHAR);
//            return new _Character(field);
        if (c==Integer.class)
            return new FieldColumn(field, INTEGER, Types.INTEGER);
//            return new _Integer(field);
        if (c==Long.class)
            return new FieldColumn(field, BIGINT, Types.BIGINT);
//            return new _Long(field);
        if (c==Float.class)
          return new FieldColumn(field, FLOAT, Types.FLOAT);
//            return new _Float(field);
        if (c==Double.class)
          return new FieldColumn(field, FLOAT, Types.FLOAT);
//            return new _Double(field);
        
        // Strings
        if (c==String.class)
            return new FieldColumn(field, VARCHAR, Types.VARCHAR);
//            return new _String(field);
        
        if (c==java.util.Date.class)
            return new FieldColumn(field, DATE, Types.DATE);
//            return new _String(field);

        // Objects
        return new _Object(field);
    }


    static synchronized FieldColumn create (Vector fields) {
        if (fields.size()==0)
            return null;
        FieldColumn first = newInstance((Field)fields.firstElement());
        if (fields.size()==1)
            return first;
        FieldColumn previous = first;
        Iterator i = fields.iterator();
        i.next();
        while (i.hasNext()) {
            previous.next = newInstance((Field)i.next());
            previous = previous.next;
        }
        return first;
    }
    
    
/*
 * -------------------------------------------------------
 */    
    
    
    
/*    static class _Boolean
    extends Column {
        
        private _Boolean (Field field) {
            super(field, BOOLEAN, Types.BOOLEAN);
        }
        
    }



    static class _boolean
    extends _Boolean {
        
        private _boolean (Field field) {
            super(field);
        }
        
        protected void get (Object o, ResultSet row, int i, Database db)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.setBoolean(o, row.getBoolean(i));
        }
        
    }



    static class _Byte
    extends Column {
        
        private _Byte (Field field) {
            super(field, TINYINT, Types.TINYINT);
        }
        
    }*/



    static class _byte
    extends FieldColumn {
        
        private _byte (Field field) {
            super(field, TINYINT, Types.TINYINT);
        }
        
        protected void get (Connection conn, Object o, ResultSet row, int i)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.setByte(o, row.getByte(i));
        }
        
    }



/*    static class _Short
    extends Column {
        
        private _Short (Field field) {
            super(field, SMALLINT, Types.SMALLINT);
        }
        
    }*/



    static class _short
    extends FieldColumn {
        
        private _short (Field field) {
            super(field, SMALLINT, Types.SMALLINT);
        }
        
        protected void get (Connection conn, Object o, ResultSet row, int i)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.setShort(o, row.getShort(i));
        }
        
    }



    static class _Integer
    extends FieldColumn {
        
        private _Integer (Field field) {
            super(field, INTEGER, Types.INTEGER);
        }
        
    }



    static class _int
    extends _Integer {
        
        private _int (Field field) {
            super(field);
        }
        
        protected void get (Connection conn, Object o, ResultSet row, int i)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.setInt(o, row.getInt(i));
        }
        
    }



    static class _Long
    extends FieldColumn {
        
        private _Long (Field field) {
            super(field, BIGINT, Types.BIGINT);
        }
        
    }



    static class _long
    extends _Long {
        
        private _long (Field field) {
            super(field);
        }
        
        protected void get (Connection conn, Object o, ResultSet row, int i)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.setLong(o, row.getLong(i));
        }
        
    }


/*
    static class _Float
    extends Column {
        
        private _Float (Field field) {
            super(field, FLOAT, Types.FLOAT);
        }
        
    }*/



    static class _float
    extends FieldColumn {
        
        private _float (Field field) {
            super(field, FLOAT, Types.FLOAT);
        }
        
        protected void get (Connection conn, Object o, ResultSet row, int i)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.setFloat(o, row.getFloat(i));
        }
        
    }



/*    static class _Double
    extends Column {
        
        private _Double (Field field) {
            super(field, FLOAT, Types.FLOAT);
        }
        
    }



    static class _double
    extends _Double {
        
        private _double (Field field) {
            super(field);
        }
        
        protected void get (Object o, ResultSet row, int i, Database db)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.setDouble(o, row.getDouble(i));
        }
        
    }



    static class _Character
    extends Column {
        
        private _Character (Field field) {
            super(field, CHAR, Types.CHAR);
        }
        
    }*/



    static class _char
    extends FieldColumn {
        
        private _char (Field field) {
            super(field, CHAR, Types.CHAR);
        }
        
        protected void get (Connection conn, Object o, ResultSet row, int i)
        throws OSQLException, SQLException, IllegalAccessException {
            field.setChar(o, row.getString(i).charAt(0));
        }
        
    }



/*    static class _String
    extends Column {
        
        private _String (Field field) {
            super(field, VARCHAR, Types.VARCHAR);
        }
        
        protected void get (Object o, ResultSet row, int i, Database db)
        throws OSQLException, SQLException, IllegalAccessException { 
            field.set(o, row.getString(i));
        }
        
    }*/



    static final class _Object
    extends FieldColumn {
        
        private _Object (Field field) {
            super(field, INTEGER, Types.INTEGER);
        }
        
        protected void set (Connection conn, ObjectRecord r, byte access, PreparedStatement row, int pos)
        throws OSQLException, SQLException, IllegalAccessException {
            Object fo = field.get(r.object());
            if (fo==null) {
                row.setObject(pos, null);
                return;
            }
            if (fo.getClass().isArray()) {
            	System.out.println(fo.getClass());
            	fo = new ArrayWrapper(conn, fo, r, access);
//            	return;
            }
            row.setInt(pos, conn.store(fo, r, access).id.intValue());
        }

        protected void get (Connection conn, Object o, ResultSet row, int i)
        throws OSQLException, SQLException, IllegalAccessException {
            // Get object ID
            Integer oid = (Integer)row.getObject(i);
            // Null field value?
            if (oid==null) {
                field.set(o, null);
                return;
            }
            // Do we have this object in cache?
/*            Object fo = db.ids.get(oid);
            if (fo!=null) {
                field.set(o, ((WeakReference)fo).get());
                return;
            }*/
            // Already processing this object?
            Object fo = null;
/*            Object fo = conn.db.processedIds.get(oid);
            if (fo!=null) {
                field.set(o, ((ObjectRecord)((WeakReference)fo).get()).object.get());
                return;
            }*/
            // Load object from database
//            System.out.println("type:"+field.getType());
            fo = conn.select(field.getType(), oid.intValue());
            if (fo.getClass()==ArrayWrapper.class)
            	field.set(o, ((ArrayWrapper)fo).dump(conn));
            else
            	field.set(o, fo);
        }
        
    }



}
