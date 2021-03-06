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

package juzu.impl.plugin.controller;

import juzu.impl.plugin.application.descriptor.ApplicationDescriptor;
import juzu.impl.request.Method;
import juzu.impl.request.Parameter;
import juzu.impl.request.PhaseParameter;
import juzu.impl.common.Cardinality;
import juzu.request.Phase;
import juzu.test.AbstractTestCase;
import juzu.test.CompilerAssert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ResourceMethodTestCase extends AbstractTestCase {

  @Override
  public void setUp() throws Exception {
    CompilerAssert<?, ?> compiler = compiler("plugin.controller.method.resource");
    compiler.assertCompile();

    //
    aClass = compiler.assertClass("plugin.controller.method.resource.A");
    compiler.assertClass("plugin.controller.method.resource.A_");
    Class<?> appClass = compiler.assertClass("plugin.controller.method.resource.Application");
    descriptor = ApplicationDescriptor.create(appClass);
  }

  /** . */
  private Class<?> aClass;

  /** . */
  private ApplicationDescriptor descriptor;

  @Test
  public void testNoArg() throws Exception {
    Method cm = descriptor.getControllers().getMethod(aClass, "noArg");
    assertEquals("noArg", cm.getName());
    assertEquals(Phase.RESOURCE, cm.getPhase());
    assertEquals(Collections.<Parameter>emptyList(), cm.getParameters());
  }

  @Test
  public void testStringArg() throws Exception {
    Method cm = descriptor.getControllers().getMethod(aClass, "oneArg", String.class);
    assertEquals("oneArg", cm.getName());
    assertEquals(Phase.RESOURCE, cm.getPhase());
    assertEquals(Arrays.asList(new PhaseParameter("foo", null, Cardinality.SINGLE)), cm.getParameters());
  }
}
