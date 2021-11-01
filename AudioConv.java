import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class AudioConv {
    private final double q;
    private final boolean removeOrig;
    private final List<String> listFiles;
    private int n;
    private int i = 0;

    private void removeFile(String file){
        try {
            Process process = new ProcessBuilder("rm", "-f", file).start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public AudioConv(double q, String path, boolean rf){
        this.q = q;
        removeOrig = rf;
        listFiles = new ArrayList<>();
        System.out.println("Якість: " + this.q);
        Pattern formats = Pattern.compile("(mp3|ogg|aac|mp4|m4a|wma|opus|oga|flac|wav|aiff|webm|matroska|asf|amr|avi|mov|3gp)$");
        for(File f : Objects.requireNonNull(new File(path).listFiles())){
            if(formats.matcher(f.getName().toLowerCase()).find()){
                listFiles.add(f.getName());
            }
        }
        n = listFiles.size();
    }

    public void work() throws IOException, InterruptedException {
        while(true){
            String name;
            String subName;
            synchronized (this){
                if(!listFiles.isEmpty()){
                    name = listFiles.remove(0);
                }else{
                    break;
                }
                subName = name.substring(0, name.lastIndexOf("."));
                i += 1;
                System.out.println((100 * i / n)  + "% (" + i + "/" + n + ") " + subName + ".m4a");
            }
            if(name.isEmpty()){
                continue;
            }
            String[] items = name.substring(0,name.lastIndexOf(".")).split(" - ");
            String title;
            String author = null;
            if(items.length == 2){
                author = items[0];
                title = items[1];
            }else{
                title = items[0];
            }
            String name2 = "." + subName + ".wav";
            String name4 = ".2" + subName + ".wav";
            String name3 = subName + ".m4a";
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-y");
            if(name.endsWith("mp3")){
                command.add("-acodec");
                command.add("mp3float");
            }
            command.add("-i");
            command.add(name);
            command.add("-acodec");
            command.add("pcm_f32le");
            command.add(name2);
            Process process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println(name3 + " to wav Error");
                removeFile(name2);
                continue;
            }

            command.clear();
            command.add("sox");
            command.add(name2);
            command.add("-e");
            command.add("floating-point");
            command.add(name4);
            command.add("rate");
            command.add("-v");
            command.add("-I");
            command.add("-a");
            command.add("-b");
            command.add("99.7");
            command.add("-p");
            command.add("100");
            command.add("48000");
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println(name3 + " sox Error");
                removeFile(name2);
                removeFile(name4);
                continue;
            }

            command.clear();
            command.add("mv");
            command.add(name4);
            command.add(name2);
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println(name3 + " mv Error");
                removeFile(name2);
                removeFile(name4);
                continue;
            }

            command.clear();
            command.add("neroAacEnc");
            if(q > 1){
                command.add("-2pass");
                command.add("-br");
                command.add(String.valueOf(q * 1000));
            }else{
                command.add("-q");
                command.add(String.valueOf(q));
            }
            command.add("-ignorelength");
            command.add("-if");
            command.add(name2);
            command.add("-of");
            command.add(name3);
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println(name3 + " to m4a Error");
                removeFile(name2);
                removeFile(name3);
                continue;
            }

            command.clear();
            command.add("rm");
            command.add(name2);
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println(name2 + " rm Error");
                removeFile(name2);
                continue;
            }

            command.clear();
            command.add("aacgain");
            command.add("-e");
            command.add(name3);
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println(name3 + " aacgain Error");
                removeFile(name3);
                continue;
            }

            command.clear();
            command.add("neroAacTag");
            command.add("-meta:title=" + title);
            if(!(author == null || author.isEmpty())){
                command.add("-meta:artist=" + author);
            }
            command.add(name3);
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println(name3 + " neroAacTag Error");
                removeFile(name3);
                continue;
            }

            if(removeOrig){
                command.clear();
                command.add("rm");
                command.add("-f");
                command.add(name);
                process = new ProcessBuilder(command).start();
                if(process.waitFor() != 0){
                    System.out.println(name + " remove orig Error");
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int processors = Runtime.getRuntime().availableProcessors();
        double q = 0.75;
        boolean removeFile = false;
        AudioConv audioConv;
        try{
            for(int i = 0; i < args.length; i++){
                if(args[i].equals("-q") && args[i + 1] != null){
                    q = Double.parseDouble(args[i + 1]);
                }
                if(args[i].equals("-c") && args[i + 1] != null){
                    processors = Integer.parseInt(args[i + 1]);
                }
                if(args[i].equals("-r")){
                    removeFile = true;
                }
            }
            audioConv = new AudioConv(q, new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("pwd").getInputStream())).readLine(), removeFile);
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        Thread[] workers = new Thread[processors];
        for(int i = 0; i < processors; i++){
            workers[i] = new Thread(() -> {
                try {
                    audioConv.work();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        for(Thread worker : workers){
            worker.start();
        }
        for(Thread worker : workers){
            worker.join();
        }
    }
}