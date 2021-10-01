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

package database.js.config;

import org.json.JSONObject;


public class Topology
{
  private final boolean hot;
  private final short workers;
  private final short waiters;
  private final short servers;
  
  private final int extnds;
  private final String extsize;

  private static final int cores = Runtime.getRuntime().availableProcessors();
  
  
  Topology(JSONObject config) throws Exception
  {
    if (!config.has("servers")) servers = 0;
    else servers = (short) config.getInt("servers");
        
    short waiters = 0;
    short workers = 0;

    short multi = servers > 0 ? servers : 1;
    
    if (!config.isNull("waiters"))
      waiters = (short) config.getInt("waiters");
    
    if (waiters == 0) 
    {
      waiters = (short) (cores/2);
      if (waiters < 2) waiters = (short) cores;
    }
      
    this.waiters = waiters;
    
    if (!config.isNull("workers"))
      workers = (short) config.getInt("workers");
    
    if (workers > 0) this.workers = workers;
    else             this.workers = (short) (multi * 8 * cores);
    
    this.hot = config.getBoolean("hot-standby");
    
    JSONObject ipc = config.getJSONObject("ipc");
    
    this.extnds = ipc.getInt("extends");
    this.extsize = ipc.get("extsize").toString();
  }

  public short waiters()
  {
    return(waiters);
  }

  public short workers()
  {
    return(workers);
  }

  public short servers()
  {
    return(servers);
  }

  public boolean hotstandby()
  {
    return(hot);
  }

  public int extnds()
  {
    return(extnds);
  }

  public String extsize()
  {
    return(extsize);
  }
}
