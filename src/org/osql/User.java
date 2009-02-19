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

import java.sql.ResultSet;
import java.sql.SQLException;


class User {

    
    static final String OSQL_TABLE_NAME = ".User"; 
    

    final short id;
    String login;
//    private String password;
    short group;
    String groups;
//    transient boolean admin = false;
    

    /**
     * Find an existing OSQL user in database from specified login/password.
     * 
     * @param db
     * @param login
     * @param password
     * @throws OSQLException
     * @throws OSQLAccessViolation
     */
    User (Database db, String login, String password)
    throws OSQLException, OSQLAccessViolation {
    	if (login==null)
    		throw new OSQLException ("NULL login not allowed.");
    	if (login.equals(""))
    		throw new OSQLException ("Empty login not allowed.");
    	if (password==null)
    		password = "";
        StringBuffer q = new StringBuffer("SELECT * FROM \".User\" WHERE \"login\"='");
        q.append(login);
//        q.append("' AND \"password\"='");
//        q.append(password);
        q.append("'");
        ResultSet rs;
        try {
            rs = db.statement.executeQuery(q.toString());
            db.connection.commit();
        } catch (SQLException e) {
            throw new OSQLException("System table \""+OSQL_TABLE_NAME+"\" is probably missing.\nCould not execute SQL query: "+q.toString(), e);
        }
        try {
            rs.next();
            String pwd = rs.getString(3);
            if (!password.equals(pwd))
                throw new OSQLAccessViolation("Wrong login/password: "+login+"/"+password);
            this.id = rs.getShort(1);
            this.group = rs.getShort(4);
            this.login = login;
//            if (login.equals("root"))
//                this.admin = true;
        } catch (SQLException e) {
        	// User does not exist
            throw new OSQLException("Unknown user: "+login, e);
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new OSQLException("Could not close SQL result set:\n"+rs.toString(), e);
            }
        }
    }
    
    
    /**
     * Create a new OSQL user, with specified login/password and main group.
     * 
     * @param db
     * @param login
     * @param password
     * @param mainGroup
     * @throws OSQLException
     * @throws OSQLAccessViolation
     */
    User(Database db, String login, String password, short mainGroup, String groups)
    throws OSQLException, OSQLAccessViolation {
    	if (login==null)
    		throw new OSQLException ("NULL login not allowed.");
    	if (login.equals(""))
    		throw new OSQLException ("Empty login not allowed.");
    	if (password==null)
    		password = "";
        this.login = login;
//        if (login.equals("root"))
//            this.admin = true;
//        if (mainGroup==null)
//            throw new OSQLException("You must provide a valid main group for this user.");

        StringBuffer q = new StringBuffer("SELECT TOP 1 \"id\" FROM \"");
        q.append(OSQL_TABLE_NAME);
        q.append("\" ORDER BY \"id\" DESC");
        ResultSet rs;
        try {
            rs = db.statement.executeQuery(q.toString());
        } catch (SQLException e) {
            throw new OSQLException("System table \""+OSQL_TABLE_NAME+"\" is probably missing.\nCould not execute SQL query: "+q.toString(), e);
        }
        short id;
        try {
            rs.next();
            id = (short)(rs.getShort(1)+1);
            if (id<0)
            	throw new OSQLException("Maximum number of users (32768) reached. Could not create user \""+login+"\".");
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
        this.id = id;
        this.group = mainGroup;
        // Attempt to create new user with unique login
        q.setLength(0);
        q.append("INSERT INTO \"");
        q.append(OSQL_TABLE_NAME);
        q.append("\" VALUES (");
        q.append(id);
        q.append(", '");
        q.append(login);
        q.append("', '");
        q.append(password);
        q.append("', ");
        q.append(mainGroup);
        q.append(", NULL)");
        try {
            db.statement.executeUpdate(q.toString());
        } catch (SQLException e) {
            throw new OSQLAccessViolation("User already exists: "+login, e);
        }
    }
    
    
    String getViewName(ClassTable ct) {
        StringBuffer tn = new StringBuffer();
//        if (!admin)
//            tn.append(login);
        tn.append(":");
        tn.append(ct.table.name);
        return tn.toString();
    }
    
    
    public boolean equals(Object o) {
        if (o.getClass()!=User.class)
            return false;
        User user = (User)o;
        if (login.equals(user.login))// && password.equals(user.password))
            return true;
        return false;
    }
    
    
    public String toString() {
        return login;
    }
    
    
}
