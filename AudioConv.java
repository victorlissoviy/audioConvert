import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

enum Format {
    M4A,
    MP3
}

public class AudioConv {
    private final double q;
    private final boolean removeOrig;
    private final List<String> listFiles;
    private final Format format;
    private final boolean replayGain;
    private final int n;
    private int i = 0;

    private void removeFile(String file) {
        try {
            Process process = new ProcessBuilder("rm", "-f", file).start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean wait(Process process) {
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String stdin;
        try {
            stdin = in.readLine();
            while (stdin != null) {
                stdin = in.readLine();
            }
            in.close();
            return process.waitFor() != 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void toWaw(String name, String subName) throws Exception {
        String name2 = "." + subName + ".wav";
        String name4 = ".2" + subName + ".wav";
        String name3 = subName + ".m4a";
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        if (name.endsWith("mp3")) {
            command.add("-acodec");
            command.add("mp3float");
        }
        command.addAll(Arrays.asList("-i", name, "-acodec", "pcm_f32le", name2));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        Process process = processBuilder.start();
        if (wait(process)) {
            removeFile(name2);
            throw new Exception(name3 + "to waw Error");
        }

        //Ресамплінг до 48000
        processBuilder.command("sox", name2, "-e", "floating-point", name4, "rate", "-v", "-I", "-a", "-b", "99.7", "-p", "100", "48000");
        process = processBuilder.start();
        if (wait(process)) {
            removeFile(name2);
            removeFile(name4);
            throw new Exception(name3 + "resample Error");
        }

        processBuilder.command("mv", name4, name2);
        process = processBuilder.start();
        if (wait(process)) {
            removeFile(name2);
            removeFile(name4);
            throw new Exception(name3 + "mv Error");
        }
    }

    private void toM4a(String subName, String title, String author) throws Exception {
        String name2 = "." + subName + ".wav";
        String name3 = subName + ".m4a";
        List<String> command = new ArrayList<>();
        command.add("neroAacEnc");
        if (q > 1) {
            command.addAll(Arrays.asList("-2pass", "-br", String.valueOf(q * 1000)));
        } else {
            command.add("-q");
            command.add(String.valueOf(q));
        }
        command.addAll(Arrays.asList("-if", name2, "-of", name3));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        Process process = processBuilder.start();
        if (wait(process)) {
            System.out.println(name3 + " to m4a Error, try to ogg");
            removeFile(name3);

            //Конвертація в ogg
            String[] quality;
            String nameogg = subName + ".ogg";
            double Q;
            if (q > 1) {
                Q = q;
            } else {
                Q = q * 10.0;
            }
            if (Q > 10) {
                quality = new String[]{"-b", String.valueOf(Q), "-m", String.valueOf(Q), "-M", String.valueOf(Q)};
            } else {
                quality = new String[]{"-q", String.valueOf(Q)};
            }
            command.clear();
            command.addAll(Arrays.asList("oggenc", "-Q", "--utf8", "--ignorelength", "-t", title));
            if (!(author == null || author.isEmpty())) {
                command.addAll(Arrays.asList("-a", author));
            }
            command.addAll(Arrays.asList(quality));
            command.addAll(Arrays.asList("-o", nameogg, name2));
            processBuilder.command(command);
            process = processBuilder.start();
            if (wait(process)) {
                removeFile(nameogg);
                throw new Exception(nameogg + "to ogg Error");
            }

            processBuilder.command("vorbisgain", nameogg);
            process = processBuilder.start();
            wait(process);

            removeFile(name2);
            throw new Exception(name3 + "to m4a Error");
        }

        command.clear();
        command.add("neroAacTag");
        command.add("-meta:title=" + title);
        if (!(author == null || author.isEmpty())) {
            command.add("-meta:artist=" + author);
        }
        command.add(name3);
        processBuilder.command(command);
        process = processBuilder.start();
        if (wait(process)) {
            removeFile(name3);
            throw new Exception(name3 + "to m4a tag Error");
        }

        //знаходження ReplayGain
        command.clear();
        command.addAll(Arrays.asList("aacgain", "-q", "-e"));
        if (replayGain) {
            command.add("-r");
        }
        command.add(name3);
        processBuilder.command(command);
        try {
            process = processBuilder.start();
            if (wait(process)) {
                throw new IOException();
            }
        } catch (IOException e) {
            System.out.println(name3 + " aacgain Warning");
        }
    }

    private void toMp3(String subName, String title, String author) throws Exception {
        String name2 = "." + subName + ".wav";
        String name3 = subName + ".mp3";
        List<String> command = new ArrayList<>(Arrays.asList("lame", "-S", "--bitwidth", "32", "-o", "--buffer-constraint", "maximum", "--preset", "extreme"));
        double Q = q;
        if (Q > 320) {
            Q = 320;
        }
        if (Q <= 10) {
            command.addAll(Arrays.asList("-V", String.valueOf(q), "-q", "0"));
        } else {
            command.addAll(Arrays.asList("-v", "0", "-q", "0", "-b", String.valueOf(Q), "-B", String.valueOf(Q)));
        }
        command.addAll(Arrays.asList("--tt", title));
        if (!(author == null || author.isEmpty())) {
            command.add("--ta");
            command.add(author);
        }
        command.add(name2);
        command.add(name3);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        if (wait(process)) {
            throw new Exception(name3 + "to mp3 Error");
        }

        command.clear();
        command.addAll(Arrays.asList("mp3gain", "-q", "-e"));
        if (replayGain) {
            command.add("-r");
        }
        command.add(name3);
        processBuilder.command(command);
        try {
            process = processBuilder.start();
            if (wait(process)) {
                throw new IOException();
            }
        } catch (IOException e) {
            System.out.println(name3 + " mp3gain Warning");
        }
    }

    public AudioConv(double q, String path, Format format, boolean g, boolean rf) {
        this.q = q;
        this.format = format;
        replayGain = g;
        removeOrig = rf;
        listFiles = new ArrayList<>();
        System.out.println("Якість: " + this.q);
        if (format == Format.MP3) {
            System.out.println("Формат: mp3");
        } else if (format == Format.M4A) {
            System.out.println("Формат: m4a");
        }
        Pattern formats = Pattern.compile("(mp3|ogg|aac|mp4|m4a|wma|opus|oga|flac|wav|aiff|webm|matroska|asf|amr|avi|mov|3gp)$");
        for (File f : Objects.requireNonNull(new File(path).listFiles())) {
            if (formats.matcher(f.getName().toLowerCase()).find()) {
                listFiles.add(f.getName());
            }
        }
        n = listFiles.size();
    }

    public void work() {
        while (true) {
            try {
                String subName;
                String name;
                synchronized (this) {
                    if (!listFiles.isEmpty()) {
                        name = listFiles.remove(0);
                    } else {
                        break;
                    }
                    subName = name.substring(0, name.lastIndexOf("."));
                    i += 1;
                    String out = (100 * i / n) + "% (" + i + "/" + n + ") " + subName;
                    if (format == Format.M4A) {
                        System.out.println(out + ".m4a");
                    } else if (format == Format.MP3) {
                        System.out.println(out + ".mp3");
                    }
                }
                if (name.isEmpty()) {
                    continue;
                }
                String[] items = name.substring(0, name.lastIndexOf(".")).split(" - ");
                String title;
                String author = null;
                if (items.length == 2) {
                    author = items[0];
                    title = items[1];
                } else {
                    title = items[0];
                }

                toWaw(name, subName);

                if (format == Format.M4A) {
                    toM4a(subName, title, author);
                } else if (format == Format.MP3) {
                    toMp3(subName, title, author);
                }

                removeFile("." + subName + ".wav");

                //Видалення оригінального файлу
                if (removeOrig) {
                    Process process = new ProcessBuilder().command("rm", "-f", name).start();
                    if (wait(process)) {
                        System.out.println(name + " remove orig Warning");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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