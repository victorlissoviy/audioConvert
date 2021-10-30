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
    private final String path;
    private List<String> listFiles;

    public AudioConv(double q, String path){
        this.q = q;
        this.path = path;
        listFiles = new ArrayList<>();
        System.out.println("Якість: " + this.q);
        Pattern formats = Pattern.compile("(mp3|ogg|aac|mp4|m4a|wma|opus|oga|flac|wav|aiff|webm|matroska|webm|matroska|asf|amr|avi|mov|3gp)$");
        for(File f : Objects.requireNonNull(new File(this.path).listFiles())){
            if(formats.matcher(f.getName().toLowerCase()).find()){
                listFiles.add(f.getName());
            }
        }
    }

    public void work() throws IOException, InterruptedException {
        while(true){
            String name = null;
            synchronized (this){
                if(!listFiles.isEmpty()){
                    name = listFiles.remove(0);
                }else{
                    break;
                }
                System.out.println(name.substring(0, name.lastIndexOf(".")) + ".m4a");
            }
            if(name == null || name.isEmpty()){
                break;
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
            items = null;
            String name2 = path + "/." + name.substring(0, name.lastIndexOf(".")) + ".wav";
            String name3 = path + "/" + name.substring(0, name.lastIndexOf(".")) + ".m4a";
            name = path + "/" + name;
            List<String> command = new ArrayList<>();
            command.add("/usr/bin/ffmpeg");
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
                System.out.println("To wav Error");
                return;
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
                System.out.println("To m4a Error");
                return;
            }

            command.clear();
            command.add("rm");
            command.add(name2);
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println("rm Error");
                return;
            }

            command.clear();
            command.add("aacgain");
            command.add("-e");
            command.add(name3);
            process = new ProcessBuilder(command).start();
            if(process.waitFor() != 0){
                System.out.println("aacgain Error");
                return;
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
                System.out.println("neroAacTag Error");
                return;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int processors = Runtime.getRuntime().availableProcessors();
        double q = 0.75;
        AudioConv audioConv;
        try{
            for(int i = 0; i < args.length - 1; i++){
                if(args[i].equals("-q")){
                    q = Double.parseDouble(args[i + 1]);
                }
                if(args[i].equals("-c")){
                    processors = Integer.parseInt(args[i + 1]);
                }
            }
            audioConv = new AudioConv(q, new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("pwd").getInputStream())).readLine());
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