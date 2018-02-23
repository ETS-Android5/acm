package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.utils.IOUtils;

public class FileSystemExporter {
  public static final String FILENAME_SEPARATOR = "___";

  public static void export(Iterable<AudioItem> audioItems, File targetDirectory,
      AudioFormat targetFormat, boolean titleInFilename, boolean idInFilename)
      throws IOException {

    try {
      AudioItemRepository repository = ACMConfiguration.getInstance()
          .getCurrentDB().getRepository();

      for (AudioItem audioItem : audioItems) {
        // first: check which formats we have
        File sourceFile = repository.convert(audioItem, targetFormat);

        if (sourceFile != null) {
          String title = (titleInFilename
              ? audioItem.getMetadata()
                  .getMetadataValue(MetadataSpecification.DC_TITLE).getValue()
              : "")
              + (idInFilename && titleInFilename ? FILENAME_SEPARATOR : "")
              + (idInFilename ? audioItem.getMetadata()
                  .getMetadataValue(MetadataSpecification.DC_IDENTIFIER)
                  .getValue() : "");

          // replace invalid file name characters (windows) with an underscore
          // ('_')
          title = title.trim().replaceAll("[\\\\/:*?\"<>|']", "_");
          File targetFile;
          int counter = 0;
          do {
            if (counter == 0) {
              targetFile = new File(targetDirectory,
                  title + "." + targetFormat.getFileExtension());
            } else {
              targetFile = new File(targetDirectory, title + "-" + counter + "."
                  + targetFormat.getFileExtension());
            }
            counter++;
          } while (targetFile.exists());
          if (targetFormat == AudioFormat.A18) {
            repository.exportA18WithMetadataToFile(audioItem, targetFile);
          } else {
            IOUtils.copy(sourceFile, targetFile);
          }
        }
      }
    } catch (ConversionException e) {
      throw new IOException(e);
    }
  }
}
