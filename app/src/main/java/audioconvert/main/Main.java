package audioconvert.main;

import audioconvert.enums.Format;
import audioconvert.implementations.AudioConv;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Main class in project
 */
public final class Main {

  private Main() {}
  public static void main(String[] args) throws InterruptedException {
    int processors = Runtime.getRuntime().availableProcessors();
    double q = 0.75;
    Format format = Format.M4A;
    boolean replayGain = false;
    boolean removeFile = false;
    AudioConv audioConv;
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
          removeFile = true;
        }
        if (args[i].equals("-g")) {
          replayGain = true;
        }
        if(args[i].equals("-h")){
          System.out.println("""
                            AudioConvert
                            Для роботи програми потрібно щоб було встановлено наступні програми:
                            ffmpeg neroAacEnc aacgain neroAacTag sox oggenc vorbisgain lame mp3gain

                            Параметри:
                            \t-q <num> - Якість
                            \t-f (mp3,m4a) - формат
                            \t-c - кількість потоків
                            \t-r - видалення оригінального файлу
                            \t-g - встановлення гучності вихідного файлу по ReplayGain""");
          return;
        }
      }
      audioConv = new AudioConv(q,
              new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("pwd").getInputStream())).readLine(),
              format,
              replayGain,
              removeFile);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }
    Thread[] workers = new Thread[processors];
    for (int i = 0; i < processors; i++) {
      workers[i] = new Thread(audioConv::work);
    }
    for (Thread worker : workers) {
      worker.start();
    }
    for (Thread worker : workers) {
      worker.join();
    }
  }
}
