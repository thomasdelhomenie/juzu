package org.juzu.impl.spi.inject.declared.qualifier.declared.provider;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class DeclaredQualifierDeclaredProvider
{

   /** .*/
   private final String id = "" + Math.random();

   public String getId()
   {
      return id;
   }

   public static class Green extends DeclaredQualifierDeclaredProvider
   {
   }
}
