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

package juzu.impl.compiler.file;

import juzu.impl.common.FileKey;
import juzu.impl.common.Timestamped;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.fs.spi.ReadWriteFileSystem;
import juzu.impl.common.Content;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class JavaFileObjectImpl<P> implements JavaFileObject {

  /***/
  final FileKey key;

  /** . */
  final ReadFileSystem<P> fs;

  /** The file. */
  private final P file;

  /** . */
  Timestamped<Content> content;

  /** . */
  private boolean writing;

  /** . */
  final URI uri;

  public JavaFileObjectImpl(JavaFileManager.Location location, FileKey key, ReadFileSystem<P> fs, P file) throws NullPointerException, IOException {

    //
    if (file == null) {
      throw new NullPointerException("No null file accepted for " + key);
    }

    //
    URI uri;
    try {
      uri = fs.getURL(file).toURI();
    }
    catch (URISyntaxException e) {
      throw new IOException("Could not create uri for file " + file, e);
    }

    //
    this.key = key;
    this.fs = fs;
    this.file = file;
    this.writing = false;
    this.uri = uri;
  }

  public FileKey getKey() {
    return key;
  }

  private Timestamped<Content> assertContent() throws IOException {
    if (writing) {
      throw new IllegalStateException("Opened for writing");
    } else {
      if (content == null) {
        content = fs.getContent(file);
        if (content == null) {
          throw new FileNotFoundException("File " + key + " cannot be found");
        }
      }
      return content;
    }
  }

  public File getFile() throws IOException {
    return fs.getFile(file);
  }

  public boolean isNameCompatible(String simpleName, Kind kind) {
    String baseName = simpleName + kind.extension;
    return kind.equals(getKind()) && (baseName.equals(toUri().getPath()) || toUri().getPath().endsWith("/" + baseName));
  }

  public Kind getKind() {
    return key.getKind();
  }

  public NestingKind getNestingKind() {
    return null;
  }

  public Modifier getAccessLevel() {
    return null;
  }

  public URI toUri() {
    return uri;
  }

  public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
    CharSequence charContent = getCharContent(ignoreEncodingErrors);
    if (charContent == null) {
      throw new IOException("No content");
    }
    return new StringReader(charContent.toString());
  }

  public boolean delete() {
    if (fs instanceof ReadWriteFileSystem) {
      try {
        return ((ReadWriteFileSystem<P>)fs).removePath(file);
      }
      catch (IOException ignore) {
        return false;
      }
    } else {
      return false;
    }
  }

  public String getName() {
    File f = fs.getFile(file);
    if (f != null) {
      try {
        return f.getCanonicalPath();
      }
      catch (IOException ignore) {
      }
    }
    return key.rawName;
  }

  /**
   * We subclass this in order to have the correct name in the stack trace. This is not really documented anywhere.
   *
   * @return the file name
   */
  @Override
  public String toString() {
    return key.rawName;
  }

  public long getLastModified() {
    try {
      return assertContent().getTime();
    }
    catch (IOException e) {
      // We return 0 as the javadoc say to do
      return 0;
    }
  }

  public final CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
    return assertContent().getObject().getCharSequence();
  }

  public final InputStream openInputStream() throws IOException {
    return assertContent().getObject().getInputStream();
  }

  public OutputStream openOutputStream() throws IOException {
    if (writing) {
      throw new IllegalStateException("Opened for writing");
    }
    if (fs instanceof ReadWriteFileSystem<?>) {
      final ReadWriteFileSystem<P> fs = (ReadWriteFileSystem<P>)this.fs;
      return new ByteArrayOutputStream() {
        @Override
        public void close() throws IOException {
          Content content = new Content(toByteArray(), null);
          long lastModified = fs.setContent(file, content);
          JavaFileObjectImpl.this.content = new Timestamped<Content>(lastModified, content);
          JavaFileObjectImpl.this.writing = false;
        }
      };
    }
    else {
      throw new UnsupportedOperationException("Read only");
    }
  }

  public Writer openWriter() throws IOException {
    if (writing) {
      throw new IllegalStateException("Opened for writing");
    }
    if (fs instanceof ReadWriteFileSystem<?>) {
      final ReadWriteFileSystem<P> fs = (ReadWriteFileSystem<P>)this.fs;
      return new StringWriter() {
        @Override
        public void close() throws IOException {
          Content content = new Content(getBuffer());
          long lastModified = fs.setContent(file, content);
          JavaFileObjectImpl.this.content = new Timestamped<Content>(lastModified, content);
          JavaFileObjectImpl.this.writing = false;
        }
      };
    }
    else {
      throw new UnsupportedOperationException("Read only");
    }
  }
}
