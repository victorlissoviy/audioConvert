package audioconvert.implementations.converters;

import audioconvert.abstracts.Converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConverterM4A extends Converter {
  public ConverterM4A(String name, double quality, boolean removeOrig) {
    super(name, quality, removeOrig);
    format = "m4a";
  }

  @Override
  protected void toFormat() throws IOException {
    newName = "%s - %s.m4a".formatted(author, title);
    List<String> command = new ArrayList<>();
    command.add("neroAacEnc");
    if (quality > 1) {
      command.addAll(Arrays.asList("-2pass", "-br", String.valueOf(quality * 1000)));
    } else {
      command.add("-q");
      command.add(String.valueOf(quality));
    }
    command.addAll(Arrays.asList("-if", tmpName1, "-of", newName));
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command(command);
    Process process = processBuilder.start();
    if (wait(process)) {
      System.out.println(newName + " to m4a Error");
      Files.deleteIfExists(Path.of(newName));
    }

    command.clear();
    command.add("neroAacTag");
    command.add("-meta:title=" + title);
    if (author != null && !author.isEmpty()) {
      command.add("-meta:artist=" + author);
    }
    command.add(newName);
    processBuilder.command(command);
    process = processBuilder.start();
    if (wait(process)) {
      Files.deleteIfExists(Path.of(newName));
      throw new IOException(newName + "to m4a tag Error");
    }

    //знаходження ReplayGain
    command.clear();
    command.addAll(Arrays.asList("aacgain", "-q", "-e", "-s", "r"));
    command.add(newName);
    processBuilder.command(command);
    try {
      process = processBuilder.start();
      if (wait(process)) {
        throw new IOException();
      }
    } catch (IOException e) {
      System.out.println(newName + " aacgain Warning");
    }
  }


}
