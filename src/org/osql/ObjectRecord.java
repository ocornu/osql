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


public final class ObjectRecord {


//    static final String OSQL_TABLE_NAME = ".Object"; 

    
    // UNIX-like primitive access rights
    public static final byte TRASHED        	= 0;
    public static final byte USER_READ      	= 1;
    public static final byte USER_WRITE     	= 2;
    public static final byte USER_READ_WRITE    = 3;
    public static final byte GROUP_READ     	= 4;
    public static final byte GROUP_WRITE    	= 8;
    public static final byte GROUP_READ_WRITE   = 12;
    public static final byte OTHER_READ     	= 16;
    public static final byte OTHER_WRITE    	= 32;
    public static final byte OTHER_READ_WRITE   = 48;

    public static final byte ______   = 0;
    public static final byte R_____   = 1;
    public static final byte RW____   = 3;
    public static final byte R_R___   = 5;
    public static final byte RWR___   = 7;
    public static final byte RWRW__   = 15;
    public static final byte R_R_R_   = 21;
    public static final byte RWR_R_   = 23;
    public static final byte RWRWR_   = 31;
    public static final byte RWRWRW   = 63;
    
    
    
    Integer id;
    private final WeakReference<Object> object;
//    Object lock;
//    final ClassTable ct;
    
    // Owner and privileges
    short	uid;
    short	gid;
    byte	access;
    
    
/*    ObjectRecord(ClassTable ct, User user, Integer oid, Object o, byte access) {
        this(ct, user.id, user.group, oid, o, access);
    }
    
    ObjectRecord(ClassTable ct, User user, int oid, Object o, byte access) {
        this(ct, user.id, user.group, new Integer(oid), o, access);
    }
    
    ObjectRecord(ClassTable ct, User user, Object o, byte access) {
        this(ct, user.id, user.group, null, o, access);
    }
    */
    ObjectRecord(ClassTable ct, short uid, short gid, Integer oid, Object o, byte access) {
//        this.ct = ct;
    	this.id = oid;
        this.object = new WeakReference<Object>(o);
        this.uid = uid;
        this.gid = gid;
        this.access = access;
    }

    
    boolean isWritable(User user) {
        if ((access&OTHER_WRITE)!=0)
            return true;
        if (this.uid==user.id && ((access&USER_WRITE)!=0))
            return true;
        if (this.gid==user.group && ((access&GROUP_WRITE)!=0))
            return true;
        return false;
    }
    
    
    protected void finalize()
    throws Throwable {
//    	ct.cache.remove(this);
//    	System.err.println("Finalizing object #"+id+": "+object+" ("+ct.cache.size()+")");
    }
    
    Object object() {
    	return object.get();
    }
    
}
