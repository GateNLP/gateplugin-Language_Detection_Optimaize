/*
 * LanguageIdentifier
 * 
 * Copyright (c) 1995-2011, The University of Sheffield.
 * 
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 3, June 2007
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 * 
 * $Id: LanguageIdentifier.java 17698 2014-03-19 09:09:28Z markagreenwood $
 */
package gate.optimaize;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.BuiltInLanguages;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Utils;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.ResourceReference;
import gate.creole.metadata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CreoleResource(name = "Optimaize Language Detection", comment = "Recognizes the document language using Optimaize", icon = "paw-print.png", helpURL = "http://gate.ac.uk/userguide/sec:misc-creole:language-identification")
public class LanguageIdentifier extends gate.creole.AbstractLanguageAnalyser {

  private static final long serialVersionUID = 5831213212185693826L;

  private static final Logger LOG = LoggerFactory.getLogger(LanguageIdentifier.class);

  private LanguageDetector detector;

  private TextType textType;

  private Boolean loadBuiltInProfiles;

  private List<String> builtInLanguages;

  private List<ResourceReference> extraProfiles;

  private String languageFeatureName;

  private String unknownValue;

  private String annotationType;

  private String annotationSetName;

  public LanguageIdentifier init() throws ResourceInstantiationException {
    if(detector == null) {
      if(textType == null) {
        throw new ResourceInstantiationException("textType must be set");
      }
      try {
        List<LanguageProfile> languageProfiles = new ArrayList<>();
        if(loadBuiltInProfiles == null || loadBuiltInProfiles) {
          List<String> languages = builtInLanguages;
          if(languages == null || languages.isEmpty()) {
            languages = BuiltInLanguages.getLanguages().stream().map(LdLocale::toString).collect(Collectors.toList());
          }
          for(String language: languages) {
            languageProfiles.add(textType.loadProfile(language));
          }
        }
        if(extraProfiles != null) {
          LanguageProfileReader reader = new LanguageProfileReader();
          for(ResourceReference res : extraProfiles) {
            try(InputStream in = res.openStream()) {
              languageProfiles.add(reader.read(in));
            }
          }
        }

        LOG.info("Creating language detector with {} profiles, optimized for {} text", languageProfiles.size(), textType);
        detector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles).build();
      } catch(IOException e) {
        throw new ResourceInstantiationException("Unable to load language profiles", e);
      }
    }

    return this;
  }

  /**
   * Based on the document content, recognizes the language and adds a document
   * feature.
   */
  public void execute() throws ExecutionException {
    if(document == null || document.getFeatures() == null) return;

    if(annotationType == null || annotationType.trim().equals("")) {
      /*
       * Default situation: classify the whole document and save the result as a
       * document feature.
       */
      String text = document.getContent().toString();
      com.google.common.base.Optional<LdLocale> detected = detector.detect(
              textType.textObjectFactory().forText(text));
      if(detected.isPresent()) {
        document.getFeatures().put(languageFeatureName, detected.get().getLanguage());
      } else if(unknownValue != null && !"".equals(unknownValue)) {
        document.getFeatures().put(languageFeatureName, unknownValue);
      }
    }

    else {
      /*
       * New option: classify the text underlying each annotation (specified by
       * AS and type) and save the result as an annotation feature.
       */
      AnnotationSet annotations =
              document.getAnnotations(annotationSetName).get(annotationType);
      for(Annotation annotation : annotations) {
        String text = Utils.stringFor(document, annotation);
        com.google.common.base.Optional<LdLocale> detected = detector.detect(
                textType.textObjectFactory().forText(text));
        if(detected.isPresent()) {
          annotation.getFeatures().put(languageFeatureName, detected.get().getLanguage());
        } else if(unknownValue != null && !"".equals(unknownValue)) {
          annotation.getFeatures().put(languageFeatureName, unknownValue);
        }
      }
    }

  }

  public void reInit() throws ResourceInstantiationException {
    detector = null;
    init();
  }

  @CreoleParameter(comment = "Load models optimized for this type of text", defaultValue = "LONG")
  public void setTextType(TextType textType) {
    this.textType = textType;
  }

  public TextType getTextType() {
    return textType;
  }

  @CreoleParameter(comment = "Should any built-in profiles be loaded?  If true, builtInLanguages specifies which ones.", defaultValue = "true")
  public void setLoadBuiltInProfiles(Boolean loadBuiltInProfiles) {
    this.loadBuiltInProfiles = loadBuiltInProfiles;
  }

  public Boolean getLoadBuiltInProfiles() {
    return loadBuiltInProfiles;
  }

  @Optional
  @CreoleParameter(comment = "Which built in language profiles should be loaded?  If unspecified or empty, all the built in languages will be used - if you do not want to use the built in profiles at all, set loadBuiltInProfiles=false")
  public void setBuiltInLanguages(List<String> builtInLanguages) {
    this.builtInLanguages = builtInLanguages;
  }

  public List<String> getBuiltInLanguages() {
    return builtInLanguages;
  }

  @Optional
  @CreoleParameter(comment = "Additional language profile files to load, either in addition to (if loadBuiltInProfiles=true) or instead of (if false) the default ones.")
  public void setExtraProfiles(List<ResourceReference> extraProfiles) {
    this.extraProfiles = extraProfiles;
  }

  public List<ResourceReference> getExtraProfiles() {
    return extraProfiles;
  }

  @RunTime
  @Optional
  @CreoleParameter(comment = "Value set for the language feature when the detector cannot determine the language.  " +
          "If unspecified, the feature will be left unset when the detector is unsure.")
  public void setUnknownValue(String unknownValue) {
    this.unknownValue = unknownValue;
  }

  public String getUnknownValue() {
    return unknownValue;
  }

  @RunTime
  @Optional
  @CreoleParameter(comment = "Name of document or annotation feature for the language identified", defaultValue = "lang")
  public void setLanguageFeatureName(String languageFeatureName) {
    this.languageFeatureName = languageFeatureName;
  }

  public String getLanguageFeatureName() {
    return languageFeatureName;
  }

  @RunTime
  @Optional
  @CreoleParameter(comment = "Type of annotations to classify; leave blank for whole-document classification")
  public void setAnnotationType(String atype) {
    this.annotationType = atype;
  }

  public String getAnnotationType() {
    return this.annotationType;
  }

  @RunTime
  @Optional
  @CreoleParameter(comment = "Annotation set used for input/output (ignored for whole-document classification)")
  public void setAnnotationSetName(String inputASName) {
    this.annotationSetName = inputASName;
  }

  public String getAnnotationSetName() {
    return annotationSetName;
  }


  /**
   * Not for public use - exists only for use by Factory.duplicate.
   */
  public LanguageDetector getDetector() {
    return detector;
  }

  /**
   * Not for public use - exists only for use by Factory.duplicate.
   */
  @Sharable
  public void setDetector(LanguageDetector detector) {
    this.detector = detector;
  }
}
