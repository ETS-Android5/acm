package org.literacybridge.acm.audioconverter.converters;

import org.literacybridge.acm.utils.OsUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class AnyToA18Converter extends BaseAudioConverter {

  public static final String USE_HEADER = "useHeader";
  public static final String ALGORITHM = "algorithm";

  public AnyToA18Converter() {
    super(".a18");
  }

  private FFMpegConverter ffmpegConverter = new FFMpegConverter();
  private WavToA18Converter wavToA18Converter = new WavToA18Converter();

  @Override
  public ConversionResult doConvertFile(File audioFile, File targetDir,
      File targetFile, File tmpDir, Map<String, String> parameters)
      throws ConversionException {

    if (!OsUtils.WINDOWS) {
      throw new ConversionException("A18 conversion is only supported on Windows.");
    }

    File tmp = null;
    try {
      tmp = File.createTempFile(audioFile.getName() + "_tmp_", ".wav", tmpDir);
      ConversionResult r1 = ffmpegConverter.doConvertFile(audioFile, tmpDir,
          tmp, tmpDir, parameters);
      tmp = r1.outputFile;
      ConversionResult r2 = wavToA18Converter.doConvertFile(tmp, targetDir,
          targetFile, tmpDir, parameters);

      r2.response = r1.response + "\n" + r2.response;
      return r2;
    } catch (IOException e) {
      throw new ConversionException(
          "Converter: Internal error while converting file: '"
              + audioFile.getName() + "'");
    } finally {
      if (tmp != null) {
        tmp.delete();
      }
    }
  }

  @Override
  public String getShortDescription() {
    return "Convert any audio file to .a18";
  }

  @Override
  public Set<String> getSourceFileExtensions() {
    return this.ffmpegConverter.getSourceFileExtensions();
  }

  @Override
  public void validateConverter() throws AudioConverterInitializationException {
    this.ffmpegConverter.validateConverter();
    this.wavToA18Converter.validateConverter();
  }

}
