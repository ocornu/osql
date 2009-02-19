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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;

public class TableNameMapper {

    
//    private static final String PACKAGE = "org.osql.";	//Database.class.getPackage().getName()+".";
    
    private IdentityHashMap<Class, String> tables;
    
    
    protected TableNameMapper () {
    	tables = new IdentityHashMap<Class, String>();
    }
    
/*    private final String getName(String className) {
        if (className.indexOf(PACKAGE)==0)
            return className.replaceFirst(PACKAGE, "");
        return wrap(className);
    }
*/
    
/*    final String getName(Class c) {
    	String tableName = tables.get(c);
    	// Do we have this table name in cache? Return it
    	if (tableName!=null)
    		return tableName;
    	// Let's see if class has a its own specific table name
        try {
        	Field f = c.getDeclaredField("OSQL_TABLE_NAME");
        	f.setAccessible(true);
        	if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))
        		tableName = (String)f.get(c);
        } catch (Exception e) {}
        // Found it?
        if (tableName!=null) {
        	tables.put(c, tableName);
        	return tableName;
        }
    	// Fallback on a global table name mapping
        return getName(c.getName());
    }
*/    
    final void setName(Class c, String tableName) {
       	tables.put(c, tableName);
    }
    
    
    protected String getName(String className) {
    	return className;
    }
    
    
}
