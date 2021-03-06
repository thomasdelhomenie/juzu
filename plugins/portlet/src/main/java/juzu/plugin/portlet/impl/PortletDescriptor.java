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

package juzu.plugin.portlet.impl;

import juzu.Scope;
import juzu.impl.inject.BeanDescriptor;
import juzu.impl.metadata.Descriptor;
import juzu.impl.common.Tools;

import javax.portlet.PortletPreferences;
import java.util.ResourceBundle;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class PortletDescriptor extends Descriptor {

  /** . */
  public static PortletDescriptor INSTANCE = new PortletDescriptor();

  private PortletDescriptor() {
  }

  @Override
  public Iterable<BeanDescriptor> getBeans() {
    return Tools.list(
        BeanDescriptor.createFromProviderType(PortletPreferences.class, Scope.REQUEST, null, PortletPreferencesProvider.class),
        BeanDescriptor.createFromProviderType(ResourceBundle.class, Scope.REQUEST, null, ResourceBundleProvider.class)
    );
  }
}
