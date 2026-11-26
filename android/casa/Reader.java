package es.luzlocal.matter;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.*;
import android.hardware.Camera;
import android.view.*;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.*;

public final class Reader extends Activity implements SurfaceHolder.Callback,Camera.PreviewCallback {
 Camera cam;TextView hint;boolean done;
 protected void onCreate(Bundle state){super.onCreate(state);FrameLayout frame=new FrameLayout(this);SurfaceView preview=new SurfaceView(this);frame.addView(preview);preview.getHolder().addCallback(this);hint=new TextView(this);hint.setPadding(28,24,28,24);hint.setBackgroundColor(0xd9111b22);hint.setTextColor(-1);hint.setTextSize(18);hint.setText("Centra el QR Matter de la bombilla o de su caja");frame.addView(hint,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));setContentView(frame);}
 public void surfaceCreated(SurfaceHolder h){try{cam=Camera.open();Camera.Parameters p=cam.getParameters();if(p.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);Camera.Size best=null;for(Camera.Size s:p.getSupportedPreviewSizes())if(s.width<=1280&&(best==null||s.width>best.width))best=s;if(best!=null)p.setPreviewSize(best.width,best.height);cam.setParameters(p);cam.setDisplayOrientation(90);cam.setPreviewDisplay(h);cam.setPreviewCallback(this);cam.startPreview();}catch(Exception e){release();hint.setText("Cámara no disponible. Vuelve atrás y selecciona una imagen del QR.");}}
 public void surfaceChanged(SurfaceHolder h,int fmt,int w,int y){} public void surfaceDestroyed(SurfaceHolder h){release();}
 void release(){if(cam!=null){cam.setPreviewCallback(null);try{cam.stopPreview();}catch(Exception ignored){}cam.release();cam=null;}}
 long next;public void onPreviewFrame(byte[] frame,Camera camera){if(done||SystemClock.elapsedRealtime()<next)return;next=SystemClock.elapsedRealtime()+280;Camera.Size s=camera.getParameters().getPreviewSize();try{String qr=decode(new PlanarYUVLuminanceSource(frame,s.width,s.height,0,0,s.width,s.height,false));if(!qr.startsWith("MT:")){hint.setText("QR leído. Busca el QR con el logotipo Matter.");return;}done=true;setResult(RESULT_OK,new Intent().putExtra("qr",qr));finish();}catch(ReaderException ignored){}}
 static String decode(LuminanceSource source)throws ReaderException{return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText().trim();}
 static String image(Context c,Uri uri)throws Exception{BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;try(InputStream i=c.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(i,null,opts);}opts.inSampleSize=1;while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>2200)opts.inSampleSize*=2;opts.inJustDecodeBounds=false;Bitmap b;try(InputStream i=c.getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(i,null,opts);}if(b==null)throw new IOException();int w=b.getWidth(),h=b.getHeight();int[] pixels=new int[w*h];b.getPixels(pixels,0,w,0,0,w,h);b.recycle();return decode(new RGBLuminanceSource(w,h,pixels));}
}
