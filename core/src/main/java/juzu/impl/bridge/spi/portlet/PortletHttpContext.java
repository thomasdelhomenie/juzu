/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.bridge.spi.portlet;

import juzu.request.HttpContext;

import javax.portlet.ClientDataRequest;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.servlet.http.Cookie;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class PortletHttpContext implements HttpContext {

  /** . */
  private final PortletRequest request;

  public PortletHttpContext(PortletRequest request) {
    this.request = request;
  }

  public String getMethod() {
    if (request instanceof RenderRequest) {
      return GET_METHOD;
    } else {
      return ((ClientDataRequest)request).getMethod();
    }
  }

  public Cookie[] getCookies() {
    return request.getCookies();
  }

  public String getScheme() {
    return request.getScheme();
  }

  public int getServerPort() {
    return request.getServerPort();
  }

  public String getServerName() {
    return request.getServerName();
  }

  public String getContextPath() {
    return request.getContextPath();
  }
}
