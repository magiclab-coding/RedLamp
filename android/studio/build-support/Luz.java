package es.luzlocal.matter;
import android.app.Activity;
import android.widget.*;
import chip.devicecontroller.*;
public class Luz extends Activity {
 static ChipDeviceController controller;
 long pointer;int endpoint,cold,warm,minLevel,maxLevel; boolean lit,pairing,linking;
 TextView status,found,brightness,temperature;Button power,qr;SeekBar level,kelvin;
 void lost(Exception e){}void draw(){}void enable(boolean b){}void connect(){}void send(int t,int v){}void refresh(){}void message(String s){}boolean saved(){return false;}
 void setup(){}void startScan(){}void stopScan(){}void openQr(){}void pick(){}void auth(String s){}void pair(String s,NetworkCredentials c,boolean n){}void wifi(String s){}
}
