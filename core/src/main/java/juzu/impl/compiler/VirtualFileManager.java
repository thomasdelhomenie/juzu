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

package juzu.impl.compiler;

import juzu.impl.compiler.file.CompositeFileManager;
import juzu.impl.common.FileKey;
import juzu.impl.compiler.file.FileManager;
import juzu.impl.compiler.file.JavaFileObjectImpl;
import juzu.impl.compiler.file.SimpleFileManager;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.fs.spi.ReadWriteFileSystem;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class VirtualFileManager extends ForwardingJavaFileManager<JavaFileManager> implements StandardJavaFileManager {

  /** . */
  final SimpleFileManager<?> sourcePath;

  /** . */
  final SimpleFileManager<?> classOutput;

  /** . */
  final CompositeFileManager classPath;

  /** . */
  final SimpleFileManager<?> sourceOutput;

  public VirtualFileManager(
    JavaFileManager fileManager,
    ReadFileSystem<?> sourcePath,
    Collection<ReadFileSystem<?>> classPath,
    ReadWriteFileSystem<?> sourceOutput,
    ReadWriteFileSystem<?> classOutput) {
    super(fileManager);

    //
    this.sourcePath = safeWrap(StandardLocation.SOURCE_PATH, sourcePath);
    this.classPath = new CompositeFileManager(StandardLocation.CLASS_PATH, classPath);
    this.classOutput = safeWrap(StandardLocation.CLASS_OUTPUT, classOutput);
    this.sourceOutput = safeWrap(StandardLocation.SOURCE_OUTPUT, sourceOutput);
  }

  private <P> SimpleFileManager<P> safeWrap(StandardLocation location, ReadFileSystem<P> fs) {
    return fs != null ? new SimpleFileManager<P>(location, fs) : null;
  }

  private FileManager getFiles(Location location) {
    if (location instanceof StandardLocation) {
      switch ((StandardLocation)location) {
        case SOURCE_PATH:
          return sourcePath;
        case SOURCE_OUTPUT:
          return sourceOutput;
        case CLASS_OUTPUT:
          return classOutput;
        case CLASS_PATH:
          return classPath;
      }
    }
    return null;
  }

  //

  public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
    throw new UnsupportedOperationException();
  }

  public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
    throw new UnsupportedOperationException();
  }

  public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
    throw new UnsupportedOperationException();
  }

  public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
    throw new UnsupportedOperationException();
  }

  public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
    throw new UnsupportedOperationException();
  }

  public Iterable<? extends File> getLocation(Location location) {
    if (location == StandardLocation.CLASS_PATH) {
      FileManager manager = getFiles(location);
      if (manager != null) {
        try {
          HashSet<File> files = new HashSet<File>();
          manager.populateRoots(files);
          return files;
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  // **************

  @Override
  public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
    throw new UnsupportedOperationException("Does not seem used at the moment, for now we leave it as is");
  }

  @Override
  public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
    Iterable<JavaFileObject> ret;
    if (location == StandardLocation.PLATFORM_CLASS_PATH) {
      ret = super.list(location, packageName, kinds, recurse);
    }
    else {
      FileManager files = getFiles(location);
      if (files != null) {
        ret = files.list(packageName, kinds, recurse, new ArrayList<JavaFileObject>());
      }
      else {
        ret = Collections.emptyList();
      }
    }
    return ret;
  }

  @Override
  public String inferBinaryName(Location location, JavaFileObject file) {
    if (file instanceof JavaFileObjectImpl<?>) {
      JavaFileObjectImpl<?> fileClass = (JavaFileObjectImpl<?>)file;
      return fileClass.getKey().fqn;
    }
    else {
      return super.inferBinaryName(location, file);
    }
  }

  @Override
  public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
    FileKey key = FileKey.newResourceName(packageName, relativeName);
    FileManager files = getFiles(location);
    if (files != null) {
      return files.getReadable(key);
    }
    else {
      throw new FileNotFoundException("Cannot write: " + location);
    }
  }

  @Override
  public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
    FileKey key = FileKey.newResourceName(packageName, relativeName);

    // Address a bug
    if (location == StandardLocation.SOURCE_PATH) {
      FileObject file = sourcePath.getReadable(key);
      if (file == null) {
        throw new FileNotFoundException("Not found:" + key.toString());
      }
      return file;
    }
    else {
      FileManager files = getFiles(location);
      if (files != null) {
        return files.getWritable(key);
      }
      else {
        throw new FileNotFoundException("Cannot write: " + location);
      }
    }
  }

  @Override
  public boolean isSameFile(FileObject a, FileObject b) {
    FileKey ka = ((JavaFileObjectImpl)a).getKey();
    FileKey kb = ((JavaFileObjectImpl)b).getKey();
    return ka.equals(kb);
  }

  @Override
  public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
    FileManager files = getFiles(location);
    if (files != null) {
      FileKey key = FileKey.newJavaName(className, kind);
      return files.getWritable(key);
    }
    else {
      throw new UnsupportedOperationException("Location " + location + " not supported");
    }
  }
}
