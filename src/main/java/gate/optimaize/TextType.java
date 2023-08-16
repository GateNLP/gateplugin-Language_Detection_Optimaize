package gate.optimaize;

import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.profiles.BuiltInLanguages;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObjectFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum TextType {
  /**
   * "Long" texts - uses the default "large text" version of TextObjectFactory, and the long text models.
   */
  LONG {
    private final TextObjectFactory factory = CommonTextObjectFactories.forDetectingOnLargeText();

    @Override
    public LanguageProfile loadProfile(String locale) throws IOException {
      return reader.readBuiltIn(LdLocale.fromString(locale));
    }

    @Override
    public TextObjectFactory textObjectFactory() {
      return factory;
    }
  },

  /**
   * "Short" texts - uses the "short clean text" version of TextObjectFactory that does not remove minority
   * scripts or strip out URLs, and prefers the "short text" version of the built-in models where available.
   */
  SHORT {
    // map from locale name to directory within the optimaize JAR file where the model file can be found.
    // The idea here is that we prefer the shorttext model if there is one, but load the regular model for
    // languages that don't have a short one.
    private final Map<String, String> modelDir = new HashMap<>();
    {
      for(String lang : BuiltInLanguages.getShortTextLanguages()) {
        modelDir.put(lang, "languages.shorttext");
      }
      for(LdLocale locale : BuiltInLanguages.getLanguages()) {
        modelDir.putIfAbsent(locale.toString(), "languages");
      }
    }

    private final TextObjectFactory factory = CommonTextObjectFactories.forDetectingShortCleanText();

    @Override
    public LanguageProfile loadProfile(String locale) throws IOException {
      return reader.read(modelDir.get(locale), Collections.singletonList(locale)).get(0);
    }

    @Override
    public TextObjectFactory textObjectFactory() {
      return factory;
    }
  };

  private static final LanguageProfileReader reader = new LanguageProfileReader();

  public abstract LanguageProfile loadProfile(String locale) throws IOException;

  public abstract TextObjectFactory textObjectFactory();
}
