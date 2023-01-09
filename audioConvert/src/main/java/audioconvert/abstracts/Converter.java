package audioconvert.abstracts;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public abstract class Converter {
  protected static Logger logger = Logger.getLogger("Converter");
  protected double quality;
  private final String name;
  protected final String subName;
  private final boolean removeOrig;
  protected String author;
  protected String title;
  protected final String tmpName1;
  protected String newName;
  protected final String tmpName2;
  protected static String format;

  public Converter(String name, double quality, boolean removeOrig) {
    this.name = name;

    int endPointIndex = name.lastIndexOf(".");
    subName = name.substring(0, endPointIndex);

    String nameReplacedSpaces = subName.replace("_", " ");
    String[] items = nameReplacedSpaces.split(" - ");
    if (items.length == 2) {
      author = items[0];
      title = items[1];
    } else {
      author = null;
      title = items[0];
    }

    this.quality = quality;
    this.removeOrig = removeOrig;

    tmpName1 = "/tmp/." + subName + ".wav";
    tmpName2 = "/tmp/.2" + subName + ".wav";
  }

  protected boolean wait(Process process) {
    try (InputStreamReader isr = new InputStreamReader(process.getErrorStream());
         BufferedReader in = new BufferedReader(isr)) {
      String line = in.readLine();

      while (line != null) {
        line = in.readLine();
      }

      return process.waitFor() != 0;
    } catch (IOException | InterruptedException e) {
      logger.warning(e.getMessage());
    }
    return true;
  }

  protected void toWaw() throws Exception {
    List<String> command = new ArrayList<>();
    command.add("ffmpeg");
    command.add("-y");
    if (name.endsWith("mp3")) {
      command.add("-acodec");
      command.add("mp3float");
    }
    command.addAll(Arrays.asList("-i", name, "-acodec", "pcm_f32le", tmpName1));
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command(command);
    Process process = processBuilder.start();
    Path pathName2 = Path.of(tmpName1);
    if (wait(process)) {
      Files.deleteIfExists(pathName2);
      logger.warning(String.format("%s\nto waw Error\n%s", name, command));
    }

    File wavFile = new File(tmpName1);
    AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(wavFile);
    AudioFormat audioFormat = audioFileFormat.getFormat();
    if ((int) audioFormat.getFrameRate() != 48000) {
      //Ресамплінг до 48000
      processBuilder.command("sox", tmpName1, "-e", "floating-point", tmpName2, "rate", "-v", "-I", "-a", "-b", "99.7", "-p", "100", "48000");
      process = processBuilder.start();
      Path pathName4 = Path.of(tmpName2);
      if (wait(process)) {
        Files.deleteIfExists(pathName2);
        Files.deleteIfExists(pathName4);
        throw new Exception(name + "resample Error");
      }

      processBuilder.command("mv", tmpName2, tmpName1);
      process = processBuilder.start();
      if (wait(process)) {
        Files.deleteIfExists(pathName2);
        Files.deleteIfExists(pathName4);
        logger.warning(name + " mv Error");
      }
    }
  }

  public void convert() throws Exception {
    toWaw();
    toFormat();
    Files.deleteIfExists(Path.of(tmpName1));
    if (removeOrig) {
      Files.deleteIfExists(Path.of(name));
    }
  }

  public String getInfo() {
    return "%s -> %s".formatted(name, newName);
  }

  protected abstract void toFormat() throws IOException;
}
