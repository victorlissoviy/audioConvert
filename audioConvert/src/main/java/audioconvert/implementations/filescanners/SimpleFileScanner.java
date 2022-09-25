package audioconvert.implementations.filescanners;

import audioconvert.interfaces.FileScanner;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleFileScanner implements FileScanner {
  @Override
  public String[] getListFiles() {
    return getListFiles(Paths.get("").toAbsolutePath().toString());
  }

  @Override
  public String[] getListFiles(String path) {
    return getListFiles(path, "");
  }

  @Override
  public String[] getListFiles(String path, String filter) {
    File[] massiveFiles =  new File(path).listFiles();
    if(massiveFiles == null) {
      return null;
    }
    if(filter.isEmpty()) {
      String[] massivePaths = new String[massiveFiles.length];
      for (int i = 0; i < massiveFiles.length; i++) {
        massivePaths[i] = massiveFiles[i].getName();
      }
      return massivePaths;
    }else{
      List<String> listPaths = new ArrayList<>();
      Pattern pattern = Pattern.compile(filter);
      for(File f : massiveFiles) {
        Matcher matcher = pattern.matcher(f.getName());
        if(matcher.find()){
          listPaths.add(f.getName());
        }
      }
      return listPaths.toArray(new String[0]);
    }
  }
}
