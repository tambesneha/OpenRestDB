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

package database.js.handlers;

import database.js.config.Config;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;


public class FileHandler extends Handler
{
  private final PathUtil path;
  
  
  public FileHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
    this.path = new PathUtil(this);
  }
  
  
  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    request.server().request();
    HTTPResponse response = new HTTPResponse();
    String path = this.path.getPath(request.path());
    
    response.setLastModified();
    response.setBody("Hello there");
    
    return(response);
  }
}
