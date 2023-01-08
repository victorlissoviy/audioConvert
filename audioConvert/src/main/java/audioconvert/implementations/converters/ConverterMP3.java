package audioconvert.implementations.converters;

import audioconvert.abstracts.Converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConverterMP3 extends Converter {

  public ConverterMP3(String name, double quality, boolean removeOrig) {
    super(name, 10 - quality * 10, removeOrig);
    format = "mp3";
  }

  @Override
  protected void toFormat() throws IOException {
    newName = "%s - %s.mp3".formatted(author, title);
    List<String> command = new ArrayList<>(Arrays.asList("lame", "-S", "--bitwidth", "32", "-o", "--buffer-constraint", "maximum", "--preset", "extreme", "-m", "j"));
    double Q = quality;
    if (Q > 320) {
      Q = 320;
    }
    if (Q <= 10) {
      command.addAll(Arrays.asList("-V", String.valueOf(quality), "-q", "0"));
    } else {
      command.addAll(Arrays.asList("-q", "0", "-b", String.valueOf(Q), "-B", String.valueOf(Q)));
    }
    command.addAll(Arrays.asList("--tt", title));
    if (author != null && !author.isEmpty()) {
      command.add("--ta");
      command.add(author);
    }
    command.add(tmpName1);
    command.add(newName);
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    Process process = processBuilder.start();
    if (wait(process)) {
      throw new IOException(String.format("%s to mp3 Error\ncommand: %s", newName, command));
    }

    command.clear();
    command.addAll(Arrays.asList("mp3gain", "-q", "-e", "-s", "r", "-s", "i"));
    command.add(newName);
    processBuilder.command(command);
    try {
      process = processBuilder.start();
      if (wait(process)) {
        throw new IOException();
      }
    } catch (IOException e) {
      System.out.println(newName + " mp3gain Warning");
    }
  }
}
