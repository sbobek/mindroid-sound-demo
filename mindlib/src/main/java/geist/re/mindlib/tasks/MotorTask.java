package geist.re.mindlib.tasks;

import android.util.Log;

import java.nio.ByteBuffer;

import geist.re.mindlib.RobotService;
import geist.re.mindlib.hardware.Motor;

/**
 * Created by sbk on 09.02.17.
 */

public class MotorTask implements RobotTask {

    private static final byte VAL_LENGTH_LSB = 0x0c;
    private static final byte VAL_LENGTH_MSB = 0x00;
    private static final byte VAL_DIRECT_CMD = (byte) 0x80;
    private static final byte VAL_CMD_TYPE = 0x04;

    private static final byte VAL_MODE_MOTORON = 0x01;
    private static final byte VAL_MODE_USE_BREAKES = 0x02;
    private static final byte VAL_MODE_ENABLE_REGULATION = 0x04;

    private static final byte VAL_REGULATION_IDLE = 0x00;
    private static final byte VAL_REGULATION_POWER = 0x01;
    private static final byte VAL_REGULATION_SYNC = 0x02;

    private static final byte VAL_RUN_STATE_IDLE = 0x00;
    private static final byte VAL_RUN_STATE_RAMPPUP = 0x10;
    private static final byte VAL_RUN_STATE_RUNNING = 0x20;
    private static final byte VAL_RUN_STATE_RAMPDOWN = 0x40;

    private static final byte VAL_RESET_MESSAGE_LENGTH_LSB = 0x40;
    private static final byte VAL_RESET_MESSAGE_LENGTH_MSB = 0x00;



    private static final int IDX_OUTPUT_PORT = 4;
    private static final int IDX_POWER_SET_POINT = 5;
    private static final int IDX_MODE = 6;
    private static final int IDX_REGULATION = 7;
    private static final int IDX_TURN = 8;
    private static final int IDX_RUN_STATE = 9;

    private static final int IDX_TACHO_START = 10;





    byte [] resetMotorPosition = {VAL_RESET_MESSAGE_LENGTH_LSB,VAL_RESET_MESSAGE_LENGTH_MSB,0x00,0x0A,0x00};
    byte [] data = {VAL_RESET_MESSAGE_LENGTH_LSB,VAL_RESET_MESSAGE_LENGTH_MSB,0x00,0x0A,0x00,
            VAL_LENGTH_LSB, VAL_LENGTH_MSB, VAL_DIRECT_CMD, VAL_CMD_TYPE, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public MotorTask(byte [] data, byte [] resetMotorPosition){
        this.data = data;
        this.resetMotorPosition = resetMotorPosition;
    }

    public MotorTask(Motor m){
        setOutputPort(m.getPort());
        enableRegulationMode();
        enableSpeedRegulation();
        enableUseBreaksMode();
        enableMotorOnMode();
        setRunStateRunning();

    }




    public void setOutputPort(byte port){
        data[IDX_OUTPUT_PORT] = port;
        resetMotorPosition[IDX_OUTPUT_PORT] = port;
    }

    public void setPowerSetPoint(byte powerSetPoint){
        data[IDX_POWER_SET_POINT] = powerSetPoint;
    }

    public void enableSpeedRegulation(){
        data[IDX_REGULATION] |= VAL_REGULATION_POWER;
    }

    public void sync(){
        data[IDX_REGULATION] |= VAL_REGULATION_SYNC;
    }

    public MotorTask syncWith(MotorTask other){
        sync();
        other.sync();

        byte [] mergedData = new byte[data.length+other.data.length];
        byte [] mergedResetMotorPositionData = new byte[resetMotorPosition.length+other.resetMotorPosition.length];
        System.arraycopy(resetMotorPosition, 0, mergedResetMotorPositionData, 0, resetMotorPosition.length);
        System.arraycopy(other.resetMotorPosition, 0, mergedResetMotorPositionData, resetMotorPosition.length,other.resetMotorPosition.length);

        System.arraycopy(data, 0, mergedData, 0, data.length);
        System.arraycopy(other.data, 0, mergedData, data.length,other.data.length);

        return new MotorTask(mergedData, mergedResetMotorPositionData);

    }

    public void setTurnRatio(byte turnRatio){
        data[IDX_TURN] = turnRatio;
    }

    public void setRunStateRunning(){
        data[IDX_RUN_STATE] = VAL_RUN_STATE_RUNNING;
    }

    public void enableMotorOnMode(){
        data[IDX_MODE] |= VAL_MODE_MOTORON;
    }
    public void enableUseBreaksMode(){
        data[IDX_MODE] |= VAL_MODE_USE_BREAKES;
    }
    public void enableRegulationMode(){
        data[IDX_MODE] |= VAL_MODE_ENABLE_REGULATION;
    }

    public void setRotationAngleLimit(double angleLimit){

    }

    public void setTachoLimit(int limit){
        byte[] bytes = ByteBuffer.allocate(4).putInt(limit).array();

        data[IDX_TACHO_START] = bytes[3];
        data[IDX_TACHO_START+1] = bytes[2];
        data[IDX_TACHO_START+2] = bytes[1];
        data[IDX_TACHO_START+3] = bytes[0];
        Log.d(TAG, "Tacho limit: "+Integer.toHexString(limit)+" was translated to "+
                Integer.toHexString(bytes[3])+"-"+
                Integer.toHexString(bytes[2])+"-"+
                Integer.toHexString(bytes[1])+"-"+
                Integer.toHexString(bytes[0]));

    }


    @Override
    public void execute(RobotService rs) {
        //depending on the powerset -- motos is busy or not busy
        rs.writeToNXTSocket(resetMotorPosition);
        rs.writeToNXTSocket(data);
        //TODO: add listener to stop on tacho count if tacho count set
    }

    @Override
    public void dismiss(RobotService rs) {
        //unregister litener?
        //do nothing, just do not execute
        //or actually do something - cancel timer for tacho, stop motors if they run for infinity?
    }
}