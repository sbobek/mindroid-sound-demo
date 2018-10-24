package geist.re.mindroid;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

import geist.re.mindlib.RobotControlActivity;
import geist.re.mindlib.RobotService;
import geist.re.mindlib.events.SoundStateEvent;
import geist.re.mindlib.events.UltrasonicStateEvent;
import geist.re.mindlib.exceptions.SensorDisconnectedException;
import geist.re.mindlib.hardware.Motor;
import geist.re.mindlib.hardware.Sensor;
import geist.re.mindlib.hardware.UltrasonicSensor;
import geist.re.mindlib.listeners.SoundSensorListener;
import geist.re.mindlib.listeners.UltrasonicSensorListener;
import geist.re.mindlib.tasks.PlaySoundTask;

public class RobotControlSoundDemo extends RobotControlActivity {
    private static final String TAG = "ControlApp";
    private static final String ROBOT_NAME = "02Bolek";

    FloatingActionButton start;
    FloatingActionButton stop;
    FloatingActionButton voice;
    FloatingActionButton connect;
    FloatingActionButton orientation;

    Switch headSw;
    Switch soundSw;
    Switch eyesSw;


    TextView xText;
    TextView yText;
    TextView zText;


    boolean breaking = false;
    boolean head = true;

    private SoundSensorListener soundListener  = new SoundSensorListener() {
        public static final int SECONDS_WINDOW = 1;
        public static final double SILENCE_THRESHOLD = 0.2;
        private boolean deaf = false;
        private boolean playing = false;
        private long lastNoise = System.currentTimeMillis();
        private long lastCall = System.currentTimeMillis();
        Random r = new Random();
        Rolling rolling = new Rolling((int)(1000/SoundSensorListener.DEFAULT_LISTENING_RATE* SECONDS_WINDOW));
        int [] prompts = new int[]{
                R.raw.anybody,
                R.raw.wannaplay,
                R.raw.donthear
        };
        int [] greets = new int[]{
                R.raw.lauder,
                R.raw.lauder2,
                R.raw.going
        };

        @Override
        public void onEventOccurred(SoundStateEvent e) {
            double intensity = e.getSoundIntensity();
            rolling.add(intensity);

            if(System.currentTimeMillis()-lastCall < 500) return;
            lastCall = System.currentTimeMillis();

            double avgNoise = rolling.getAverage();
            Log.d(TAG, "Intensity "+intensity+ " average "+avgNoise);
            int speed = 0;
            if (avgNoise > SILENCE_THRESHOLD && !deaf){
                lastNoise = System.currentTimeMillis();
                speed = (int)(avgNoise*100);
                robot.executeSyncTwoMotorTask(robot.motorA.run(speed),
                        robot.motorB.run(speed));

                if(r.nextInt(100) < 30) {
                    playing=true;
                    MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                            greets[r.nextInt(greets.length)]);
                    //mp.start();
                    Log.d(TAG, "Playing sound...");
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            playing = false;
                        }
                    });
                }
            }else{
                robot.executeSyncTwoMotorTask(robot.motorA.stop(),
                        robot.motorB.stop());
                if(System.currentTimeMillis()-lastNoise > 5000 && !deaf && !playing){
                    // ,l   deaf=true;
                    playing=true;
                    int s = r.nextInt(prompts.length);
                    MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                            prompts[s]);
                    //mp.start();
                    Log.d(TAG,"Playing sound...");
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            deaf=false;
                            playing=false;
                        }
                    });
                    //wait 5 seconds untill new prompt
                    lastNoise+=5000;
                }
            }
        }
    };

    UltrasonicSensorListener ultrasonicListener = new UltrasonicSensorListener() {
        @Override
        public void onEventOccurred(UltrasonicStateEvent e) {
            // play sound and move backward if can't see a thing
            Log.d(TAG, "Distance: "+e.getRawOutput());
            if(robot.motorB.getState() != Motor.STATE_RUNNING && e.getDistanceInCentimeters() < 2){
                //dark();
                //robot.executeSyncTwoMotorTask(robot.motorA.run(-30,180),robot.motorB.run(-30,180));
            }
        }
    };



    /**************************************************************/
    /**************************************************************/
    /**************************************************************/
    /**************************************************************/

    @Override
    public void commandProgram() throws SensorDisconnectedException {
        super.commandProgram();
        /*************** START YOUR PROGRAM HERE ***************/
        breaking=false;
        robot.soundSensor.connect(Sensor.Port.ONE, Sensor.Type.SOUND_DB);
        robot.ultrasonicSensor.connect(Sensor.Port.THREE);

        // Random head movement
        Random r = new Random();
        double drift = 0;
        while(!breaking && head){
            if(robot.motorC.getState() == Motor.STATE_RUNNING) pause(500);
            int side = r.nextInt(10);
            int sp = r.nextInt(5);
            int angle = r.nextInt(20);
            if(side < 5) {
                angle = -angle;
                sp = -sp;
            }
            if(Math.abs(drift + angle) > 20){
                continue;
            }else{
                drift += angle;
            }

            Log.d(TAG, "drift "+drift+" angle "+angle);

            robot.executeMotorTask(robot.motorC.run(sp,Math.abs(angle)));
            int sleepTime = 1000+r.nextInt(3000);
            pause(sleepTime);

        }

    }

    private  class Rolling {
        private final Queue<Double> window = new ArrayDeque<Double>();
        private final int period;
        private double sum = 0.0;

        public Rolling(int period) {
            this.period = period;
        }

        public void add(double num) {
            sum = sum + (num);
            window.add(num);
            if (window.size() > period) {
                sum = sum - (window.remove());
            }
        }

        public double getAverage() {
            if (window.size() < period) return 0;
            return sum / (double)period;
        }
    }

    @Override
    public void onVoiceCommand(String message) {
        super.onVoiceCommand(message);
        /*************** HANDLE VOICE MESSAGE HERE ***************/

        if(message.equals("run forward")){
            speakBack("No problem");
            robot.executeSyncTwoMotorTask(robot.motorA.run(30),robot.motorB.run(30));
        }else if(message.equals("stop")){
            speakBack("It was a pleasure");
            robot.executeSyncTwoMotorTask(robot.motorA.stop(), robot.motorB.stop());
        }else if(message.equals("run backward")) {
            speakBack("I'm executing");
            robot.executeSyncTwoMotorTask(robot.motorA.run(-30), robot.motorB.run(-30));
        }else{
            Log.d(TAG, "Received wrong command: "+message);
            //error();
        }
    }



    @Override
    protected synchronized void onGestureCommand(double x, double y, double z) {
        displayValues(x,y,z);
        /*************** HANDLE GESTURES HERE ***************/
    }


    /**************************************************************/
    /**************************************************************/
    /**************************************************************/
    /**************************************************************/

    private void displayValues(double x, double y, double z){
        xText.setText(Double.toString(x));
        yText.setText(Double.toString(y));
        zText.setText(Double.toString(z));
    }
    private void pause(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onStartListeningForVoiceCommands() {

    }

    @Override
    protected void onStartListeningForVoiceWakeup() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_control);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        start = (FloatingActionButton) findViewById(R.id.start);
        stop = (FloatingActionButton) findViewById(R.id.stop);
        voice = (FloatingActionButton) findViewById(R.id.voice);
        connect = (FloatingActionButton) findViewById(R.id.connect);
        orientation = (FloatingActionButton) findViewById(R.id.orientationButton);


        xText = (TextView) findViewById(R.id.xText);
        yText = (TextView) findViewById(R.id.yText);
        zText = (TextView) findViewById(R.id.zText);


        headSw = (Switch) findViewById(R.id.head);
        soundSw = (Switch) findViewById(R.id.sound);
        eyesSw = (Switch) findViewById(R.id.eyes);



        start.setVisibility(FloatingActionButton.INVISIBLE);
        stop.setVisibility(FloatingActionButton.INVISIBLE);
        voice.setVisibility(FloatingActionButton.INVISIBLE);
        connect.setVisibility(FloatingActionButton.INVISIBLE);
        orientation.setVisibility(FloatingActionButton.INVISIBLE);
        headSw.setEnabled(false);
        soundSw.setEnabled(false);
        eyesSw.setEnabled(false);


        eyesSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked) {
                    try {
                        robot.ultrasonicSensor.registerListener(ultrasonicListener);
                    } catch (SensorDisconnectedException e) {
                        e.printStackTrace();
                    }
                }else{
                    robot.ultrasonicSensor.unregisterListener();
                }
            }
        });

        soundSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked == true) {
                    try {
                        robot.soundSensor.registerListener(soundListener);
                    } catch (SensorDisconnectedException e) {
                        e.printStackTrace();
                    }
                }else{
                    robot.soundSensor.unregisterListener();
                }

            }
        });

        headSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    head = true;
                }else{
                    head=false;
                }
            }
        });

        //keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onRobotServiceConnected() {
        //enable connect button
        connect.setVisibility(FloatingActionButton.VISIBLE);

    }


    @Override
    protected void onRobotConnected() {
        //robot connected enable buttons;
        Toast.makeText(RobotControlSoundDemo.this,"Robot connected, go!",Toast.LENGTH_LONG).show();
        start.setVisibility(FloatingActionButton.VISIBLE);
        stop.setVisibility(FloatingActionButton.VISIBLE);
        voice.setVisibility(FloatingActionButton.VISIBLE);
        connect.setVisibility(FloatingActionButton.VISIBLE);
        orientation.setVisibility(FloatingActionButton.VISIBLE);
        headSw.setEnabled(true);
        soundSw.setEnabled(true);
        eyesSw.setEnabled(true);

    }



    @Override
    protected void onRobotDisconnected() {
        //disable buttons and try connect again
        start.setVisibility(FloatingActionButton.INVISIBLE);
        stop.setVisibility(FloatingActionButton.INVISIBLE);
        voice.setVisibility(FloatingActionButton.INVISIBLE);
        connect.setVisibility(FloatingActionButton.VISIBLE);
        orientation.setVisibility(FloatingActionButton.INVISIBLE);

        headSw.setEnabled(false);
        soundSw.setEnabled(false);
        eyesSw.setEnabled(false);

    }

    public void gestures(View v){
        startOrientationScanning();
    }

    public void start(View v){
        if(robot == null || robot.getConnectionState() != RobotService.CONN_STATE_CONNECTED){
            Toast.makeText(this, "Waiting for robot to connect",Toast.LENGTH_LONG).show();
            if(robot.getConnectionState() != RobotService.CONN_STATE_CONNECTING){
                robot.connectToRobot(ROBOT_NAME);
            }
            return;
        }
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected Exception doInBackground(Void... voids) {
                try {
                    commandProgram();
                } catch (SensorDisconnectedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }
    public void voice(View v){
        if(robot == null || robot.getConnectionState() != RobotService.CONN_STATE_CONNECTED){
            Toast.makeText(this, "Waiting for robbot to connect...", Toast.LENGTH_LONG).show();
            if(robot.getConnectionState() != RobotService.CONN_STATE_CONNECTING){
                robot.connectToRobot(ROBOT_NAME);
            }
            return;
        }
        startRecognizer();
    }

    public void hi(View v){
        MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                R.raw.hi);
        mp.start();
    }

    public void dark(){
        MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                R.raw.bye);
        mp.start();
    }

    public void cantSee(){
        MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                R.raw.bye);
        mp.start();
    }

    public void bye(View v){
        MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                R.raw.bye);
        mp.start();
    }


    public void yes(View v){
        MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                R.raw.yes);
        mp.start();
    }

    public void no(View v){
        MediaPlayer mp = MediaPlayer.create(RobotControlSoundDemo.this,
                R.raw.no);
        mp.start();
    }

    public void stop(View c){
        if(robot == null || robot.getConnectionState() != RobotService.CONN_STATE_CONNECTED){
            Toast.makeText(this, "Waiting for robot to connect...", Toast.LENGTH_LONG).show();
            return;
        }
        breaking = true;
        stopOrientationScanning();
        if(recognizer != null) stopRecognizer();
        robot.soundSensor.unregisterListener();
        robot.ultrasonicSensor.unregisterListener();
        robot.lightSensor.unregisterListener();
        robot.touchSensor.unregisterListener();
        robot.executeSyncThreeMotorTask(robot.motorA.stop(), robot.motorB.stop(), robot.motorC.stop());

    }

    public void connect(View v){
        if(robot == null){
            //bind to robot
            Toast.makeText(this,"Error, robot service is down...",Toast.LENGTH_LONG).show();
        } else if(robot.getConnectionState() != RobotService.CONN_STATE_CONNECTED &&
                robot.getConnectionState() != RobotService.CONN_STATE_CONNECTING){
            new AsyncTask<Void, Void, Exception>(){
                ProgressDialog progress = new ProgressDialog(RobotControlSoundDemo.this);
                boolean dismissed = false;

                @Override
                protected void onPreExecute() {
                    progress.setMessage("Connecting...");
                    progress.setTitle("Connecting to robot");
                    progress.setCancelable(false);
                    progress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            dismissed = true;
                        }
                    });
                    progress.show();
                }

                @Override
                protected Exception doInBackground(Void... voids) {
                    Exception ex = null;
                    robot.connectToRobot(ROBOT_NAME);
                    while(robot.getConnectionState() != RobotService.CONN_STATE_CONNECTED){
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(dismissed) break;
                        if(robot.getConnectionState() == RobotService.CONN_STATE_DISCONNECTED){
                            robot.connectToRobot(ROBOT_NAME);
                        }
                    }
                    return ex;
                }

                @Override
                protected void onPostExecute(Exception e) {
                    progress.dismiss();
                }
            }.execute();



        }
    }

    public void quit(View v){
        finish();
    }



}
