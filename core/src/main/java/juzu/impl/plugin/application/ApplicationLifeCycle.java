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

package juzu.impl.plugin.application;

import juzu.Scope;
import juzu.impl.asset.AssetManager;
import juzu.impl.asset.AssetServer;
import juzu.impl.common.Filter;
import juzu.impl.common.Name;
import juzu.impl.common.NameLiteral;
import juzu.impl.common.Tools;
import juzu.impl.fs.spi.disk.DiskFileSystem;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.fs.spi.jar.JarFileSystem;
import juzu.impl.inject.BeanDescriptor;
import juzu.impl.inject.spi.BeanLifeCycle;
import juzu.impl.inject.spi.InjectionContext;
import juzu.impl.inject.spi.Injector;
import juzu.impl.inject.spi.InjectorProvider;
import juzu.impl.inject.spi.spring.SpringBuilder;
import juzu.impl.common.Logger;
import juzu.impl.plugin.Plugin;
import juzu.impl.plugin.application.descriptor.ApplicationDescriptor;
import juzu.impl.plugin.asset.AssetPlugin;
import juzu.impl.plugin.module.ModuleLifeCycle;
import juzu.impl.resource.ClassLoaderResolver;
import juzu.impl.resource.ResourceResolver;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.jar.JarFile;

/**
 * The application life cycle.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ApplicationLifeCycle<P, R> {

  /** Configuration: name. */
  protected Name name;

  /** Configuration: injector provider. */
  protected InjectorProvider injectorProvider;

  /** Contextual: logger. */
  protected final Logger logger;

  /** Contextual: resources. */
  protected ReadFileSystem<R> resources;

  /** Contextual: resoure resolver. */
  protected ResourceResolver resourceResolver;

  /** Contextual: asset server. */
  protected AssetServer assetServer;

  /** Contextual: module. */
  private final ModuleLifeCycle<R, P> module;

  /** . */
  protected ApplicationDescriptor descriptor;

  /** . */
  protected AssetManager stylesheetManager;

  /** . */
  protected AssetManager scriptManager;

  /** . */
  private InjectionContext<?, ?> injectionContext;

  /** . */
  private BeanLifeCycle<Application> application;

  public ApplicationLifeCycle(Logger logger, ModuleLifeCycle<R, P> module) {
    this.logger = logger;
    this.module = module;
  }

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public InjectorProvider getInjectorProvider() {
    return injectorProvider;
  }

  public void setInjectorProvider(InjectorProvider injectorProvider) {
    this.injectorProvider = injectorProvider;
  }

  public ReadFileSystem<R> getResources() {
    return resources;
  }

  public void setResources(ReadFileSystem<R> resources) {
    this.resources = resources;
  }

  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  public void setResourceResolver(ResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  public Application getApplication() {
    return application != null ? application.peek() : null;
  }

  public AssetServer getAssetServer() {
    return assetServer;
  }

  public AssetManager getScriptManager() {
    return scriptManager;
  }

  public AssetManager getStylesheetManager() {
    return stylesheetManager;
  }

  public void setAssetServer(AssetServer assetServer) {
    if (assetServer != null) {
      assetServer.register(this);
    }
    if (this.assetServer != null) {
      this.assetServer.unregister(this);
    }
    this.assetServer = assetServer;
  }

  public ApplicationDescriptor getDescriptor() {
    return descriptor;
  }

  public ModuleLifeCycle<R, P> getModule() {
    return module;
  }

  public boolean refresh() throws Exception {
    boolean changed = getModule().refresh();

    //
    if (application != null) {
      if (changed) {
        application = null;
      }
    }

    //
    if (application == null) {
      logger.log("Building application");
      doBoot();
      return true;
    } else {
      return false;
    }
  }

  protected final void doBoot() throws Exception {
    ReadFileSystem<P> classes = getModule().getClasses();

    //
    Name fqn = name.append("Application");

    //
    Class<?> clazz = getModule().getClassLoader().loadClass(fqn.toString());
    ApplicationDescriptor descriptor = ApplicationDescriptor.create(clazz);

    // Find the juzu jar
    URL mainURL = ApplicationLifeCycle.class.getProtectionDomain().getCodeSource().getLocation();
    if (mainURL == null) {
      throw new Exception("Cannot find juzu jar");
    }
    if (!mainURL.getProtocol().equals("file")) {
      throw new Exception("Cannot handle " + mainURL);
    }
    File file = new File(mainURL.toURI());
    ReadFileSystem<?> libs;
    if (file.isDirectory()) {
      libs = new DiskFileSystem(file);
    } else {
      libs = new JarFileSystem(new JarFile(file));
    }

    //
    Injector injector = injectorProvider.get();
    injector.addFileSystem(classes);
    injector.addFileSystem(libs);
    injector.setClassLoader(getModule().getClassLoader());

    //
    if (injector instanceof SpringBuilder) {
      R springName = resources.getPath("spring.xml");
      if (springName != null) {
        URL configurationURL = resources.getURL(springName);
        ((SpringBuilder)injector).setConfigurationURL(configurationURL);
      }
    }

    // Bind the resolver
    ClassLoaderResolver resolver = new ClassLoaderResolver(getModule().getClassLoader());
    injector.bindBean(ResourceResolver.class, Collections.<Annotation>singletonList(new NameLiteral("juzu.resource_resolver.classpath")), resolver);
    injector.bindBean(ResourceResolver.class, Collections.<Annotation>singletonList(new NameLiteral("juzu.resource_resolver.server")), this.resourceResolver);

    //
    logger.log("Starting " + descriptor.getName());
    InjectionContext<?, ?> injectionContext = _start(descriptor, injector);

    //
    AssetPlugin assetPlugin = injectionContext.get(AssetPlugin.class).get();
    BeanLifeCycle<Application> application = injectionContext.get(Application.class);

    //
    this.injectionContext = injectionContext;
    this.scriptManager = assetPlugin.getScriptManager();
    this.stylesheetManager = assetPlugin.getStylesheetManager();
    this.descriptor = descriptor;
    this.application = application;

    // For application start (perhaps we could remove that)
    try {
      application.get();
    }
    catch (InvocationTargetException e) {
      throw new UnsupportedOperationException("handle me gracefully", e);
    }
  }

  private <B, I> InjectionContext<B, I> _start(final ApplicationDescriptor descriptor, Injector injector) throws ApplicationException {

    // Bind the application descriptor
    injector.bindBean(ApplicationDescriptor.class, null, descriptor);

    // Bind the application context
    injector.declareBean(Application.class, null, null, null);

    // Bind the scopes
    for (Scope scope : Scope.values()) {
      injector.addScope(scope);
    }

    // Bind the plugins
    for (Plugin plugin : descriptor.getPlugins().values()) {
      Class aClass = plugin.getClass();
      Object o = plugin;
      injector.bindBean(aClass, null, o);
    }

    // Bind the beans
    for (BeanDescriptor bean : descriptor.getBeans()) {
      bean.bind(injector);
    }

    // Filter the classes:
    // any class beginning with juzu. is refused
    // any class prefixed with the application package is accepted
    // any other application class is refused (i.e a class having an ancestor package annotated with @Application)
    Filter<Class<?>> filter = new Filter<Class<?>>() {
      HashSet<String> blackList = new HashSet<String>();
      public boolean accept(Class<?> elt) {
        if (elt.getName().startsWith("juzu.")) {
          return false;
        } else if (elt.getPackage().getName().startsWith(descriptor.getPackageName())) {
          return true;
        } else {
          for (String currentPkg = elt.getPackage().getName();currentPkg != null;currentPkg = Tools.parentPackageOf(currentPkg)) {
            if (blackList.contains(currentPkg)) {
              return false;
            } else {
              try {
                Class<?> packageClass = descriptor.getApplicationLoader().loadClass(currentPkg + ".package-info");
                juzu.Application ann = packageClass.getAnnotation(juzu.Application.class);
                if (ann != null) {
                  blackList.add(currentPkg);
                  return false;
                }
              }
              catch (ClassNotFoundException e) {
                // Skip it
              }
            }
          }
          return true;
        }
      }
    };

    //
    InjectionContext<B, I> injectionContext;
    try {
      injectionContext = (InjectionContext<B, I>)injector.create(filter);
    }
    catch (Exception e) {
      throw new UnsupportedOperationException("handle me gracefully", e);
    }

    //
    return injectionContext;
  }

  void stop() {
    if (application != null) {
      application.release();
    }
    if (injectionContext != null) {
      injectionContext.shutdown();
    }
  }

  public void shutdown() {
    stop();
  }
}
