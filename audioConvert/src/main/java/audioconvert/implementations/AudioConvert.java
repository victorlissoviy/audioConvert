package audioconvert.implementations;

import audioconvert.abstracts.Converter;
import audioconvert.enums.Format;
import audioconvert.factories.ConverterFactory;
import audioconvert.implementations.filescanners.SimpleFileScanner;
import audioconvert.interfaces.FileScanner;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Class to convert audio
 */
public class AudioConvert {
  /**
   * File Scanner for find files.
   */
  private final FileScanner fileScanner;

  /**
   * Filter files.
   */
  private final String filter;

  private final List<String> listFiles;

  private final int countThreads;
  private int count = 0;
  private int n;

  ConverterFactory converterFactory;

  private final ExecutorService executorService;

  public AudioConvert(double quality,
                      Format format,
                      String filter,
                      int countThreads,
                      boolean removeOrig) {
    this.filter = filter;
    this.countThreads = countThreads;
    this.fileScanner = new SimpleFileScanner();
    this.listFiles = new ArrayList<>();
    executorService = Executors.newFixedThreadPool(countThreads);
    converterFactory = new ConverterFactory(quality, removeOrig, format);
  }

  private synchronized String getPath() {
    if (listFiles.isEmpty()) {
      return null;
    }
    return listFiles.remove(0);
  }

  private synchronized int getCount() {
    return ++count;
  }

  /**
   * Method to start work.
   */
  public void work() {
    listFiles.addAll(List.of(fileScanner.getListFiles(
            Paths.get("").toAbsolutePath().toString(),
            filter
    )));
    List<CompletableFuture<Void>> listThreads = new ArrayList<>();
    n = listFiles.size();
    for (int i = 0; i < countThreads; i++) {
      listThreads.add(worker());
    }
    CompletableFuture<Void> threadAll = CompletableFuture.allOf(listThreads.toArray(new CompletableFuture[0]));
    threadAll.join();
    executorService.shutdown();
  }

  /**
   * One worker.
   *
   * @return thread worker
   */
  private CompletableFuture<Void> worker() {
    return CompletableFuture.runAsync(() -> {
      while (true) {
        try {
          String name = getPath();
          if (name == null) {
            break;
          }
          int i = getCount();
          Converter converter = converterFactory.getConverter(name);
          if (converter == null) {
            continue;
          }
          System.out.printf("%d%% (%d/%d) %s%n", 100 * i / n, i, n, converter.getInfo());
          converter.convert();
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
    }, executorService);
  }


}
