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

/**
 * @author zit
 *
 */
public class OSQLException
extends RuntimeException {

    
    /**
     * 
     */
    public OSQLException() {
        super();
        // TODO Auto-generated constructor stub
    }

    
    /**
     * @param arg0
     */
    public OSQLException(String arg0) {
        super("!!! "+arg0);
        // TODO Auto-generated constructor stub
    }

    
    /**
     * @param arg0
     */
    public OSQLException(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    
    /**
     * @param arg0
     * @param arg1
     */
    public OSQLException(String arg0, Throwable arg1) {
        super("!!! "+arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    
}
