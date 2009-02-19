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


import java.util.List;


public class Connection {

    
    final Database db;
    final User user;
    
    byte defaultAccess = ObjectRecord.RWR_R_;
    
    
    Connection (Database db, User user)
    throws OSQLException {
        this.db = db;
        this.user = user;
    }
    
    
    public void store (Object o)
    throws OSQLException {
        db.store(this, o, defaultAccess);
    }
    
    public void store (Object o, byte access)
    throws OSQLException {
        db.store(this, o, access);
    }
    
    
    public Object get (Class c, String where)
    throws OSQLException {
        return db.get(this, c, where, null);
    }
        
    public Object get (Class c, String where, String order)
    throws OSQLException {
        return db.get(this, c, where, order);
    }
        
    public Object getFirst (Class c)
    throws OSQLException {
        return db.get(this, c, null, null);
    }
    
    public Object getLast (Class c)
    throws OSQLException {
        return db.get(this, c, null, "\".id\" DESC");
    }
    
    public Object getLast (Class c, String where)
    throws OSQLException {
        return db.get(this, c, where, "\".id\" DESC");
    }
    
    public List getAll (Class c)
    throws OSQLException {
        return db.getAll(this, c, null, null);
    }
    
    public List getAll (Class c, String where)
    throws OSQLException {
        return db.getAll(this, c, where, null);
    }
    
    public List getAll (Class c, String where, String order)
    throws OSQLException {
        return db.getAll(this, c, where, order);
    }
    
    
    public boolean remove (Object o)
    throws OSQLException {
        return db.remove(this, o);
    }
    
    public void removeAll (Class c)
    throws OSQLException {
        db.removeAll(this, c);
    }
    
    public int removeAll (Class c, String where)
    throws OSQLException {
        return db.removeAll(this, c, where);
    }    
    
    
    public int count (Class c)
    throws OSQLException {
        return db.count(this, c, null);
    }
    
    public int count (Class c, String where)
    throws OSQLException {
        return db.count(this, c, where);
    }
    
    
    public String upTime() {
        return db.uptime();
    }
 
    
    public String toString(Object o)
    throws OSQLException {
    	return db.toString(o);
    }
    
    
    Object select (String type, int oid)
    throws OSQLException {
    	return db.select(this, type, oid);
    }

    Object select (Class c, int oid)
    throws OSQLException {
        return db.select(this, c, oid);
    }
    
    ObjectRecord store (Object o, ObjectRecord parent, byte access)
    throws OSQLException, OSQLAccessViolation {
    	return db.store(this, o, parent, access);
    }
    
    
}
