/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package database.js.servers;

import java.io.PrintStream;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import database.js.config.Config;
import database.js.control.Process;
import database.js.cluster.Cluster;
import database.js.pools.ThreadPool;
import java.io.BufferedOutputStream;
import database.js.servers.http.HTTPServer;
import database.js.servers.rest.RESTServer;
import database.js.cluster.Cluster.ServerType;
import database.js.servers.http.HTTPServerType;


/**
 *
 * The start/stop sequence is rather complicated:
 *
 * There are 2 roles:
 *   The ipc guarantees that only 1 process holds the given role at any time.
 *
 *   The secretary role. All processes is eligible for this role
 *   The manager role. Only http processes is eligible for this role
 *
 *
 * The secretary is responsible for keeping all other servers alive.
 * The manager is responsible for the http interfaces, including the admin port.
 *
 * When a server starts, it will check to see if it has become the secretary. in which
 * case it will start all other processes that is not running.
 *
 * When the manager receives a shutdown command, it will pass it on to the secretary.
 * The secretary will then cease the automatic keep alive, and send a shutdown message
 * to all other processes, and shut itself down.
 *
 */
public class Server extends Thread
{
  private final short id;
  private final long started;
  private final int heartbeat;
  private final Logger logger;
  private final Config config;
  private final boolean embedded;

  private long requests = 0;
  private volatile boolean stop = false;
  private volatile boolean shutdown = false;
  
  private final HTTPServer ssl;
  private final HTTPServer plain;
  private final HTTPServer admin;

  private final RESTServer rest;

  
  public static void main(String[] args)
  {
    try {new Server(Short.parseShort(args[0]));}
    catch (Exception e) {e.printStackTrace();}
  }
  
  
  public Server(short id) throws Exception
  {
    this.id = id;
    this.config = new Config();
    PrintStream out = stdout();
    this.setName("Server Main");

    System.setOut(out);
    System.setErr(out);

    config.getLogger().open(id);
    this.logger = config.getLogger().logger;    
    this.started = System.currentTimeMillis();
    Process.Type type = Cluster.getType(config,id);
        
    this.embedded = config.getTopology().servers() > 0;
    this.heartbeat = config.getTopology().heartbeat();

    if (type == Process.Type.rest)
    {
      this.ssl = null;
      this.plain = null;
      this.admin = null;
      this.rest = new RESTServer(this);
    }
    else
    {
      this.rest = null;
      this.ssl = new HTTPServer(this,HTTPServerType.ssl,embedded);
      this.plain = new HTTPServer(this,HTTPServerType.plain,embedded);
      this.admin = new HTTPServer(this,HTTPServerType.admin,embedded);

      this.startup();
    }

    this.start();
    this.ensure();
    
    logger.info("Instance startet"+System.lineSeparator());
  }
  
  
  private void startup()
  {
    if (!open())
    {
      logger.info("Address already in use");
      return;
    }
    
    logger.info("Open http sockets");

    ssl.start();
    plain.start();
    admin.start();
  }
  
  
  private boolean open()
  {
    try
    {
      ServerSocket socket = null;
      
      socket = new ServerSocket(ssl.port());
      socket.close();
      
      socket = new ServerSocket(plain.port());
      socket.close();
      
      socket = new ServerSocket(admin.port());
      socket.close();
      
      return(true);
    }
    catch (Exception e)
    {
      return(false);
    }
  }
  
  
  private void ensure()
  {
    try 
    {
      synchronized(this)
      {
        if (!shutdown)
        {
          Process process = new Process(config);
          logger.fine("Checking all instances are up");
          
          ArrayList<ServerType> servers = Cluster.notRunning(this);
          
          for(ServerType server : servers)
          {
            logger.info("Starting instance "+server.id);
            process.start(server.type,server.id);
          }
        }        
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
  
  
  public Logger logger()
  {
    return(logger);
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  public long started()
  {
    return(started);
  }
  
  
  public Config config()
  {
    return(config);
  }
  
  
  public synchronized void request()
  {
    requests++;
  }
  
  
  public synchronized long requests()
  {
    return(requests);
  }
  public void shutdown()
  {
    this.shutdown = true;
        
    synchronized(this)
    {
      stop = true;
      this.notify();
    }
  }
  
  
  @Override
  public void run()
  {
    try 
    {
      synchronized(this)
      {
        while(!stop)
        {
          Cluster.setStatistics(this);
          this.wait(4*this.heartbeat);
        }
      }
      
      sleep(100);
    }
    catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}
    
    ThreadPool.shutdown();
    logger.info("Server stopped");
  }
  
  
  private PrintStream stdout() throws Exception
  {
    String srvout = config.getLogger().getServerOut(id);
    return(new PrintStream(new BufferedOutputStream(new FileOutputStream(srvout)), true));
  }
}