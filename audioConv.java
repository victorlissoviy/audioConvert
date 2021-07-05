
public class audioConv{
    public static void main(String[] args) {
        int processors = Runtime.getRuntime().availableProcessors();
        Thread[] workers = new Thread[processors];
        parametrs pars = new parametrs();
        try{
            for(int i = 0; i < args.length - 1; i++){
                if(args[i].equals("-q")){
                    pars.setQ(Double.parseDouble(args[i + 1]));
                }
                if(args[i].equals("-f")){
                    pars.setFormat(args[i + 1]);
                }
                if(args[i].equals("-c")){
                    System.out.println(args[i + 1]);
                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        for(int i = 0; i < processors; i++){
            workers[i] = new Thread(new worker(pars));
        }
        for(int i = 0; i < processors; i++){
            workers[i].start();
        }
    }
}

class parametrs{
    double q = 0;
    String format = "m4a";

    public void setQ(double q)throws Exception{
        if(q >= 0){
            this.q = q;
        }else{
            throw new Exception("Якість не може бути менше 0");
        }
    }

    public void setFormat(String format) throws Exception{
        if("mp3,flac,m4a,ogg".indexOf(format) != -1){
            this.format = format;
        }else{
            throw new Exception("Формат " + format + " не підтримується");
        }
    }
    
    public double getQ(){
        return this.q;
    }
    
    public String getFormat(){
        return this.format;
    }
}

class worker implements Runnable{
    private parametrs pars;
    public worker(parametrs pars){
        this.pars = pars;
    }
    public void run(){
        System.out.println(pars.getFormat());
    }
    private void toWaw(){

    }
    private void toMp3(){

    }
    private void toFlac(){

    }
    private void toOgg(){

    }
    private void toM4a(){

    }
}