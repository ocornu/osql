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



public final class AdminConnection
extends Connection {


	AdminConnection(Database db, User user)
	throws OSQLException {
		super(db, user);
		// TODO Auto-generated constructor stub
	}

	
    public void close ()
    throws OSQLException {
        db.close(this);
    }
    
    
    public void addUser(String login, String password)
    throws OSQLException {
        db.addUser(this, login, password);
    }
    
    public void addUser(String login, String password, String mainGroup)
    throws OSQLException {
        db.addUser(this, login, password, mainGroup);
    }
    
    public void addGroup(String name)
    throws OSQLException {
        db.addGroup(this, name);
    }
    
    
}
