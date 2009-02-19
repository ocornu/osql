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


import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.WeakHashMap;

import org.osql.parser.ParseException;
import org.osql.parser.Parser;
import org.osql.parser.StringReader;


/**
 * @author Olivier Cornu <o.cornu@gmail.com>
 */
public final class Database {

	
    // SQL database backend
//  public static final String DB       = "jdbc:hsqldb:.";
    /**
     * The default database backend to use if user does not furnish his own.
     */
    public static final String DEFAULT_DB       = "jdbc:hsqldb:file:/home/zit/private/projects/osql";
    /**
     * The default JDBC driver to use if user does not fursnish is own.
     */
    public static final String DEFAULT_DRIVER   = "org.hsqldb.jdbcDriver";
    
    private final String url;
	final java.sql.Connection connection;	        // Connection to database
	final Statement statement;		// Request statement
    
    
    // Admin user connection
    AdminConnection sysConn;

    // Cache for classes
	private final WeakHashMap tables;	         // Existing class --> Table

    // Request parsing
    private final StringBuffer parseBuffer = new StringBuffer();
    private final StringReader reader	= new StringReader("");
    private final Parser parser			= new Parser(reader);
    
    
    // Current objects processed
    final RequestCache requestCache;
/*    final IdentityHashMap   processedObjects;
    final HashMap           processedIds;
*/
    
    // Logging
    /**
     * Log level: no logging will occur.
     */
    static public final byte LOG_NONE       = 0;
    /**
     * Log level: relatively quiet logging.
     */
    static public final byte LOG_NORMAL     = 1;
    /**
     * Log level: relatively verbose logging.
     */
    static public final byte LOG_VERBOSE    = 2;
    /**
     * Log level: all operations will be logged.
     */
    static public final byte LOG_DEBUG      = 3;
    public final byte logLevel;
    PrintStream out = System.out;
    
    // Stats
    final long startTime;
    
    
    public Database ()
    throws OSQLException {
        this(DEFAULT_DB, "sa", "");
    }
    
    public Database (String url, String login, String pwd)
    throws OSQLException {
        this(DEFAULT_DRIVER, url, login, pwd, new TableNameMapper(), LOG_NORMAL);
    }
    
    public Database (String url,
    				 String login,
    				 String pwd,
    				 TableNameMapper wrapper)
    throws OSQLException {
        this(DEFAULT_DRIVER, url, login, pwd, wrapper, LOG_NORMAL);
    }
    
    public Database (String url,
    				 String login,
    				 String pwd,
    				 byte logLevel)
    throws OSQLException {
        this(DEFAULT_DRIVER, url, login, pwd, new TableNameMapper(), logLevel);
    }
    
    public Database (String url,
    				 String login,
    				 String pwd,
    				 TableNameMapper wrapper,
    				 byte logLevel)
    throws OSQLException {
    	this(DEFAULT_DRIVER, url, login, pwd, wrapper, logLevel);
    }

	private Database (String driver,
					  String url,
					  String login,
					  String pwd,
					  TableNameMapper wrapper,
					  byte logLevel)
	throws OSQLException {
        this.startTime = System.currentTimeMillis();
        this.logLevel = logLevel;
        if (this.logLevel>LOG_NONE) {
            StringBuffer log = new StringBuffer("*** Opening OSQL database for ");
            log.append(login);
            log.append("@");
            log.append(url);
            log.append(" ...");
            out.println(log.toString());
        }

        this.url = url;
        
        // Load SQL driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			throw new OSQLException("Class not found: "+driver+"\nFailed to load SQL driver.", e);
		}
        // Init database connection
		try {
			this.connection = DriverManager.getConnection (url, login, pwd);
			this.statement = connection.createStatement();
            // Set autocommit to off, so we can rollback in case of error
            this.connection.setAutoCommit(false);
            this.connection.commit();
		} catch (SQLException e) {
            quit();
			throw new OSQLException("Failed to connect to SQL database.", e);
		}
        
		// Init class tables map and objects map
        this.tables = new WeakHashMap();
        // Init processed objects map (to avoid infinite loops with recursive referencing)
        this.requestCache = new RequestCache();
        
        // Check if OSQL system tables already exist in database
        try {
        	ResultSet rs = statement.executeQuery("SELECT top 1 * from \""+User.OSQL_TABLE_NAME+"\"");
            rs.close();
            rs = statement.executeQuery("SELECT top 1 * from \""+ClassTable.OSQL_TABLE_NAME+"\"");
            rs.close();
        } catch (SQLException e) {
            // New OSQL database
            try {
                if (this.logLevel>LOG_NONE)
                    out.println("*** Creating system tables...");
                // Create References table
                try {
                    statement.executeUpdate("CREATE TABLE \".Group\" ("+
                        " \"id\" SMALLINT NOT NULL"+
                        ", \"name\" VARCHAR NOT NULL"+
                        ", PRIMARY KEY (\"id\")"+
                        ", UNIQUE (\"name\")"+
                        " )");
                    statement.executeUpdate("CREATE TABLE \""+User.OSQL_TABLE_NAME+"\" ("+
                        " \"id\" SMALLINT NOT NULL"+
                        ", \"login\" VARCHAR NOT NULL"+
                        ", \"password\" VARCHAR NOT NULL"+
                        ", \"group\" SMALLINT"+
                        ", \"groups\" VARCHAR"+
                        ", PRIMARY KEY (\"id\")"+
                        ", UNIQUE (\"login\")"+
                        ", FOREIGN KEY (\"group\") REFERENCES \".Group\" (\"id\")" +
                        " )");
                    statement.executeUpdate("CREATE TABLE \""+ClassTable.OSQL_TABLE_NAME+"\" ("+
                        " \"id\" SMALLINT NOT NULL"+
                        ", \"class\" VARCHAR"+
                        ", \"superclass\" SMALLINT"+
                        ", PRIMARY KEY (\"id\")"+
                        ", UNIQUE (\"class\")"+
                        " )");
                } catch (SQLException ee) {
                    throw new OSQLException("Failed to create system tables.", ee);
                }
                this.sysConn = new AdminConnection(this, new User(this, "root", pwd, newGroup("root"), "root"));
                newGroup("users");
                if (this.logLevel>LOG_NONE) {
                    StringBuffer log = new StringBuffer("*** Database ready (");
                    uptime(log);
                    log.append(").");
                    out.println(log.toString());
                }
                return;
            } catch (OSQLException oe) {
                quit();
                throw oe;
            }
        }

        // Existing OSQL database
        if (this.logLevel>LOG_NORMAL)
            out.println("*** Loading system tables...");
        // Restore admin user
        sysConn = new AdminConnection(this, new User(this, "root", pwd));
        
        if (this.logLevel>LOG_NONE) {
            StringBuffer log = new StringBuffer("*** Database ready (");
            uptime(log);
            log.append(").");
            out.println(log.toString());
        }
	}
	

    /**
     * Stores the given <code>Object</code> with specified <code>access</code>
     * rights using this <code>Connection</code>.
     * <p>
     * Operation is:
     * <ul>
     * <li><i>universal</i>: all object properties are saved regardless of their
     * access modifiers (<i>i.e.</i> <code>private</code> properties are saved
     * too). Only <code>transient</code> properties are disregarded. This
     * guaranties all object properties are stored.</li>
     * <li><i>recursive</i>: any other object contained in this
     * <code>Object</code> is stored as well. This garanties full retrieval of
     * stored objects.</li>
     * <li><i>atomic</i>: operation either succeeds or fails altogether (all
     * ongoing changes are rolled back if an error occurs). This garanties
     * stored data integrity.</li>
     * <li><i>thread safe</i>: this guaranties several concurrent threads can
     * access the OSQL database seamlessly.
     * <li><i>state conscious</i>: operation results in SQL <code>INSERT</code>
     * queries for new objects and SQL <code>UPDATE</code> queries for existing
     * ones.</li>
     * </ul>
     * <p>
     * This method is only called by a {@link Connection} in order to serve a
     * client request. All store requests from clients MUST use this method only.
     * 
     * @param conn		the <code>Connection</code> this operation is done on
     * 					behalf of
     * @param o			the <code>Object</code> to store
     * @param access	the UNIX-like access rights according to which further
     * 					operations on this <code>Object</code> will be enforced
     * @throws OSQLAccessViolation	thrown if this <code>Object</code> cannot be
     * 								stored dued to UNIX-like access rights
     * 								enforcement  
     * @throws OSQLException		thrown if any other error occurs during the 
     * 								storing process
     */
    synchronized void store (Connection conn, Object o, byte access)
    throws OSQLAccessViolation, OSQLException {
    	// Check there indeed is an object to store
    	if (o==null)
            return;
    	/*
    	 * Store object
    	 */
        try {
            store(conn, o, null, access);
            // Commit changes on success
            connection.commit();
            // Cache processed objects
            requestCache.cacheAll();
        } catch (OSQLException e) {
            try {
            	// Rollback changes on error
                connection.rollback();
            } catch (SQLException se) {
                throw new OSQLException("Could not rollback changes after error.", e);
            }
            throw e;
        } catch (SQLException e) {
            throw new OSQLException("Could not commit changes to database.", e);
        } finally {
        	// Whatever happens, clear request cache before exiting
        	requestCache.clear();
        }
    }

    
/*    // Mauvaise façon d'interdire la sauvegarde d'un String.
    // Généraliser pour tous les Integer, Float...
    synchronized void store (Connection conn, String s, byte access)
    throws OSQLException {
    		throw new OSQLException("Can only store String when it is another object's field.");
    }
*/    
    
    /**
     * Stores the given <code>Object</code> with specified <code>access</code>
     * rights as the child of this <code>parent</code> object, using this
     * <code>Connection</code>.
     * 
     * @param conn		the <code>Connection</code> this operation is done on
     * 					behalf of
     * @param o			the <code>Object</code> to store
     * @param parent	the reference to the <code>parent</code> object this
     * 					<code>Object</code> is a child of
     * @param access	the UNIX-like access rights according to which further
     * 					operations on this <code>Object</code> will be enforced
     * @return			the <code>ObjectRecord</code> holding the reference to
     * 					this <code>Object</code>
     * @throws OSQLAccessViolation	thrown if this <code>Object</code> cannot be
     * 								stored dued to UNIX-like access rights
     * 								enforcement  
     * @throws OSQLException		thrown if any error other occurs during the 
     * 								storing process
     * @see	store(Connection conn, Object o, byte access)
     */
    ObjectRecord store (Connection conn, Object o, ObjectRecord parent, byte access)
    throws OSQLAccessViolation, OSQLException  {
        ObjectRecord r = requestCache.get(o);
        // Avoid infinite loops with self-referencing objects
        if (r!=null)
            return r;
        
        Class c = o.getClass();
        // Process arrays in a special way
//        if (c.isArray())
//            c = Array.class;
        
        ClassTable ct = getClassTable(c);
        r = ct.cache.get(o);
        // Do we know this object yet?
        if (r!=null) {
            // Yes, let's update its values
            try {
                /*
                 * UPDATE object in database
                 */
            	requestCache.put(o, r);		// protect object from GC
//            	r.lock = o;
//            System.out.println("Lock: "+o);
                ct.updateRecord(conn, r, access, parent);
                if (logLevel<LOG_VERBOSE)
                    return r;
                // Log object update
                StringBuffer log = new StringBuffer("[");
                log.append(conn.user.login);
                log.append("] >-U-> ");
                log.append(ct.className);
                log.append(" #");
                log.append(r.id);
                if (logLevel>LOG_VERBOSE) {
                    log.append("  {");
                    log.append(o);
                    log.append("}");
                }
                out.println(log.toString());
            } catch (OSQLAccessViolation e) {
                if (parent==null)
                    throw e;
            }
            return r;
        }
        /*
         * INSERT object in database and get back its ID.
         * 
         * Can raise an exception if class has a user-specified primary key,
         * provided that:
         *   - such primary key exists in DB 
         *   - we don't have any reference to it in cache
         *   - user creates another object with same PK
         * In which case, we'll catch the exception and make request an UPDATE.
         */ 
        try {
	        r = ct.insertRecord(conn, o, access, parent, ct);
	        if (logLevel<LOG_VERBOSE)
	            return r;
	        // Log object insert
	        StringBuffer log = new StringBuffer("[");
	        log.append(conn.user.login);
	        log.append("] >-I-> ");
	        log.append(ct.className);
	        log.append(" #");
	        log.append(r.id);
	        if (logLevel>LOG_VERBOSE) {
	            log.append("  {");
	            log.append(o);
	            log.append("}");
	        }
	        out.println(log.toString());
	        return r;
        } catch (OSQLException e) {
        	// Did it fail because of the above mentioned reasons?
        	if (e.getCause().getMessage().indexOf("Violation of unique constraint")==0) {
//        		e.getCause().printStackTrace();
        		// Add reference to cache and retry
        		Integer oid = ct.getPrimaryKey(o);
        		if (oid==null)
        			throw e;
        		r = new ObjectRecord(ct, conn.user.id, conn.user.group, oid, o, access);
        		ct.cache.put(r);
        		return store (conn, o, parent, access);
        	} else
        		throw e;
        }
    }
    

    /**
     * @param conn
     * @param o
     * @return
     * @throws OSQLException
     */
    synchronized boolean remove (Connection conn, Object o)
    throws OSQLException {
        if (o==null)
            return false;
        Class c = o.getClass();
        ClassTable ct = getExistingClassTable(c);
        if (ct==null)
            return false;
        ObjectRecord r = ct.cache.get(o); 
        if (r==null)
            return false;
        delete(conn, ct, r.id);
        ct.cache.remove(r);
        return true;
    }

    
    /**
     * Removes all stored objects of this <code>Class</code>.
     * <p>
     * TODO: should also drop the table and remove all reference to it in
     * system tables when possible (e.g. there is no subclass with object stored).
     * 
     * @param conn
     * @param c
     * @return
     * @throws OSQLException
     */
    synchronized boolean removeAll (Connection conn, Class c)
    throws OSQLException {
        // Null class?
        if (c==null)
            return false;
        
        // Unknow class?
        ClassTable ct = getExistingClassTable(c);
        if (ct==null)
            return false;
        
        StringBuffer q = new StringBuffer("DELETE FROM \"");
//        q.append(ObjectRecord.OSQL_TABLE_NAME);
        q.append("\" WHERE \"class\"=");
        q.append(ct.id);
        q.append("");
        try {
            // Browse id list and DELETE corresponding records
            statement.executeUpdate(q.toString());
            return true;
        } catch (SQLException e) {
            throw new OSQLException("Could not remove objects.\nQuery:  "+q.toString(), e);
        }
    }
    
    
    /**
     * Removes all stored objects of this <code>Class</code> that fulfill
     * this <code>where</code> clause.
     * <p> 
     * Operation is:
     * <ul>
     * <li><i>atomic</i>: operation either succeeds or fails altogether (all
     * ongoing changes are rolled back if an error occurs). This garanties
     * stored data integrity.</li>
     * <li><i>thread safe</i>: this guaranties several concurrent threads can
     * access the OSQL database seamlessly.
     * </ul>
     * <p>
     * This method is only called by a {@link Connection} in order to serve a
     * client request.
     * @param conn		the <code>Connection</code> objects are removed on behalf of
     * @param c			the <code>Class</code> of removed objects
     * @param where		the clause removed objects must fulfill
     * @return			the number of objects removed
     * @throws OSQLSyntaxError	thrown if <code>where</code> clause is
     * 							syntaxically incorrect
     * @throws OSQLException	thrown if any other error occurs during the
     * 							removal process
     */
    synchronized int removeAll (Connection conn, Class c, String where)
    throws OSQLSyntaxError, OSQLException {
        // Null class?
        if (c==null)
            return 0;
        ClassTable ct = getExistingClassTable(c);
        // Unknow class? Nothing to remove
        if (ct==null)
            return 0;

        /*
         * Build query to retrieve objects selected for deletion
         */
        StringBuffer q = new StringBuffer("SELECT");
        if ((where!=null) && (!where.equals(""))) {
        	StringBuffer f = new StringBuffer();
        	StringBuffer j = new StringBuffer();
        	where = parse(ct, where, f, j);
            ct.appendView(conn.user, q, f, j);
            q.append("\nWHERE ");
            q.append(where);
        } else
        	ct.appendView(conn.user, q, null, null);
        
//        System.out.println(q);
        try {
            /*
             * Execute SELECT query and store PK values
             */
            ResultSet rs = statement.executeQuery(q.toString());
            Vector v = new Vector();
            int i = 0;
            int d = 0;
            Integer oid;
            while (rs.next()) {
                oid = new Integer(rs.getInt(ct.table.primaryKey));
                if (delete(conn, ct, oid))
                	d++;
                v.add(oid);
                i++;
            }
            if (logLevel==LOG_VERBOSE)
            	System.out.println(d+"/"+i+" record(s) deleted");
            rs.close();
            /*
             * Remove objects from cache
             */
            ObjectRecord r;
            int n = i;
            for (i=0; i<n; i++) {
                oid = (Integer)v.get(i);
                r = ct.cache.getById(oid);
                if (r==null)
                    continue;
                ct.cache.remove(r);
            }
            // Commit changes to database and exit
            connection.commit();
            return n;
        } catch (SQLException e) {
            throw new OSQLException("Could not remove objects.\nSELECTion query:  "+q.toString(), e);
        }
    }


    /**
     * Delete the object of primary key value <code>oid</code> from database
     * using this <code>Connection</code>.
     * <p>
     * TODO:	enforcement of UNIX-like access rights.
     * 
     * @param conn	the <code>Connection</code> we delete this object on behalf of
     * @param ct	the <code>ClassTable</code> responsible for this object class
     * @param oid	the primary key value of this object
     * @return		<code>true</code> if object has been deleted,
     * 				<code>false</code> otherwise (should never happen: in case of
     * 				error an exception is raised
     * @throws OSQLException	thrown if an SQL error occurs during the deletion
     */
    private boolean delete (Connection conn, ClassTable ct, Integer oid)
    throws OSQLException {
    	ClassTable tc = ct.topClassTable;
        StringBuffer q = new StringBuffer("DELETE FROM \"");
        q.append(tc.table.name);
        q.append("\" WHERE \"");
        q.append(tc.table.primaryKey);
        q.append("\"=");
        q.append(oid);
        try {
            return statement.executeUpdate(q.toString())==1;
        } catch (SQLException e) {
            throw new OSQLException("Could not delete object #"+oid+" ("+ct.className+").", e);
        }
    }
    
    
    synchronized Object get (Connection conn, Class c, String where, String order)
    throws OSQLException {
//    	long ts = System.currentTimeMillis();
        if ((c==null))
            throw new OSQLException("Can't search for a null-class object.");       // Don't retrieve a null class object
        try {
        	ClassTable t = getClassTable(c);
        	StringBuffer f = null;
        	StringBuffer j = null;
        	if ((where!=null) && (!where.equals(""))) {
        		f = new StringBuffer();
        		j = new StringBuffer();
        		where = parse(t, where, f, j);
        	}
        	if (!t.table.autoIndex && (order!=null && order.matches(".*\"\\.id\".*")))
        		order = null;
        	// Retrieve specified object (if any)
            Object o = select(conn, t, where, order, f, j);
            // Store processed objects in cache
            requestCache.cacheAll();
//            ids.putAll(processedIds);
//            System.out.println("Get: "+(System.currentTimeMillis()-ts)+"ms \t["+c.getName()+" WHERE "+where+" ORDER BY "+order);
            return o;
        } catch (OSQLException e) {
            throw e;
        } finally {
            // Clear our temporary caches
        	requestCache.clear();
        }
    }
    
    
    Object select (Connection conn, String type, int oid)
    throws OSQLException {
        try {
            return select(conn, getClassTable(Class.forName(type)), "\".id\"="+oid, null, null, null);
        } catch (ClassNotFoundException e) {
            throw new OSQLException("Class not found: "+type, e);
        }
    }
    

    Object select (Connection conn, Class c, int oid)
    throws OSQLException {
    	ClassTable ct = getClassTable(c);
    	StringBuffer where = new StringBuffer("\"");
    	where.append(ct.table.primaryKey);
    	where.append("\"=");
    	where.append(oid);
        return select(conn, ct, where.toString(), null, null, null);
    }
    
    
    private Object select (Connection conn, ClassTable t, String where, String order, StringBuffer f, StringBuffer j)
    throws OSQLException {
//        Table table = getTable(c);
        // Build query
//        StringBuffer q = new StringBuffer("SELECT TOP 1 * FROM (");
        StringBuffer q = new StringBuffer("SELECT TOP 1 ");
        t.appendView(conn.user, q, f, j);
//        q.append(")");
        if ((where!=null) && (!where.equals(""))) {
            q.append(" WHERE ");
            q.append(where);
        }
        if ((order!=null) && (!order.equals(""))) {
            q.append(" ORDER BY ");
            q.append(order);
        }
        ResultSet rs;
        try {
            // Execute query
            try {
//            	long ts = System.currentTimeMillis();
                rs = statement.executeQuery(q.toString());
//                System.out.println("Query: "+(System.currentTimeMillis()-ts));
            } catch (SQLException ee) {
//                if(!ee.getMessage().startsWith("Table not found: "))
                    throw ee;
//                ee.printStackTrace();
//                table.createView(conn);
//                rs = statement.executeQuery(q.toString());
            }
            rs.next();
        } catch (SQLException e) {
//            e.printStackTrace();
            throw new OSQLException("Could not create SQL statement:\n"+q.toString(), e);
        }
        
        try {
            return t.restoreObject(conn, rs);
        } catch (OSQLException e) {
            throw e;//new OSQLException("Could not restore object.\nTable:  "+table.tableName+"\nWHERE:  "+where+(order==null ? "": "\nORDER:  "+order), e);
        } finally {
            // Attempt to close ResultSet
            try {
                rs.close();
            } catch (SQLException e) {
                throw new OSQLException("Could not close SQL statement:\n"+rs.toString(), e);
            }
        }
    }


    synchronized List getAll (Connection conn, Class c, String where, String order)
    throws OSQLException {
        if (c==null)
            return null;        // Don't retrieve a null class object
        try {
        	ClassTable t = getClassTable(c);
        	StringBuffer f = null;
        	StringBuffer j = null;
        	if ((where!=null) && (!where.equals(""))) {
        		f = new StringBuffer();
        		j = new StringBuffer();
        		where = parse(t, where, f, j);
        	}
            List l = selectAll(conn, t, where, order, f, j);
            requestCache.cacheAll();
//            ids.putAll(processedIds);
            return l;
        } catch (OSQLException e) {
            throw e;
        } finally {
        	requestCache.clear();
        }
    }
    
    
    private List selectAll (Connection conn, ClassTable t, String where, String order, StringBuffer f, StringBuffer j)
    throws OSQLException {
//        Table table = getTable(c);
        // Build query
        StringBuffer q = new StringBuffer("SELECT");
        t.appendView(conn.user, q, f, j);
//        q.append(")");
        if ((where!=null) && (!where.equals(""))) {
            q.append(" WHERE ");
            q.append(where);
        }
        if ((order!=null) && (!order.equals(""))) {
            q.append(" ORDER BY ");
            q.append(order);
        }
        ResultSet rs;
        try {
            // Execute query
            rs = statement.executeQuery(q.toString());
        } catch (SQLException e) {
            throw new OSQLException("Could not create SQL statement:\n"+q.toString(), e);
        }
        
        try {
            ArrayList list = new ArrayList();
//            Integer oid; String className; Object o;
            while (rs.next())
                list.add(t.restoreObject(conn, rs));
            return list;
        } catch (SQLException e) {
            throw new OSQLException("SQL exception during transaction:\n"+q.toString(), e);
        } catch (OSQLException e) {
            throw new OSQLException("SQL exception during transaction:\n"+q.toString(), e);
        } finally {
            // Attempt to close ResultSet
            try {
                rs.close();
            } catch (SQLException e) {
                throw new OSQLException("Could not close SQL statement:\n"+rs.toString(), e);
            }
        }
    }
    
    
    /**
     * Returns the number of stored objects of this <code>Class</code> that
     * fulfill this <code>where</code> clause.
     * 
     * @param conn
     * @param c
     * @param where
     * @return
     * @throws OSQLException
     */
    synchronized int count (Connection conn, Class c, String where)
    throws OSQLException {
    	if (c==null)
    		return 0;
        ClassTable ct = getExistingClassTable(c);
        // Unknown class? Zero object stored
        if (ct==null)
        	return 0;
        /*
         * Build query
         */ 
        StringBuffer q = new StringBuffer("SELECT COUNT(*) FROM (SELECT");
    	StringBuffer f = null;
    	StringBuffer j = null;
    	boolean w = false;
        if ((where!=null) && (!where.equals(""))) {
    		w = true;
    		f = new StringBuffer();
    		j = new StringBuffer();
    		where = parse(ct, where, f, j);
        }
        ct.appendView(conn.user, q, null, null);
        q.append(")");
        if (w) {
            q.append(" WHERE ");
            q.append(where);
        }
        /*
         * Execute query
         */
        ResultSet rs;
        try {
        	rs = statement.executeQuery(q.toString());
        } catch (SQLException e) {
            throw new OSQLException("Could not create SQL statement:\n"+q.toString(), e);
        }
        try {
        	// Read result and return it
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new OSQLException("Could not get records count.\nQuery:  "+q.toString()+"\nResultSet:  "+rs.toString(), e);
        } finally {
            // Attempt to close ResultSet
            try {
                rs.close();
            } catch (SQLException e) {
                throw new OSQLException("Could not close SQL result set:\n"+rs.toString(), e);
            }
        }
    }

    
    String toString(Object o)
    throws OSQLException {
    	if (o==null)
    		return null;
    	ClassTable ct = getClassTableFromCache(o);
    	if (ct==null)
    		throw new OSQLException("Unknown class: "+o.getClass().getName());
    	ObjectRecord r = ct.cache.get(o);
    	if (r==null)
    		throw new OSQLException("Unknown object: "+o);
    	return r.id.intValue()+"";
    }
    
    
/*
 * =========================================================================
 */    

        
    public Connection connect (String login, String password)
    throws OSQLException {
/*        StringBuffer w = new StringBuffer("\"login\"='");
        w.append(login);
        w.append("' AND \"password\"='");
        w.append(password);
        w.append("'");
        User user = (User)get(sysConn, User.class, w.toString(), null);
        if (user==null)
            throw new OSQLException("Wrong login/password.");*/
        User user = new User(this, login, password);
        if (user.login.equals("root"))
            return sysConn;
        return new Connection(this, user);
    }

    
    void addUser (AdminConnection conn, String login, String password)
    throws OSQLException, OSQLAccessViolation {
    	addUser(conn, login, password, "users");
    }
    
    
    void addUser (AdminConnection conn, String login, String password, String mainGroup)
    throws OSQLException, OSQLAccessViolation {
//        if (!conn.user.admin)
//            throw new OSQLAccessViolation("You must be an administrator to add users.");
        if (mainGroup==null || mainGroup.equals(""))
        	throw new OSQLException("Invalid group name \""+mainGroup+"\".");
        StringBuffer q = new StringBuffer("SELECT \"id\" FROM \".Group\" WHERE \"name\"='");
        q.append(mainGroup);
        q.append("'");
        ResultSet rs;
        try {
            rs = statement.executeQuery(q.toString());
        } catch (SQLException e) {
            throw new OSQLException("System table \".Group\" is probably missing.\nCould not execute SQL query: "+q.toString(), e);
        }
        short id;
        try {
            rs.next();
            id = (short)rs.getShort(1);
        } catch (SQLException e) {
            throw new OSQLAccessViolation("Group does not exists: "+mainGroup, e);
        } finally {
            // Attempt to close ResultSet
            try {
                rs.close();
            } catch (SQLException e) {
                throw new OSQLException("Could not close SQL result set:\n"+rs.toString(), e);
            }
        }
        new User(conn.db, login, password, id, null);
    }
    

    void addGroup (AdminConnection conn, String group)
    throws OSQLException, OSQLAccessViolation {
//        if (!conn.user.admin)
//            throw new OSQLAccessViolation("You must be an administrator to add users.");
        newGroup(group);
    }
    
    private short newGroup(String group)
    throws OSQLException, OSQLAccessViolation {
        if (group==null || group.equals(""))
        	throw new OSQLException("Invalid group name \""+group+"\".");
        StringBuffer q = new StringBuffer("SELECT TOP 1 \"id\" FROM \".Group\" ORDER BY \"id\" DESC");
        ResultSet rs;
        try {
            rs = statement.executeQuery(q.toString());
        } catch (SQLException e) {
            throw new OSQLException("System table \".Group\" is probably missing.\nCould not execute SQL query: "+q.toString(), e);
        }
        short id;
        try {
            rs.next();
            id = (short)(rs.getShort(1)+1);
            if (id<0)
            	throw new OSQLException("Maximum number of groups (32768) reached. Could not create group \""+group+"\".");
        } catch (SQLException e) {
            id = (short)0;
            //throw new OSQLException("Can't retrieve user id.", e);
        } finally {
            // Attempt to close ResultSet
            try {
                rs.close();
            } catch (SQLException e) {
                throw new OSQLException("Could not close SQL result set:\n"+rs.toString(), e);
            }
        }
        // Attempt to create new group with unique name
        q.setLength(0);
        q.append("INSERT INTO \".Group\" VALUES (");
        q.append(id);
        q.append(", '");
        q.append(group);
        q.append("')");
        try {
            statement.executeUpdate(q.toString());
            connection.commit();
        } catch (SQLException e) {
            throw new OSQLAccessViolation("Group already exists: "+group, e);
        }
    	return id;
    }
    
/*
 * =========================================================================
 */    


    private String parse(ClassTable t, String where, StringBuffer f, StringBuffer j)
    throws OSQLSyntaxError, OSQLException {
    	parseBuffer.setLength(0);
    	try {
	    	reader.reset(where);
	    	parser.ReInit(reader);
	    	parser.Where(this, t, parseBuffer, f, j);
	    	return parseBuffer.toString();
    	} catch (IOException e) {
    		throw new OSQLException("Could not parse WHERE clause.", e);
    	} catch (ParseException e) {
    		throw new OSQLSyntaxError("Error parsing following request:\n*** Where clause:\n"+where+
    			"\n.........1....1....2....2....3....3....4....4....5....5....6....6....7....7....8"+
    			"\n1...5....0....5....0....5....0....5....0....5....0....5....0....5....0....5....0", e);
//    			+"\n*** fields:\n"+f+"\n*** join:\n"+j, e);
    	}
    }
    
    /**
     * Creates a new <code>ClassTable</code> tailored to handle this
     * <code>Class</code> and store it in database.
     * 
     * @param c		the <code>Class</code> we need a new <code>ClassTable</code> for
     * @return		the newly created <code>ClassTable</code>
     * @throws OSQLException	thrown if an error occurs during the creation process
     */
    private ClassTable createClassTable (Class c)
    throws OSQLException {
        // Check if we need to create c's superclass table 
        Class sc = c.getSuperclass();
        ClassTable st = null;
        if (sc!=null && sc!=Object.class)
            // Get superclass Table
            st = getClassTable(sc);
        // Check if we need to create c's topclass table 
        Class tc = c.getSuperclass();
        if (tc==Object.class)
        	tc = null;
        else 
        	while (tc.getSuperclass()!=Object.class)
        		tc = tc.getSuperclass();
        // Store in db
        ClassTable table = new ClassTable(sysConn, c, st, getClassTable(tc));
        tables.put(c, table);
        return table;
    }
    
    
    /**
     * Returns the <code>ClassTable</code> object associated with this
     * <code>Class</code>, creating it if need be.
     * <p>
     * Process goes as follow:
     * <ul>
     * <li>if the <code>ClassTable</code> object is in cache, it is returned</li>
     * <li>if there is a database record for this <code>ClassTable</code>, a new
     * object is reconstructed from it and returned</li>
     * <li>otherwise a new <code>ClassTable</code> tailored to this <code>Class</code>
     * is built from scratch, stored in database and returned</li>
     * </ul>
     * 
     * @param c		the target <code>Class</code>
     * @return		the associated <code>ClassTable</code> object
     * @throws OSQLException	if any problem occurs during this process
     */
    ClassTable getClassTable (Class c)
    throws OSQLException {
        if (c==null || c==Object.class)
            return null;
        if (c.isArray())
        	c = ArrayWrapper.class;
        // Attempt to get existing ClassTable
        ClassTable t = getExistingClassTable(c);
        if (t!=null)
            return t;
        // ClassTable is unknown, build a new one from scratch
        return createClassTable(c);
    }


    /**
     * Returns the <code>ClassTable</code> object associated with this
     * <code>className</code>, creating it if need be.
     * <p>
     * This method is a shortcut to the generic
     * {@link getTable(java.lang.Class c)} method.
     * 
     * @param	className		the name of the target <code>Class</code>
     * @return					the associated <code>ClassTable</code> object
     * @throws	OSQLException	if target <code>Class</code> is not found, or if
     * 							any other problem occurs
     * @see		getClassTable(Class c)
     */
    ClassTable getClassTable(String className)
    throws OSQLException {
    	try {
    		return getClassTable(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new OSQLException("Class not found: "+className, e);
        }
    }


    /**
     * Returns the <code>ClassTable</code> object associated with this
     * <code>Class</code> if it exists either in cache or in the database.
     * 
     * @param c		the target <code>Class</code>
     * @return		the associated <code>ClassTable</code> object if it exists
     * 				either in cache or in the database, <code>null</code>
     * 				otherwise
     * @throws OSQLException	if any problem occurs during this process
     */
    private ClassTable getExistingClassTable (Class c)
    throws OSQLException {
        // Attempt to get table from cache
        ClassTable t = getClassTableFromCache(c);
        if (t!=null)
            return t;
        // Table not in cache, attempt to load it from database
        return getClassTableFromDB(c);
    }
    
    
    /**
     * Returns the cached <code>ClassTable</code> associated with this
     * <code>Class</code>.
     * 
     * @param c	the target <code>Class</code>
     * @return	the associated <code>ClassTable</code> object, <code>null</code>
     * 			if it is not in cache 
     */
    private ClassTable getClassTableFromCache(Class c) {
        return (ClassTable)tables.get(c);
    }
    
    
    /**
     * Returns the cached <code>ClassTable</code> associated with this object
     * <code>Class</code>.
     * 
     * @param o	the target object
     * @return	the associated <code>ClassTable</code> object, <code>null</code>
     * 			if it is not in cache 
     * @see		getClassTableFromCache(java.lang.Class c)
     */
    private ClassTable getClassTableFromCache(Object o) {
        return getClassTableFromCache(o.getClass());
    }
    

    /**
     * Returns the <code>ClassTable</code> associated with this
     * <code>Class</code> from database.
     * 
     * @param c	the target <code>Class</code>
     * @return	the associated <code>ClassTable</code> object, <code>null</code>
     * 			if it is not in database
     * @throws OSQLException	if any problem occurs
     */
    private ClassTable getClassTableFromDB (Class c)
    throws OSQLException {
    	/*
    	 * Create SQL SELECT query
    	 */ 
        StringBuffer q = new StringBuffer("SELECT * FROM \"");
        q.append(ClassTable.OSQL_TABLE_NAME);
        q.append("\" WHERE \"class\"='");
        q.append(c.getName());
        q.append("'");
        /*
         * Retrieve ClassTable from DB
         */ 
        ResultSet rs;
        try {
        	// Attempt to execute query
            rs = statement.executeQuery(q.toString());
        } catch (SQLException e) {
        	// Error: system class table is missing
            // "Table not found: <OSQL Tables table name>" 
            throw new OSQLException("System table \""+ClassTable.OSQL_TABLE_NAME+"\" is probably missing.\nCould not execute SQL query: "+q.toString(), e);
        }
        try {
        	// Retrieve ClassTable ID
        	// Will raise a SQLException if there's no record for this Class
            rs.next();
            short id  = rs.getShort("id");
            /*
             * Reconstruct ClassTable from DB data
             */
            // Find top class
            Class tc = c.getSuperclass();
            if (tc==Object.class)
            	tc = null;
            else 
            	while (tc.getSuperclass()!=Object.class)
            		tc = tc.getSuperclass();
            // Create ClassTable object
            //
            // If we have this ClassTable in DB its superclasses must be either
            // in cache or in DB as well (unless someone messed up with system
            // tables). Thus, calls to getTable will not result in the creation
            // of new DB records.
            ClassTable t = new ClassTable(sysConn, id, c,
            	getClassTable(c.getSuperclass()),
            	getClassTable(tc));
            // Store it in cache and return it
            tables.put(c, t);
            return t;
        } catch (SQLException e) {
            // "No data available" => ClassTable is not in DB
            return null;
        } finally {
        	// If we went that far we have a ResultSet, so let's close it
            try {
                rs.close();
            } catch (SQLException ee) {}
        }
    }
    
    
    
    private ObjectRecord getObjectRecord(Object o)
    throws OSQLException {
        ClassTable t = getClassTableFromCache(o.getClass());
        return getObjectRecord(o, t);
    }
    
    
    private ObjectRecord getObjectRecord(Object o, ClassTable t)
    throws OSQLException {
        if (t==null)
            return null;
        return t.cache.get(o);
    }
    
    
    public synchronized String print (Object o) {
        if (o==null)
            return null;
        StringBuffer out = new StringBuffer("### Object structure for:   ");
        out.append(o);
        out.append("\n");
        print (o, new IdentityHashMap(), out, "    ");
        return out.toString();
    }

    
    private void print (Object o, IdentityHashMap p, StringBuffer out, String tab) {
        p.put(o, null);
        print(o, o.getClass(), p, out, tab);
    }

    
    private void print (Object o, Class c, IdentityHashMap p, StringBuffer out, String tab) {
		Class sc = c.getSuperclass();
        if (sc!=Object.class)
            print(o, sc, p, out, tab);
        
        // Class description
        String modif = Modifier.toString(c.getModifiers());
        StringBuffer def = new StringBuffer(modif);
        if (modif!="") {
			def.append(' ');
        }
        def.append("(");
		def.append(c.getName().replaceFirst("java.lang.", ""));
        def.append(")  ");
        Integer id = null;
        try {
        	ObjectRecord r = getObjectRecord(o);
        	if (r!=null)
        		id = r.id;
        } catch (OSQLException e) {}
        def.insert(0, "#"+id+" ");
        def.insert(0, tab);
        String className = def.toString();
		
		// Make fields accessible
		Field[] fields = c.getDeclaredFields();
		try {
				AccessibleObject.setAccessible(fields, true);
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		// Fields description
		Field field; Object fo;
		for (int f=0; f<fields.length; f++) {
            try {
    			field = fields[f];
                // Do we need to save this field?
                if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    continue;
    //			out.setLength(0);
                out.append(className);
                modif = Modifier.toString(field.getModifiers());
                if (modif!="") {
                    out.append(modif);
    				out.append(' ');
                }
                fo = field.get(o);
//                Array a;
                if (field.getType().isArray()) {
                    String name = field.getType().getName();
                    switch (name.charAt(1)) {
                    case 'L':
                        name = name.substring(2, name.length()-1);
                        out.append(name.replaceFirst("java.lang.", ""));
                        if (fo==null) {
                            out.append("[] ");
                            out.append(field.getName());
                            out.append(" = NULL\n");
                            continue;
                        }
                        int length = java.lang.reflect.Array.getLength(fo);
                        out.append("[");
                        out.append(length);
                        out.append("] ");
                        out.append(field.getName());
                        out.append(" = ");
                        if (length==0) {
                            out.append("{}   #");
                            out.append(getObjectRecord(fo));
                            out.append("\n");
                            continue;
                        }
                        out.append("{\n");
//                        out.append(objects.get(fo));
//                        out.append(")\n");
                        Object ao;
                        for (int i=0; i<length; i++) {
                            ao = java.lang.reflect.Array.get(fo, i);
                            if (ao==null)
                                out.append(tab+"    ["+i+"] NULL\n");
                            else
                                print(ao, ao.getClass(), p, out, tab+"    ["+i+"] ");
                        }
                        out.append(tab+"    }\n");
                    }
                    continue;
                }
       			out.append(field.getType().getName().replaceFirst("java.lang.", ""));
       			out.append(' ');
    			out.append(field.getName());
    			out.append(" = ");
                if (fo==null) {
                    out.append("NULL\n");
                    continue;
                }
                if (field.getType()==String.class) {
                    out.append("\"");
                    out.append(fo);
                    out.append("\"\n");
                    continue;
                }
				if (Object.class.isAssignableFrom(field.getType())) {
//                    out.append("  (id=");
//                    out.append(objects.get(fo));
                    out.append("\n");
                    if (p.containsKey(fo)) {
                        out.append(tab);
                        out.append("    ");
                        out.append(fo);
                        out.append("\n");
                        continue;
                    }
                    print (fo, p, out, tab+"    ");
                    continue;
                }
                out.append(fo);
                out.append("\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    
    synchronized void close (AdminConnection conn)
    throws OSQLException {
        if (conn!=sysConn)
            throw new OSQLException("You need administrator privileges.");
        if (logLevel>LOG_NONE) {
            StringBuffer log = new StringBuffer("*** Closing database ");
            log.append(sysConn.user);
            log.append("@");
            log.append(url);
            log.append(" ...  (up ");
            log.append((System.currentTimeMillis()-startTime)/1000.0);
            log.append("s)");
            out.println(log.toString());
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new OSQLException("Could not commit last changes before closing.", e);
        }
        quit();
    }
	
    
    private synchronized void quit () {
        try {
            finalize();
        } catch (Throwable e) {}
    }
    
    
	protected void finalize ()
	throws Throwable {
//        super.finalize();
//		statement.executeUpdate("SHUTDOWN COMPACT");
		try {
            if (statement!=null)
                statement.close();      // Attempt to close SQL statement
		} catch (SQLException e) {}
        if (connection!=null)
            connection.close();         // Attempt to close connection to SQL database
	}
	
    
	String uptime() {
		StringBuffer uptime = new StringBuffer();
		uptime(uptime);
		return uptime.toString();
	}
	
	private void uptime(StringBuffer buf) {
		long uptime = System.currentTimeMillis()-startTime;
		long d, h, m; d = h = m = 0;
		if (uptime>=86400000) {
			d = uptime/86400000;
			buf.append(d);
			buf.append('d');
			uptime -= d*86400000;
		}
		if (uptime>=3600000) {
			h = uptime/3600000;
			buf.append(h);
			buf.append('h');
			uptime -= h*3600000;
		}
		if (uptime>=60000) {
			m = uptime/60000;
			buf.append(m);
			buf.append('m');
			uptime -= m*60000;
		}
		buf.append(uptime/1000.0);
		buf.append('s');
	}
	
	
/*    public static void main (String[] args) {
        try {
            Database a = new Database("jdbc:hsqldb:hsql://localhost", "sa", "");
/*            try {
                Class.forName("org.hsqldb.jdbcDriver");
                java.sql.Connection conn = DriverManager.getConnection ("jdbc:hsqldb:hsql://localhost", "sa", "");
                java.sql.Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery("SELECT * FROM \".Table\" WHERE \"className\"='java.lang.Object'");
                rs.next();
                Integer id = (Integer)rs.getObject("superclassTable");
                System.out.println(id);
            } catch (Exception ee) {
                ee.printStackTrace();
            }*/
/*            a.close(a.sysConn);
        } catch (OSQLException e) {
            e.printStackTrace();
        }
    }
*/
	
	
	
	final class RequestCache {
		
	
	    final IdentityHashMap<Object,ObjectRecord> objects;
	    final HashMap<Integer,ObjectRecord>        ids;
		
		
		private RequestCache () {
	        this.objects = new IdentityHashMap<Object,ObjectRecord>();
	        this.ids     = new HashMap<Integer,ObjectRecord>();
		}
	    
		
		private ObjectRecord get(Object o) {
			return objects.get(o);
		}

		
		void put (Object o, ObjectRecord r) {
	        objects.put(o, r);
	        ids.put(r.id, r);
		}
		
		
		private void clear() {
			objects.clear();
			ids.clear();
		}
	    
		
	    private void cacheAll() {
	        Iterator i = objects.keySet().iterator();
	        Object o; ClassTable t; ObjectRecord r;
	        while (i.hasNext()) {
	            o = i.next();
	            if (o==null)
	            	continue;
	            t = getClassTableFromCache(o.getClass());
	            r = (ObjectRecord)objects.get(o);
	            t.cache.put(r);
//	            System.out.println("Caching "+o+" #"+r.id+" ("+t.ids.size()+")");
	        }
	    }
	    
	    
	}
	
	
	
}