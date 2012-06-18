package juzu.impl.plugin.asset;

import juzu.impl.application.metamodel.ApplicationMetaModel;
import juzu.impl.application.metamodel.ApplicationMetaModelPlugin;
import juzu.impl.compiler.AnnotationData;
import juzu.impl.compiler.ElementHandle;
import juzu.impl.utils.JSON;
import juzu.plugin.asset.Assets;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class AssetMetaModelPlugin extends ApplicationMetaModelPlugin {

  /** . */
  private final HashMap<ElementHandle.Package, JSON> enabledMap = new HashMap<ElementHandle.Package, JSON>();

  public AssetMetaModelPlugin() {
    super("asset");
  }

  @Override
  public Set<Class<? extends Annotation>> getAnnotationTypes() {
    return Collections.<Class<? extends Annotation>>singleton(Assets.class);
  }

  @Override
  public void processAnnotation(ApplicationMetaModel application, Element element, String fqn, AnnotationData data) {
    if (fqn.equals(Assets.class.getName())) {
      ElementHandle.Package handle = application.getHandle();
      JSON json = new JSON();
      json.set("scripts", build((List<Map<String, Object>>)data.get("scripts")));
      json.set("stylesheets", build((List<Map<String, Object>>)data.get("stylesheets")));
      json.set("package", application.getFQN().getPackageName().append("assets").getValue());
      enabledMap.put(handle, json);
    }
  }

  private List<JSON> build(List<Map<String, Object>> scripts) {
    List<JSON> foo = Collections.emptyList();
    if (scripts != null && scripts.size() > 0) {
      foo = new ArrayList<JSON>(scripts.size());
      for (Map<String, Object> script : scripts) {
        JSON bar = new JSON();
        for (Map.Entry<String, Object> entry : script.entrySet()) {
          bar.set(entry.getKey(), entry.getValue());
        }
        foo.add(bar);
      }
    }
    return foo;
  }

  @Override
  public void postConstruct(ApplicationMetaModel application) {
  }

  @Override
  public void preDestroy(ApplicationMetaModel application) {
    enabledMap.remove(application.getHandle());
  }

  @Override
  public JSON getDescriptor(ApplicationMetaModel application) {
    ElementHandle.Package handle = application.getHandle();
    return enabledMap.get(handle);
  }
}