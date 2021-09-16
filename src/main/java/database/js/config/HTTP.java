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

import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;


public class HTTP
{
  private final int ssl;
  private final int plain;
  private final int admin;
  private final String path;
  private final String version;
  private final String virtstr;
  private final String virtendp;
  private final Handlers handlers;
  private final boolean requiressl;
  private final ArrayList<String> corsdomains;
  
  
  HTTP(JSONObject config) throws Exception
  {
    JSONObject app = config.getJSONObject("application");
    
    String apppath = null;
    apppath = app.getString("path");
    
    if (apppath.startsWith("."))
    {
      apppath = Paths.apphome + File.separator + apppath;
      File appf = new File(apppath);
      apppath = appf.getCanonicalPath();
    }

    this.path = apppath;
    
    String version = "";
    
    if (!config.isNull("version"))
      version = app.getString("version");
    
    this.version = version;


    JSONObject ports = config.getJSONObject("ports");

    this.ssl = ports.getInt("ssl");
    this.plain = ports.getInt("plain");
    this.admin = ports.getInt("admin");

        
    boolean requiressl = false;
    JSONObject security = config.getJSONObject("security");
    
    if (security.has("require.ssl")) requiressl = security.getBoolean("require.ssl");
    this.requiressl = requiressl;

    String elem = "Cors-Allow-Sites";
    this.corsdomains = new ArrayList<String>();
    
    String corsdomains = null;

    if (security.has(elem) && !security.isNull(elem)) 
      corsdomains = security.getString(elem);
        
    if (corsdomains != null) 
    {
      String[] domains = corsdomains.split("[ ,]+");
      
      for (int i = 0; i < domains.length; i++)
      {
        domains[i] = domains[i].trim();
        if (!domains[i].equals("*")) domains[i] = "."+domains[i]+".";
        if (domains[i].length() > 0) this.corsdomains.add(domains[i]);
      }
    }
    

    JSONObject virtp = config.getJSONObject("virtual-path");
    
    this.virtstr = virtp.getString("strategi");
    this.virtendp = virtp.getString("endpoint");


    this.handlers = new Handlers();

    JSONArray handlers = config.getJSONArray("handlers");
    
    for (int i = 0; i < handlers.length(); i++)
    {
      JSONObject entry = handlers.getJSONObject(i);
      this.handlers.add(entry.getString("url"),entry.getString("methods"),entry.getString("class"));
    }
  }


  public int ssl()
  {
    return(ssl);
  }

  public int plain()
  {
    return(plain);
  }

  public int admin()
  {
    return(admin);
  }

  public String getAppPath()
  {
    return(path);
  }

  public String getAppVersion()
  {
    return(version);
  }

  public Handlers handlers()
  {
    return(handlers);
  }

  public boolean requiressl()
  {
    return(requiressl);
  }

  public ArrayList<String> corsdomains()
  {
    return(corsdomains);
  }
}
