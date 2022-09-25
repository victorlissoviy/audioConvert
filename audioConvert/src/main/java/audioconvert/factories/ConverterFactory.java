package audioconvert.factories;

import audioconvert.abstracts.Converter;
import audioconvert.enums.Format;
import audioconvert.implementations.converters.ConverterM4A;
import audioconvert.implementations.converters.ConverterMP3;

public class ConverterFactory {
  private double quality;
  private boolean removeOriginalFile;
  private Format format;

  public void setQuality(double quality) {
    this.quality = quality;
  }

  public void setRemoveOriginalFile(boolean removeOriginalFile) {
    this.removeOriginalFile = removeOriginalFile;
  }

  public void setFormat(Format format) {
    this.format = format;
  }

  public ConverterFactory(double quality, boolean removeOriginalFile, Format format) {
    setFormat(format);
    setQuality(quality);
    setRemoveOriginalFile(removeOriginalFile);
  }

  public Converter getConverter(String name) {
    switch (format) {
      case M4A -> {
        return new ConverterM4A(name, quality, removeOriginalFile);
      }
      case MP3 -> {
        return new ConverterMP3(name, quality, removeOriginalFile);
      }
    }
    return null;
  }
}
