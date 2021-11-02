import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class AudioConv {
    private final double q;
    private final boolean removeOrig;
    private final List<String> listFiles;
    private final int n;
    private int i = 0;

    private void removeFile(String file){
        try {
            Process process = new ProcessBuilder("rm", "-f", file).start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean wait(Process process) throws IOException, InterruptedException {
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String stdin = in.readLine();
        while (stdin != null){
            stdin = in.readLine();
        }
        in.close();
        return process.waitFor() != 0;
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
            command.addAll(new ArrayList<>(Arrays.asList("-i", name, "-acodec", "pcm_f32le", name2)));
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            Process process = processBuilder.start();
            if(wait(process)){
                System.out.println(name3 + " to wav Error");
                removeFile(name2);
                continue;
            }

            command.clear();
            command.addAll(new ArrayList<>(Arrays.asList("sox", name2, "-e", "floating-point", name4, "rate", "-v", "-I", "-a", "-b", "99.7", "-p", "100", "48000")));
            processBuilder.command(command);
            process = processBuilder.start();
            if(wait(process)){
                System.out.println(name3 + " sox Error");
                removeFile(name2);
                removeFile(name4);
                continue;
            }

            command.clear();
            command.addAll(new ArrayList<>(Arrays.asList("mv", name4, name2)));
            processBuilder.command(command);
            process = processBuilder.start();
            if(wait(process)){
                System.out.println(name3 + " mv Error");
                removeFile(name2);
                removeFile(name4);
                continue;
            }

            command.clear();
            command.add("neroAacEnc");
            if(q > 1){
                command.addAll(new ArrayList<>(Arrays.asList("-2pass", "-br", String.valueOf(q * 1000))));
            }else{
                command.add("-q");
                command.add(String.valueOf(q));
            }
            command.addAll(new ArrayList<>(Arrays.asList("-if", name2, "-of", name3)));
            processBuilder.command(command);
            process = processBuilder.start();
            if(wait(process)){
                System.out.println(name3 + " to m4a Error");
                removeFile(name2);
                removeFile(name3);
                continue;
            }

            command.clear();
            command.add("rm");
            command.add(name2);
            processBuilder.command(command);
            process = processBuilder.start();
            if(wait(process)){
                System.out.println(name2 + " rm Error");
                removeFile(name2);
                continue;
            }

            command.clear();
            command.addAll(new ArrayList<>(Arrays.asList("aacgain", "-e", name3)));
            processBuilder.command(command);
            process = processBuilder.start();
            if(wait(process)){
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
            processBuilder.command(command);
            process = processBuilder.start();
            if(wait(process)){
                System.out.println(name3 + " neroAacTag Error");
                removeFile(name3);
                continue;
            }

            if(removeOrig){
                processBuilder.command("rm", "-f", name);
                process = processBuilder.start();
                if(wait(process)){
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
                    System.out.println("Вихід");
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