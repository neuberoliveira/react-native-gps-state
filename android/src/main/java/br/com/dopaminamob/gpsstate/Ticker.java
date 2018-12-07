package br.com.dopaminamob.gpsstate;

public class Ticker {
    private int maxTicks = 10;
    private int interval = 1;
    private int ticksCount = 0;
    private boolean running = false;
    private TickerCallBack callBack;

    public void startTick(TickerCallBack cb){
        running = true;
        callBack = cb;
        tick();
    }

    public void stopTick(){
        running = false;
        ticksCount = 0;
    }

    private void tick(){
        if(!reachedMaxTicks() && hasCallback() && isRunning()){
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        callBack.tick();
                        ticksCount ++;

                        tick();
                    }
                },
                getInterval()
            );
        }else{
            stopTick();
        }
    }


    public int getMaxTicks() {
        return maxTicks;
    }

    public void setMaxTicks(int maxTicks) {
        this.maxTicks = maxTicks;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isRunning(){
        return running;
    }

    public boolean hasCallback(){
        return callBack!=null;
    }

    public boolean reachedMaxTicks(){
        return ticksCount >= getMaxTicks();
    }
}

interface TickerCallBack {
    void tick();
}
