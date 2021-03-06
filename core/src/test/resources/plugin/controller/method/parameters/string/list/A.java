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

package plugin.controller.method.parameters.string.list;

import juzu.Response;
import juzu.View;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class A {

  @View(id = "none")
  public Response.Content<?> none() throws IOException {
    return Response.ok(A_.mv(null).toString());
  }

  @View(id = "0")
  public Response.Content<?> zero() throws IOException {
    return Response.ok(A_.mv(Collections.emptyList()).toString());
  }

  @View(id = "1")
  public Response.Content<?> one() throws IOException {
    return Response.ok(A_.mv(Arrays.asList("bar")).toString());
  }

  @View(id = "2")
  public Response.Content<?> two() throws IOException {
    return Response.ok(A_.mv(Arrays.asList("bar_1", "bar_2")).toString());
  }

  @View
  public Response.Content<?> mv(List<String> foo) throws IOException {
    return Response.ok(foo != null ? foo.toString() : "");
  }
}
