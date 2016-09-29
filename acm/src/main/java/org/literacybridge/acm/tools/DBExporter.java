package org.literacybridge.acm.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.importexport.CSVExporter;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.tbbuilder.TBBuilder;

import au.com.bytecode.opencsv.CSVWriter;

public class DBExporter {
  private String project;
  private String filenamePrefix;
  private File exportDirectory;

  /**
   * Creates and initializes an exporter.
   * @param dbName The ACM directory, with the ACM- part; 'ACM-EXAMPLE'.
   * @param exportDir A directory into which to export the metadata.
   * @throws Exception
   */
  public DBExporter(String dbName, File exportDir) throws Exception {
    this(dbName, exportDir, "");
  }

  private DBExporter(String dbName, File exportDir, String prefix) throws Exception {
    exportDirectory = exportDir;
    project = dbName.substring(TBBuilder.ACM_PREFIX.length());
    filenamePrefix = prefix;

    ACMConfiguration.getInstance().setCurrentDB(dbName, false);
  }

  public void export() {
    categoriesExporter();
    languagesExporter();
    metadataExporter();
  }

  private void categoriesExporter() {
    String[] header = { "ID", "Name", "Project" };
    File exportFile = new File(exportDirectory, filenamePrefix + "categories.csv");
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(exportFile), ',');
      writer.writeNext(header);
      Category leaf = ACMConfiguration.getInstance().getCurrentDB()
          .getMetadataStore().getTaxonomy().getRootCategory();
      getChildren(writer, leaf);
      writer.close();
    } catch (IOException e) {
      System.out.println("==========>Could not export categories!");
      e.printStackTrace();
    }
  }

  private void languagesExporter() {
    String[] header = { "ID", "Name", "Project" };
    File exportFile = new File(exportDirectory, filenamePrefix + "languages.csv");
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(exportFile), ',');
      // String[] header = {"ID","Name","Project"};
      writer.writeNext(header);
      List<Locale> languages = ACMConfiguration.getInstance().getCurrentDB()
          .getAudioLanguages();
      for (Locale l : languages) {
        String languageCode = l.getLanguage();
        String languageLabel = LanguageUtil.getLocalizedLanguageName(l);
        String[] values = { languageCode, languageLabel, project };
        writer.writeNext(values);
      }
      writer.close();
    } catch (IOException e) {
      System.out.println("==========>Could not export languages!");
      e.printStackTrace();
    }
  }

  private void metadataExporter() {
    File exportFile = new File(exportDirectory, filenamePrefix + "metadata.csv");
    try {
      CSVExporter.export(ACMConfiguration.getInstance().getCurrentDB()
          .getMetadataStore().getAudioItems(), exportFile);
    } catch (IOException e) {
      System.out.println("==========>Could not export metadata!");
      e.printStackTrace();
    }
  }

  private void getChildren(CSVWriter writer, Category cat) {
    for (Category child : cat.getSortedChildren()) {
      String[] values = new String[3];
      values[0] = child.getUuid();
      values[1] = child.getCategoryName().toString();
      values[2] = project;
      writer.writeNext(values);
      if (child.hasChildren()) {
        getChildren(writer, child);
      }
    }
  }

  private static void printUsage() {
    System.out.println(
        "Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tools.DBExporter <export directory> <acm_name>+");
  }

  public static void main(String[] args) throws Exception {
    int argCount = args.length;
    if (argCount < 2) {
      printUsage();
      System.exit(1);
    }
    File exportDir = new File(args[0]);
    if (!exportDir.isDirectory()) {
      throw new Exception(
          "Export directory doesn't exist.\n" + exportDir.getAbsolutePath());
    }

    CommandLineParams params = new CommandLineParams();
    params.disableUI = true;
    params.sandbox = true;
    ACMConfiguration.initialize(params);

    for (int i = 1; i < argCount; i++) {
      String prefix = args[i].substring(TBBuilder.ACM_PREFIX.length()) + "-";
      DBExporter exporter = new DBExporter(args[i], exportDir, prefix);
      exporter.export();
    }
  }
}
