package audioconvert.interfaces;

/**
 * Interface for scanner files.
 */
public interface FileScanner {
  /**
   * Get list files in directory.
   *
   * @return massive list files
   */
  String[] getListFiles();

  /**
   * Get list files from custom directory.
   *
   * @param path path to directory
   * @return massive list files
   */
  String[] getListFiles(String path);

  /**
   * Get list files from custom director using filter.
   *
   * @param path path to directory
   * @param filter string filter
   * @return massive list files
   */
  String[] getListFiles(String path, String filter);
}
