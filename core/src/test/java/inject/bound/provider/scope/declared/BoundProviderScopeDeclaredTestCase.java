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

package inject.bound.provider.scope.declared;

import juzu.Scope;
import inject.AbstractInjectTestCase;
import juzu.impl.inject.spi.InjectorProvider;
import inject.ScopedKey;
import org.junit.Test;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class BoundProviderScopeDeclaredTestCase<B, I> extends AbstractInjectTestCase<B, I> {

  public BoundProviderScopeDeclaredTestCase(InjectorProvider di) {
    super(di);
  }

  @Test
  public void test() throws Exception {
    BeanProvider provider = new BeanProvider();

    //
    init();
    bootstrap.declareBean(Injected.class, null, null, null);
    bootstrap.bindProvider(Bean.class, Scope.REQUEST, null, provider);
    boot(Scope.REQUEST);

    //
    beginScoping();
    try {
      assertEquals(0, scopingContext.getEntries().size());
      Injected injected = getBean(Injected.class);
      assertNotNull(injected);
      assertNotNull(injected.injected);
      String value = injected.injected.getValue();
      assertEquals(1, scopingContext.getEntries().size());
      ScopedKey key = scopingContext.getEntries().keySet().iterator().next();
      assertEquals(Scope.REQUEST, key.getScope());
      Bean scoped = (Bean)scopingContext.getEntries().get(key).get();
      assertEquals(scoped.getValue(), value);
      assertSame(scoped, provider.bean);
    }
    finally {
      endScoping();
    }
  }
}
