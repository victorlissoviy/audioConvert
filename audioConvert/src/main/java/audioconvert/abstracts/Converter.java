package audioconvert.abstracts;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Converter {
  protected static Logger logger = Logger.getLogger("Converter");
  protected double quality;
  private final String name;
  protected final String subName;
  private boolean removeOrig;
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

  private void smartQuality() {
    String nameLowerCase = name.toLowerCase();
    if (nameLowerCase.endsWith(".mp3")) {
      getInfoFromMp3();
    } else if (checkStringAccordanceRegex(nameLowerCase, "\\.(ogg|oga)$")) {
      getInfoFromOgg();
    } else if (checkStringAccordanceRegex(nameLowerCase, "\\.(flac|wav|wave)$")) {
      this.quality = 1.0;
      this.removeOrig = false;
    }
  }

  private Boolean checkStringAccordanceRegex(String line, String regex) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(line);
    return matcher.find();
  }

  private void getInfoFromOgg() {
    ProcessBuilder processBuilder = new ProcessBuilder("ogginfo", name);
    try {
      Process process = processBuilder.start();
      try (InputStreamReader is = new InputStreamReader(process.getInputStream());
           BufferedReader br = new BufferedReader(is)) {
        String line;
        while ((line = br.readLine()) != null) {
          getBitrateFromOgg(line);
          title = getTagFromOgg(line, "title", title);
          author = getTagFromOgg(line, "artist", author);
        }
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage());
    }
  }

  private void getBitrateFromOgg(String line) {
    String splitLine = "bitrate:";
    int indexBitrate = line.indexOf(splitLine);
    if (indexBitrate != -1) {
      String[] elements = line.split(splitLine);
      String[] bitrateLine = elements[1].trim().split(" ");
      String bitrateString = bitrateLine[0].replace(",", ".");
      double bitrate = Double.parseDouble(bitrateString);
      quality = bitrate * 0.9;
    }
  }

  private String getTagFromOgg(String line, String tag, String alternative) {
    String lineLowerCase = line.toLowerCase();
    String splitLine = tag + "=";
    int indexTitle = lineLowerCase.indexOf(splitLine);
    if (indexTitle != -1) {
      String[] elements = line.split("=");
      return elements[1].trim().replace("_", " ");
    }
    return alternative;
  }

  private void getInfoFromMp3() {
    try {
      Mp3File file = new Mp3File(name);

      int bitrate = file.getBitrate();
      if (bitrate <= 128) {
        quality = bitrate;
      } else {
        quality = bitrate / 320.0 * 0.75;
      }

      if (file.hasId3v2Tag()) {
        ID3v2 id3v2Tag = file.getId3v2Tag();
        author = getMp3Tag(id3v2Tag.getArtist(), author);
        title = getMp3Tag(id3v2Tag.getTitle(), title);
      }
    } catch (InvalidDataException | UnsupportedTagException | IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private static String getMp3Tag(String tag, String alternative) throws UnsupportedEncodingException {
    if (tag != null && !tag.isEmpty()) {
      if (tag.trim().endsWith("??")) {
        byte[] tagBytes = tag.getBytes("windows-1252");
        String result = new String(tagBytes, "windows-1251");
        if(result.trim().endsWith("??")) {
          return alternative;
        }
        return result.replace("_", " ").trim();
      }
    }
    return alternative;
  }

  protected boolean wait(Process process) {
    try (InputStreamReader isr = new InputStreamReader(process.getErrorStream());
         BufferedReader in = new BufferedReader(isr)) {
      while (in.readLine() != null) {
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
