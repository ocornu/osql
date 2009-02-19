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


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;
import java.util.Vector;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;


/**
 * 
 *
 * @author Olivier S. Cornu
 */
public final class ClassTable {

    
    static final String OSQL_TABLE_NAME = ".Class";
    public static final String INIT_METHOD = "_init_";
    
    
    // Class info
    short id;
    private final	Class	clazz;
    final			String	className;
//    final			String	tableName;
    public final			ClassTable	superClassTable;
    final			ClassTable	topClassTable;
    final			boolean isFinal;

    // Optionnal features
    public final			TableProperties table;
    int pkIdx;
    
    // Storable columns (reference to first column)
    final			FieldColumn	columns;
    
    // Special methods
    private final	Constructor constructor;
    private final	Method      initializer;
	

    // Prepared SQL queries
//    final String insert;
//    final String update;
    // SQL VIEW elements
    private String viewFields;
    private String viewJoin;
    private String viewUser;
    private String viewGroup;
    private String viewEnd;

    // Cached prepared statements
    private StatementPool insertPool;
    private StatementPool updatePool;
    
//    transient WeakHashMap records;           // Existing object --> ID
//    transient WeakHashMap ids;    
	final ObjectCache cache;
    
    
    private ClassTable(Class c, ClassTable sc, ClassTable tc, java.sql.Connection conn)//, TableNameMapper mapper)
    throws OSQLException {
        if (c==Object.class)
        	throw new OSQLException("Cannot save java.lang.Object objects.");
        
        // Class info
        this.clazz = c;
        this.className = c.getName();
        this.superClassTable = sc;
        if (tc==null)
        	tc = this;
        this.topClassTable = tc;
        this.isFinal = Modifier.isFinal(c.getModifiers());

        // Optionnal features
        this.table = new TableProperties(c);//, mapper);
/*       	this.tableName = props.tableName;
       	this.hasIndex = props.hasIndex;
       	this.hasAccessRights = props.hasAccessRights;
       	this.isCached = props.isCached;
       	this.isTextTable = props.isTextTable;
        */
//        System.out.println(this.isFinal+" "+this.hasAccessRights+" "+this.hasIndex);

//        this.records = new WeakHashMap();
//        this.ids = new WeakHashMap();
        
        // One cache per object filiation
        if (tc==this)
        	this.cache = new ObjectCache();
        else
        	this.cache = tc.cache;
        
        // Find the empty constructor (used for object restoration)
        try {
            // Get empty constructor
            constructor = clazz.getDeclaredConstructor(null);
            // Make it accessible over standard access restrictions
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new OSQLException("Empty constructor missing.\nClass "+className+" thus cannot be restored.", e);
        }
        
        // Find the _init_() method (used once object has been restored)
        Method init = null;
        try {
        		init = clazz.getDeclaredMethod(INIT_METHOD, Connection.class);
        		init.setAccessible(true);
        } catch (NoSuchMethodException e) {
//            init = null;
        }
        initializer = init;
//        if (initializer!=null)
//        		initializer.setAccessible(true);
        
        // Build the list of class columns relevant for storage
        // and SQL queries for this table 
        
/*        // Shortcut for java.lang.Record class table
        if (superclass==null) {
            // INSERT query  (Table fields: id, class, uid, gid, access)
            this.insert = "INSERT into \""+this.tableName+"\" VALUES (NULL, ?, ?, ?, ?);";
//System.out.println("Insert query:  "+insert);       // Log
            // UPDATE query
            this.update = null;
//            this.update = "UPDATE \""+tableName+"\" SET \".parent\"=? WHERE \".id\"=?";
//System.out.println("Update query:  "+update);       // Log
            // Fields meta data
            this.columns = null;
            createView();
            return;
        }*/
        
        // Standard class (not java.lang.0bject)
        // INSERT query
        StringBuffer iq = new StringBuffer("INSERT into \"");
        iq.append(table.name);
        iq.append("\" VALUES (");         // Object ID placeholder
        // UPDATE query
        StringBuffer uq = new StringBuffer("UPDATE \"");
        uq.append(table.name);
        uq.append("\" SET ");              // Object ID placeholder

        int fieldIdx = 0;
        if (sc==null) { 
	        if (table.primaryKey!=null) {
	        	if (table.autoIndex) {
	        		fieldIdx++;
	        		pkIdx = fieldIdx;
//	        		System.out.println("pkIdx="+pkIdx);
		            // Top-level class ?
			        if (superClassTable==null)
			        	iq.append("NULL");
			        else
			        	iq.append("?");
	        	}
		        // Has children ?
		        if (!isFinal) {
		        	if (fieldIdx>0)
		        		iq.append(", ");
	        		fieldIdx++;
		        	iq.append("?");
		        }
		        // Has access rights ?
		       	if (table.hasAccessRights) {
		        	if (fieldIdx>0)
		        		iq.append(", ");
		       		fieldIdx += 3;
			       	iq.append("?, ?, ?");
		       	}
	        }
        } else {
        	fieldIdx++;
    		pkIdx = fieldIdx;
        	iq.append("?");
        }
        
        // Loop through all columns
        Field[] fds = clazz.getDeclaredFields();    // All columns declared in class c
        Vector fv = new Vector(fds.length);     // Vector to hold relevant columns only
        Field f; int m;
        for (int i=0; i<fds.length; i++) {
            f = fds[i];
            m = f.getModifiers();
            if (Modifier.isTransient(m) || Modifier.isStatic(m))
                continue;                  // Ignore transient and static columns
            // INSERT query
            if (fieldIdx>0)
                iq.append(", ");
           	iq.append("?");              // Add field value placeholder
           	fieldIdx++;
            // UPDATE query
            if (fv.size()==0)
                uq.append("\"");
            else
                uq.append(", \"");
            uq.append(f.getName());
            uq.append("\"=?");
//          Add storable field f
            fv.add(f);
        }
        iq.append(")");
        
        // Store INSERT query string
        insertPool = new StatementPool(conn, iq.toString());
//        this.insert = iq.toString();
//System.out.println("Insert query:  "+insert);       // Log

        // Do we have columns to work on?
        if (fv.size()==0) {
            this.updatePool = null;
            this.columns = null;
        } else {
	        // Store UPDATE query string
	        uq.append(" WHERE \""+table.primaryKey+"\"=?");
	        this.updatePool = new StatementPool(conn, uq.toString());
	//        this.update = uq.toString();
	//System.out.println("Update query:  "+update);       // Log
	        // Store relevant columns
	        this.columns = FieldColumn.create(fv);
        }
        createView();
    }
    
    
    ClassTable (Connection conn, Class c, ClassTable sc, ClassTable tc)
    throws OSQLException {
        this(c, sc, tc, conn.db.connection);//, conn.db.tableNameMapper);
        StringBuffer q = new StringBuffer("SELECT TOP 1 \"id\" FROM \"");
        q.append(OSQL_TABLE_NAME);
        q.append("\" ORDER BY \"id\" DESC");
        ResultSet rs;
        try {
            rs = conn.db.statement.executeQuery(q.toString());
        } catch (SQLException e) {
            throw new OSQLException("System table \""+OSQL_TABLE_NAME+"\" is probably missing.\nCould not execute SQL query: "+q.toString(), e);
        }
        try {
            rs.next();
            id = (short)(rs.getShort(1)+1);
        } catch (SQLException e) {
        	// "No data available" SQL Exception
//        	e.printStackTrace();
        	id = (short)0;
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new OSQLException("Could not close SQL result set:\n"+rs.toString(), e);
            }
        }
        if (id<0)
        	throw new OSQLException("Maximum number of classes (32768) reached. Could not store class \""+c.getName()+"\".");
        q.setLength(0);
        q.append("INSERT INTO \"");
        q.append(OSQL_TABLE_NAME);
        q.append("\" VALUES (");
        q.append(id);
        q.append(", '");
        q.append(c.getName());
        q.append("', ");
        if (sc!=null)
            q.append(sc.id);
        else
            q.append("NULL");
        q.append(")");
//conn.db.out.println(q.toString());
        try {
            conn.db.statement.executeUpdate(q.toString());
        } catch (SQLException e) {
            throw new OSQLException("Could not INSERT new table record for class \""+c.getName()+"\".\n"+q.toString(),e);
        }
        createTable(conn);
/*        insertPool = new StatementPool(conn.db.connection, insert);
        if (update!=null)
        	updatePool = new StatementPool(conn.db.connection, update);
*/    }
    
    
    ClassTable (Connection conn, short id, Class c, ClassTable sc, ClassTable tc)
    throws OSQLException {
        this(c, sc, tc, conn.db.connection);//, conn.db.tableNameMapper);
        this.id = id;
/*        insertPool = new StatementPool(conn.db.connection, insert);
        if (update!=null)
        	updatePool = new StatementPool(conn.db.connection, update);
*/	}
	
    
    /**
     * 
     * @param o
     * @param c
     * @param n
     * @param p
     * @return
     * @throws SQLException
     */
    ObjectRecord insertRecord (Connection conn, Object o, byte access, ObjectRecord p, ClassTable top)
    throws OSQLException {
        // Get a SQL INSERT statement for this class
        PreparedStatement row = insertPool.get();

        try {
            // Top of the object hierarchy?
            if (superClassTable==null) {
                // Create new object record
            	int i = 1;
            	if (!isFinal)
            		row.setInt(i++, top.id);              // Class name
                // Parent object, or owner object (null if none)
/*                if (p==null)                        // Parent object
                    row.setBoolean(2, true);
                else
                    row.setBoolean(2, false);*/
//                    row.setInt(2, p.id.intValue());
                if (table.hasAccessRights) {
	                row.setShort(i++, conn.user.id);     // User
	                row.setShort(i++, conn.user.group);               // Group
	                row.setByte(i++, access);     // Access rights
                }
                ObjectRecord r = new ObjectRecord(this, conn.user.id, conn.user.group, null, o, access);
                conn.db.requestCache.put(o, r);
                // Loop through columns
                if (columns!=null)
                	columns.store(conn, r, access, row, i);
                row.executeUpdate();
                /*
                 * Retrieve object ID
                 */ 
                if (table.autoIndex) {
                	// Auto indexing needs a db request to retrieve id
	                ResultSet rs = conn.db.statement.executeQuery("CALL IDENTITY();");
	                rs.next();
	                r.id = table.primaryKey!=null ? new Integer(rs.getInt(1)) : null;
	                rs.close();
                } else if (table.primaryKey!=null)
                	r.id = getPrimaryKey(o);
/*                	try {
	                	Field pk = clazz.getDeclaredField(table.primaryKey);
	                	pk.setAccessible(true);
	                	r.id = new Integer(pk.getInt(o));
                	} catch (NoSuchFieldException e) {
                		throw new OSQLException("No such primary key field", e);
                	} catch (IllegalAccessException e) {
                		throw new OSQLException("Can't access primary key field", e);
                	}*/
//                else
//                	r.id = null;
                	
/*                if (p!=null)
                    try {
                        conn.db.statement.executeUpdate("INSERT INTO \".Ref\" VALUES ("+p.id+", "+r.id+")");
                    } catch (SQLException e) {
                        throw new OSQLException("Could not INSERT reference for parent/child "+p.id+"/"+r.id, e);
                    }*/
                insertPool.release(row);
                conn.db.requestCache.put(o, r);
//                System.out.println("oid="+r.id+"  pkIdx="+pkIdx);
                return r;
            }
    
            // Get object ID from its superclass and save object
            ObjectRecord r = superClassTable.insertRecord(conn, o, access, p, top);
            row.setInt(pkIdx, r.id.intValue());
    
/*            if (clazz==Array.class) {
                Array a = new Array(conn, o, p, access);
                o = a;
            }*/
                
            // Loop through columns, if any
            if (columns!=null)
            	columns.store(conn, r, access, row, 2);
            row.executeUpdate();

            // Close or free INSERT statement
            insertPool.release(row);
            return r;
        } catch (SQLException e) {
            throw new OSQLException("Could not insert record:\n"+row.toString(), e);
        }
    }

    
    /**
     * 
     * @param o
     * @param c
     * @param n
     * @param p
     * @return
     * @throws SQLException
     */
    ObjectRecord updateRecord (Connection conn, ObjectRecord r, byte access, ObjectRecord p)
    throws OSQLException, OSQLAccessViolation {
//    	System.out.println()
        if (!r.isWritable(conn.user))
            throw new OSQLAccessViolation("Object is not writable.");
//            return r;
        return update(conn, r, access, p);
    }

    /**
     * 
     * @param o
     * @param c
     * @param n
     * @param p
     * @return
     * @throws SQLException
     */
    private ObjectRecord update (Connection conn, ObjectRecord r, byte access, ObjectRecord p)
    throws OSQLException {
/*    	if (!conn.db.processedObjects.containsKey(r.object.get())) {
    		conn.db.processedObjects.put(r.object.get(), r);
    		conn.db.processedIds.put(r.id, new WeakReference(r));
    	}*/
//    	conn.db.requestCache.put(r.object(), r);
        if (superClassTable!=null) {
        	// Update superclass record
            superClassTable.update(conn, r, access, p);
        }
        
        if (updatePool==null)
        	return r;
        // Get a SQL UPDATE statement for this class
        PreparedStatement row = updatePool.get();
        try {
/*            // c is java.lang.Record ?
            if (superclass==null) {
                // Update object record
                // Parent object id (null if none)
                if (p==null)
                    row.setObject(1, null);
                else
                    row.setInt(1, p.id.intValue());
                row.setInt(2, r.id.intValue());
                row.executeUpdate();
                // Close or free UPDATE statement
                updatePool.release(row);
                conn.db.processedObjects.put(r.object.get(), r);
                conn.db.processedIds.put(r.id, new WeakReference(r));
                return r;
            }*/
    
            // Get object ID from its superclass and save object
    
/*            if (clazz==Array.class) {
                Array a = new Array(r, db);
                r = a;
            }*/
                
            // Loop through columns
            row.setInt(columns.store(conn, r, access, row, 1), r.id.intValue());

            row.executeUpdate();
            updatePool.release(row);
            return r;
        } catch (SQLException e) {
            throw new OSQLException("Could not update record:\n"+row.toString(), e);
        }
    }
	
    
    Object restoreObject (Connection conn, ResultSet rs)
    throws OSQLException {
        Integer oid = null;
        String name = null;
        short uid = 0;
        short gid = 1;
        byte access = (byte)0xff;
//        boolean topLevel;
        int i = 1;
        try {
            // Get object information
        	if (table.autoIndex)
        		oid = new Integer(rs.getInt(i++));
        	else if (table.primaryKey!=null)
        		oid = new Integer(rs.getInt(table.primaryKey));
            if (!isFinal)
            	name = rs.getString(i++);
            if (table.hasAccessRights) {
	            uid = rs.getShort(i++);
	            gid = rs.getShort(i++);
	            access = rs.getByte(i++);
            }
//            topLevel = rs.getBoolean(3);
        } catch (SQLException e) {
            if (e.getMessage().equals("No data is available"))
                return null;
            throw new OSQLException("SQL exception during instance building.:\nClass: "+className, e);
        }

        // Check if we have this object in cache already
        ObjectRecord r;
        if (table.autoIndex || table.primaryKey!=null) { 
	        // Is this object from this class / a subclass of this class ?
	    	if (isFinal || className.equals(name))
	        	r = cache.getById(oid);
	    	else {
	    		r = conn.db.getClassTable(name).cache.getById(oid);
	    		if (r==null)
	        		return conn.select(name, oid.intValue());
	    	}
	    	// Do we have this object already?
	        if (r!=null) {
	//conn.db.out.println("### Select() - GET FROM CACHE  #"+oid);
	            Object o = r.object();
	            if (o!=null) {
		            conn.db.requestCache.put(o, r);
		            if (conn.db.logLevel<Database.LOG_VERBOSE)
		                return o;
		            // Log objet restore
		            StringBuffer log = new StringBuffer("[");
		            log.append(conn.user.login);
		            log.append("] <-C-< ");
		            log.append(this.className);
		            log.append(" #");
		            log.append(r.id);
		            if (conn.db.logLevel>Database.LOG_VERBOSE) {
		                log.append("   {");
		                log.append(o);
		                log.append("}");
		            }
		            conn.db.out.println(log.toString());
		            return o;
	            }
	        }
        }
        
        // Build object from scratch
        Object o;
        try {
            // Create instance
            o = constructor.newInstance(null);
        } catch (InstantiationException e) {
            throw new OSQLException("Could not instantiate class.\nClass: "+Modifier.toString(clazz.getModifiers())+" "+className+"\nTip: Is it an abstract class?", e);
        } catch (InvocationTargetException e) {
            throw new OSQLException("Constructor threw an exception.\nClass: "+className, e);
        } catch (IllegalAccessException e) {
            // Should never happen! Every elements are set to be accessible.
            throw new OSQLException("/!\\ ERROR:  Could not access object constructor for "+className+" !", e);
        }
        r = new ObjectRecord(this, uid, gid, oid, o, access);
//        conn.db.requestCache.put(oid, r);
        conn.db.requestCache.put(o, r);
        // Fill object with stored values
        restoreFields(conn, o, rs, i);

/*        if (clazz==Array.class) {
            o = ((Array)o).dump(db);
            db.processedIds.put(oid, new WeakReference(o));
        } else*/
        initObject(conn, o);
        // Save object in cache 
        conn.db.requestCache.put(o, r);
        if (conn.db.logLevel<Database.LOG_VERBOSE)
            return o;
        // Log objet restore
        StringBuffer log = new StringBuffer("[");
        log.append(conn.user.login);
        log.append("] <-R-< ");
        log.append(this.className);
        log.append(" #");
        log.append(r.id);
        if (conn.db.logLevel>Database.LOG_VERBOSE) {
            log.append("   {");
            log.append(o);
            log.append("}");
        }
        conn.db.out.println(log.toString());
        return o;
    }

    
    private void restoreFields (Connection conn, Object o, ResultSet row, int col)
    throws OSQLException {
        try {
            if (columns!=null)
                col = columns.restore(conn, o, row, col);
            if (superClassTable==null || superClassTable.clazz==Object.class)
                return;
            superClassTable.restoreFields(conn, o, row, col);
        } catch (SQLException e) {
            throw new OSQLException("Could not set object data.\nClass: "+className, e);
        }
    }
    

    private void initObject(Connection conn, Object o)
    throws OSQLException {
        if (superClassTable!=null)
            superClassTable.initObject(conn, o);
        if (initializer!=null)
            try {
                initializer.invoke(o, conn);
            } catch (IllegalAccessException e) {
                // Should never happen! Every elements are set to be accessible.
                throw new OSQLException("/!\\ ERROR:  Could not access object intializer for "+className+" !", e);
            } catch (InvocationTargetException e) {
                throw new OSQLException("Constructor threw an exception.\nClass: "+className, e);
            }
    }
    
    
    /**
     * Create database table
     * @param c
     * @throws SQLException
     */
    void createTable (Connection conn)
    throws OSQLException {
    	// Build create table query:
    	StringBuffer q = new StringBuffer("CREATE ");
    	if (this.table.isTextTable!=null) {
    		q.append("TEXT");
    	} else if (this.table.isCached)
    		q.append("CACHED");
    	q.append(" TABLE \""+table.name+"\" (");	// First part 
    	StringBuffer k = new StringBuffer();						// Constraints (keys) part
    	int fieldIdx = 0;
        if (superClassTable==null) {
            // java.lang.Record class shortcut
        	// First part
        	if (table.primaryKey!=null) {
        		if (table.autoIndex) {
        			fieldIdx++;
        			this.pkIdx = fieldIdx;
        			q.append("\".id\" INTEGER IDENTITY");
        		}
	            if (!isFinal) {
	            	if (fieldIdx>0)
	            		q.append(",");
        			fieldIdx++;
	            	q.append(" \".class\" SMALLINT");
	            	k.append(", FOREIGN KEY (\".class\") REFERENCES \""+OSQL_TABLE_NAME+"\" (\"id\") ON DELETE CASCADE");
	            }
	            if (table.hasAccessRights) {
	            	if (fieldIdx>0)
	            		q.append(",");
        			fieldIdx += 3;
		            q.append(" \".uid\" SMALLINT NOT NULL");
		            q.append(", \".gid\" SMALLINT NOT NULL");
		            q.append(", \".access\" TINYINT NOT NULL");
		            k.append(", FOREIGN KEY (\".uid\") REFERENCES \""+User.OSQL_TABLE_NAME+"\" (\"id\")");
		            k.append(", FOREIGN KEY (\".gid\") REFERENCES \".Group\" (\"id\")");
	            }
        	}
        } else {
	        // Any other class
	        fieldIdx++;
			this.pkIdx = fieldIdx;
	        q.append("\".id\" INTEGER NOT NULL PRIMARY KEY");
	        k.append(", CONSTRAINT \"");
	        k.append(className);
	        k.append(" superclass\" FOREIGN KEY (\".id\") REFERENCES \"");
	        k.append(topClassTable.table.name);
	        k.append("\" (\"");
	        k.append(topClassTable.table.primaryKey);
	        k.append("\") ON DELETE CASCADE");
        }

        // Do we have object columns to store?
        if (columns!=null) {
            // Loop through class columns
            FieldColumn col = columns;
            do {
            	if (fieldIdx>0)
            		q.append(", ");
            	fieldIdx++;
            	q.append("\"");
                q.append(col.field.getName());
                q.append("\" ");
                q.append(col.sqlType);
                // Is this field the user-defined primary key?
                if (table.primaryKey!=null && !table.autoIndex && table.primaryKey.equals(col.field.getName())) {
                	q.append(" PRIMARY KEY");
                	this.pkIdx = fieldIdx;
                }
                if (col.getClass()==FieldColumn._Object.class) {
//                	throw new OSQLException("'java.lang.Object' is not a valid field type.");
                	k.append(", CONSTRAINT \"");
                	k.append(className);
                	k.append(" field ");
                	k.append(col.field.getName());
                	k.append("\" FOREIGN KEY (\"");
                	k.append(col.field.getName());
                	k.append("\") REFERENCES \"");
                	ClassTable t = conn.db.getClassTable(col.field.getType());
                	k.append(t.topClassTable.table.name);
                	k.append("\" (\"");
                	k.append(t.topClassTable.table.primaryKey);
                	k.append("\")");
                }
                col = col.next();
//                  if ((!tables.containsKey(t)) && (t!=c))
//                      createClassTable(t);
            } while (col!=null);
        }

        // Add constraints and keys to table definition
        q.append(k);
        q.append(")");
        // Execute SQL query
//System.out.println("Create table:\n"+q.toString());
        try {
            conn.db.statement.executeUpdate(q.toString());
//            conn.db.connection.commit();
        } catch (SQLException e) {
            throw new OSQLException("Could not create database TABLE '"+table.name+"' for class "+className+"\nQuery:  "+q, e);
        }
        if (this.table.isTextTable!=null)
        	            try {
        	StringBuffer st = new StringBuffer("SET TABLE \"");
        	st.append(table.name);
        	st.append("\" SOURCE \"");
        	st.append(this.table.isTextTable);
        	st.append("\"");
            conn.db.statement.executeUpdate(st.toString());
//            conn.db.statement.executeUpdate("CHECKPOINT");
//            conn.db.connection.commit();
        } catch (SQLException e) {
            throw new OSQLException("Could not create database TABLE '"+table.name+"' for class "+className+"\nQuery:  "+q, e);
        }
        // Add class table to pool
//        a.tables.put(clazz, this);
        // Create VIEW table for this class
//        createView(conn);
    }
    
    
    private void createView ()
    throws OSQLException {
        // Create VIEW query
    	String top = topClassTable.table.name;
//        StringBuffer q = new StringBuffer("\nSELECT\n");
        StringBuffer q = new StringBuffer("\n");
        int fieldIdx = 0;
        if (table.primaryKey!=null) {
        	if (table.autoIndex) {
	        	fieldIdx++;
	        	q.append("\t\"");
		        q.append(table.name);
		        q.append("\".\".id\" AS \".id\"");
        	}
	        if (!isFinal) {
	        	if (fieldIdx>0)
	        		q.append(",\n");
	        	fieldIdx++;
	        	q.append("\t\"");
	        	q.append(OSQL_TABLE_NAME);
	        	q.append("\".\"class\" AS \".class\"");
	        }
	        if (table.hasAccessRights) {
	        	if (fieldIdx>0)
	        		q.append(",\n");
	        	fieldIdx += 3;
	        	q.append("\t\"");
		        q.append(top);
		        q.append("\".\".uid\" AS \".user\",\n\t\"");
		        q.append(top);
		        q.append("\".\".gid\" AS \".group\",\n\t\"");
		        q.append(top);
		        q.append("\".\".access\" AS \".access\"");
	        }
        }
        StringBuffer join = new StringBuffer("\nFROM \"");     // FROM part of the query
        StringBuffer on = new StringBuffer("\" ON \"");
        on.append(table.name);
        on.append("\".\"");
        on.append(table.primaryKey);
        on.append("\"=\"");
        // Fill SELECT and FROM parts with proper field names
        createView(q, new Vector(), "", "\t", join, on.toString(), fieldIdx);
        // Append FROM part and finish it
        viewFields = q.toString();
        viewJoin = join.toString();
//        if (table.autoIndex) {
        if (table.primaryKey!=null) {
	        q.setLength(0);
/*	        q.append("\nJOIN \"");
	        q.append(top);
	        q.append("\" ON \"");
	        q.append(table.name);
	        q.append("\".\".id\"=\"");
	        q.append(top);
	        q.append("\".\".id\"");*/
	        if (table.hasAccessRights) {
		        q.append("\n\tAND \"");
		        q.append(top);
		        q.append("\".\".access\"!=0");
		        q.append(" AND ( BITAND(\"");
		        q.append(top);
		        q.append("\".\".access\", 16)!=0 OR (BITAND(\"");
		        q.append(top);
		        q.append("\".\".access\", 1)!=0 AND \"");
		        q.append(top);
		        q.append("\".\".uid\"=");
		        viewUser = q.toString();
		        q.setLength(0);
		        q.append(") OR (BITAND(\"");
		        q.append(top);
		        q.append("\".\".access\", 4)!=0 AND \"");
		        q.append(top);
		        q.append("\".\".gid\"=");
		        viewGroup = q.toString();
		        q.setLength(0);
		        q.append(") )");
	        }
	        if (!isFinal) {
		        q.append("\n\tJOIN \"");
		        q.append(OSQL_TABLE_NAME);
		        q.append("\" ON \"");
		        q.append(top);
		        q.append("\".\".class\"=\"");
		        q.append(OSQL_TABLE_NAME);
		        q.append("\".\"id\"");
	        }
	        viewEnd = q.toString();
        }
//System.out.println("View query:    "+q.toString());
    }
    
    
    private void createView (StringBuffer fields,
    						 Vector existing,
    						 String level,
    						 String tab,
    						 StringBuffer join,
    						 String origin,
    						 int fieldIdx) {
        String fieldName;
//        String tName = user.getViewName(this);
        if (columns!=null) {
        	String className = tab+"\""+table.name+"\".\"";
            FieldColumn col = columns;
            do {
                fieldName = col.field.getName();
            	if (fieldIdx>0)
            		fields.append(",\n");
            	fieldIdx++;
                fields.append(className);
                fields.append(fieldName);
                fields.append("\" AS \"");
                if (existing.contains(fieldName))
                    fields.append(level);
                else
                    existing.add(fieldName);
                fields.append(fieldName);
                fields.append("\"");
                col = col.next();
            } while (col!=null);
        }
        if (level.equals("")) {
            join.append(table.name);
            join.append("\"");
        } else {
        	join.append("\n");
        	join.append(tab);
            join.append("JOIN \"");
            join.append(table.name);
            join.append(origin);
            join.append(table.name);
            join.append("\".\"");
            join.append(table.primaryKey);
            join.append("\"");
        }
        if (superClassTable==null)
            return;
        superClassTable.createView(fields, existing, level+"super.", tab+"\t", join, origin, fieldIdx);
    }
    
    
    void appendView (User user,
    				 StringBuffer q,
    				 StringBuffer fields,
    				 StringBuffer join) {
    	q.append(viewFields);
    	if (fields!=null)
    		q.append(fields);
        q.append(viewJoin);
    	if (join!=null)
    		q.append(join);
    	if (table.hasAccessRights) {
	        q.append(viewUser);
	        q.append(user.id);
	        q.append(viewGroup);
	        q.append(user.group);
    	}
    	if (table.primaryKey!=null)
    		q.append(viewEnd);
    }
    
    
    public ClassTable getFieldTable(Database db, String field)
    throws OSQLException {
    	if (columns==null)
    		return null;
    	FieldColumn col = columns;
    	Class c = null;
    	do {
    		if (field.equals(col.field.getName())) {
    			c = col.field.getType();
    			break;
    		}
    	} while ((col=col.next())!=null);
//    	if (c==null)
//    		return null;
    	return db.getClassTable(c);
    }

    
/*    ObjectRecord getRecord(String name, Integer id) {
    	if (!className.equals(name))
    		return conn.db.getTable(name).getRecord(id);
    	return getRecord(id);
    }*/
    	
    
    Integer getPrimaryKey(Object o)
    throws OSQLException {
    	if (topClassTable!=this)
    		return topClassTable.getPrimaryKey(o);
    	if (table.autoIndex) {
    		// There is no object field we can get the PK value from
    		return null;
    	}
    	try {
        	Field pk = clazz.getDeclaredField(table.primaryKey);
        	pk.setAccessible(true);
        	return new Integer(pk.getInt(o));
    	} catch (NoSuchFieldException e) {
    		throw new OSQLException("No such primary key field", e);
    	} catch (IllegalAccessException e) {
    		throw new OSQLException("Can't access primary key field", e);
    	}
    }
    
    
    public String toString() {
        StringBuffer sb = new StringBuffer("Class: ");
        sb.append(className);
        sb.append("  Table: ");
        sb.append(table.name);
        return sb.toString();
    }
 
    
    
    /**
     * A pool of SQL prepared statements designed to speed up request processing
     * time under heavy load without hurting memory footprint.
     */
    static private class StatementPool {


    	/**
    	 * SQL <code>Connection</code> to the database. 
    	 */
    	private final java.sql.Connection conn;
    	/**
    	 * <code>PreparedStatement</code> definition.
    	 */
    	private final String statement;
    	/**
    	 * Stack caching available prepared statements.
    	 * Each <code>PreparedStatement</code> is wrapped into a
    	 * <code>SoftReference</code> to allow garbage-collection of unused
    	 * statements.
    	 */
    	private final Stack<SoftReference<PreparedStatement>> cache;
    	
    	
    	StatementPool(java.sql.Connection conn, String statement) {
    		this.conn = conn;
    		this.statement = statement;
    		this.cache = new Stack<SoftReference<PreparedStatement>>();
    	}

    	
    	PreparedStatement get()
    	throws OSQLException {
    		SoftReference<PreparedStatement> ref;
    		PreparedStatement ps;
    		// Attempt to find a free PS
    		while (!cache.isEmpty()) {
    			ref = cache.pop();
    			ps = ref.get();
    			if (ps!=null)
    				return ps;
    		}
    		// No PS available, create a new one
    		try {
    			return conn.prepareStatement(statement);
    		} catch (SQLException e) {
                throw new OSQLException("Could not create SQL statement:\n"+statement, e);
    		}
    	}
    	
    	
    	void release(PreparedStatement ps) {
    		cache.push(new SoftReference<PreparedStatement>(ps));
    	}
    	

        protected void finalize ()
    	throws Throwable {
        	SoftReference<PreparedStatement> ref;
        	PreparedStatement ps;
        	while (!cache.isEmpty()) {
    			ref = cache.pop();
//    			if (ref==null)
//    				continue;
    			ps = ref.get();
    			if (ps==null)
    				continue;
    			// Attempt to close SQL statement
		    	try {
	                ps.close();
		    	} catch (SQLException e) {}
        	}
    	}
    	
    }

    
    
    public static final class TableProperties {
    	
    	
        public static final String TABLE_NAME = "name";
        public static final String PRIMARY_KEY = "primaryKey";
        public static final String AUTO_INDEX = "autoIndex";
        public static final String ACCESS_RIGHTS = "accessRights";
        public static final String TEXT_TABLE = "textTable";
        public static final String CACHED_TABLE = "cached";
        
        
        public final String  name;
        public final String  primaryKey;
        final boolean autoIndex;
        private final boolean hasAccessRights;
        private final boolean isCached;
        private final String  isTextTable;
    	
        
        private TableProperties(Class c)//, TableNameMapper mapper)
        throws OSQLException {
    		/*
    		 * Default values
    		 */
        	String  name = c.getName();
        	String  primaryKey = null;
            boolean autoIndex = true;
            boolean hasAccessRights = false;
            boolean isCached = false;
            String  isTextTable = null;
    		/*
    		 * Retrieve table properties, if any
    		 * (silent exit on exception if none) 
    		 */
        	try {
        		Field f = c.getDeclaredField("OSQL_TABLE");
    	    	f.setAccessible(true);
    	    	// Table properties must be static and final
    	    	if (!Modifier.isStatic(f.getModifiers()) || !Modifier.isFinal(f.getModifiers()))
		    		throw new OSQLException("Error: table properties must be static and final (class "+c.getName()+")");
    	    	String[] tmp = (String[])f.get(c);
        		/*
        		 * Parse properties
        		 */
    		    StringTokenizer prop;
    		    String key, value;
    		    for (int i=0, n=tmp.length; i<n; i++) {
    		    	prop = new StringTokenizer(tmp[i], "=");
    		    	// Check property syntax
    		    	if (prop.countTokens()!=2)
    		    		throw new OSQLException("Syntax error in class "+c.getName()
    		    			+", table property n°"+i+": '"+tmp[i]+"'");
    		    	key = prop.nextToken();
    		    	if (key.equals(""))
    		    		throw new OSQLException("Syntax error in class "+c.getName()
    		    			+", table property n°"+i+", empty key: '"+tmp[i]+"'");
    		    	value = prop.nextToken();
    		    	if (value.equals(""))
    		    		throw new OSQLException("Syntax error in class "+c.getName()
    		    			+", table property n°"+i+", empty value: '"+tmp[i]+"'");
    		    	// Assign supported property
    		    	if (key.equals(TABLE_NAME)) {
    		    		name = value;
//    		    		mapper.setName(c, name);
    		    	} else if (key.equals(PRIMARY_KEY)) {
   		    			primaryKey = value;
    		    	} else if (key.equals(AUTO_INDEX)) {
    		    		if (value.equals("false"))
    		    			autoIndex = false;
    		    	} else if (key.equals(ACCESS_RIGHTS)) {
    		    		if (value.equals("true"))
    		    			hasAccessRights = true;
    		    	} else if (key.equals(TEXT_TABLE)) { 
    		    		isTextTable = value;
    		    	} else if (key.equals(CACHED_TABLE)) {
    		    		if (value.equals("true"))
    		    			isCached = true;
    		    	}
    		    }
    	    } catch (NoSuchFieldException e) {
    	    } catch (IllegalAccessException e) {
    	    	throw new OSQLException("Could not access OSQL table properties for class "+c.getName(), e);
    	    }
    	    /*
    	     * Process table properties
    	     */
/*    	        if (!isFinal || hasAccessRights)
        	this.hasIndex = true;
        else {
            boolean index = true;
            try {
            	Field f = c.getDeclaredField("OSQL_HAS_INDEX");
            	f.setAccessible(true);
            	if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))
            		index = f.getBoolean(c);
            } catch (Exception e) {}
        	this.hasIndex = index;
        }*/
        	this.name = name;
        	if (primaryKey!=null) {
            	this.primaryKey = primaryKey;
        		this.autoIndex = false;
        	} else {
        		if (autoIndex)
        			primaryKey = ".id";
            	this.primaryKey = primaryKey;
        		this.autoIndex = autoIndex;
        	}
            this.hasAccessRights = hasAccessRights;
            this.isCached = isCached;
            this.isTextTable = isTextTable;
        }

        
    }

    
    
    /**
     * A cache with weak references designed to hold objects without preventing
     * them from being garbage-collected.
     * <p>
     * Objects will remain in cache as long as user applications use them
     * (<i>i.e.</i> maintain strong references to them). Then, they will be
     * released at the discretion of the garbage collector, insuring however
     * memory will never run out because of cached dead objects.
     * <p>
     * The goal of this cache is to speed up user requests as cached objects
     * will not have to be reconstructed from database data. 
     */
    final class ObjectCache {
    	
    	
        /**
         * A map with weak identity-keys mapping objects with their records.
         * Each <code>ObjectRecord</code> is wrapped into a
         * <code>WeakReference</code> to prevent circular strong referencement.
         */
        private final WeakHashMap<Object,ObjectRecord> objects;
        
        /**
         * A map with value-keys mapping object IDs with their records.
         * Each <code>ObjectRecord</code> is wrapped into a
         * <code>WeakReference</code> to allow garbage collection of cached
         * objects.
         */
        private final WeakHashMap<Integer,WeakReference<ObjectRecord>> ids;    
    	
 
        private ObjectCache () {
            this.objects = new WeakHashMap<Object,ObjectRecord>();
            this.ids = new WeakHashMap<Integer,WeakReference<ObjectRecord>>();
        }

        
        // only called by Database
        ObjectRecord get(Object o)
        throws OSQLException {
        	ObjectRecord r = objects.get(o);
            // Auto PK indexing: if we know this object it *must* be in our cache
        	// No indexing: we *may* have it in our cache. If not, we have no way to find it
            if (topClassTable.table.autoIndex || topClassTable.table.primaryKey==null)
            	return r;
            // Class has a user-specified primary key:
            // We still *might* have the object in cache as-is
        	if (r!=null)
        		return r;
        	// But user may have built another object with same PK
        	Integer oid = getPrimaryKey(o);
        	r = getById(oid);
        	// We got it?
        	if (r!=null)
        		return r;
        	// We don't have any such reference in cache, still object may exist in DB
//        	System.out.println("Object #"+oid+" not in cache :(");
        	return null;
        }

        
        ObjectRecord getById(Integer id) {
        	WeakReference<ObjectRecord> ref = ids.get(id);
            // Not in cache?
            if (ref==null)
                return null;
            ObjectRecord r = ref.get();
            // In cache but reference cleared by GC? Update our cache
            if (r==null)
            	ids.remove(id);
            return r;
        }
        
        
        void put (ObjectRecord r) {
        	if (r==null || r.id==null)// || r.object==null)
        		return;
        	Object o = r.object();
        	if (o==null)
        		return;
            objects.put(o, r);
            ids.put(r.id, new WeakReference<ObjectRecord>(r));
        }
        
        
        void remove (ObjectRecord r) {
            objects.remove(r.object());
            ids.remove(r.id);
        }
        
        
        String size() {
        	StringBuffer sb = new StringBuffer();
        	sb.append(objects.size());
        	sb.append("/");
        	sb.append(ids.size());
        	return sb.toString();
        }
    }
    
    
    
}