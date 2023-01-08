package main;

import audioconvert.enums.Format;
import audioconvert.implementations.AudioConvert;

/**
 * Main class in project
 */
public final class Main {

  private Main() {
  }

  public static void main(String[] args) {
    int processors = Runtime.getRuntime().availableProcessors();
    double q = 0.75;
    Format format = Format.M4A;
    boolean removeOrig = false;
    try {
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-q") && args[i + 1] != null) {
          q = Double.parseDouble(args[i + 1]);
        }
        if (args[i].equals("-f") && args[i + 1] != null) {
          if (args[i + 1].equals("mp3")) {
            format = Format.MP3;
          }
        }
        if (args[i].equals("-c") && args[i + 1] != null) {
          processors = Integer.parseInt(args[i + 1]);
        }
        if (args[i].equals("-r")) {
          removeOrig = true;
        }
        if (args[i].equals("-h")) {
          System.out.println("""
                  AudioConvert
                  Для роботи програми потрібно щоб було встановлено наступні програми:
                  ffmpeg neroAacEnc aacgain neroAacTag sox oggenc vorbisgain lame mp3gain

                  Параметри:
                  \t-q <num> - Якість (0, 1)
                  \t-f (mp3, m4a) - формат
                  \t-c <num> - кількість потоків
                  \t-r - видалення оригінального файлу
                  """);
          return;
        }
      }
      StringBuilder statusLine = new StringBuilder();
      String standartLine = String.format("Формат: %s%nЯкість: %s%nКількість потоків: %s", format, q, processors);
      statusLine.append(standartLine);
      if(removeOrig){
        statusLine.append("\nВидалення оригінальних файлів");
      }
      System.out.println(statusLine);
      AudioConvert audioConvert = new AudioConvert(
              q,
              format,
              "\\.(mp3|ogg|aac|mp4|m4a|wma|opus|oga|flac|wav|aiff|webm|matroska|asf|amr|avi|mov|3gp)$",
              processors,
              removeOrig
      );
      audioConvert.work();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
