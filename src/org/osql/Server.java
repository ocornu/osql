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
import java.net.ServerSocket;
import java.net.Socket;


public class Server {

    
    ServerSocket socket;
    
    
    public Server()
    throws IOException {
        socket = new ServerSocket();
        new Connection(socket.accept());
    }
    
    
    
    
    private class Connection
    implements Runnable {
        
        private final Socket socket;
        
        private Connection (Socket socket) {
            this.socket = socket;
            new Thread(this).run();
        }
        
        public void run() {
            try {
                // Init socket streams
                socket.getInputStream();
            } catch (IOException e) {
                
            }
        }
        
    }
    
    
    
}
