package es.luzlocal.matter;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.net.nsd.*;
import android.net.wifi.WifiManager;
import android.view.*;
import android.widget.*;
import java.util.*;
import chip.devicecontroller.*;

public final class Luz extends Lamp {
 final int bg=0xff111b22,card=0xff203039,ink=0xfff5f2e9,muted=0xffbecbc9,amber=0xffffcd87;
 final Handler ui=new Handler(Looper.getMainLooper());
 TextView found; LinearLayout lights; Button qr;
 BluetoothLeScanner scanner; NsdManager nsd; NsdManager.DiscoveryListener discovery;
 final Set<String> nearby=new HashSet<>(); boolean scanning,asked,active;
 int d(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 GradientDrawable pill(int a,int b,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{a,b});g.setCornerRadius(d(radius));return g;}
 TextView text(LinearLayout box,String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);box.addView(t);return t;}
 LinearLayout block(LinearLayout root,int a,int b){LinearLayout x=new LinearLayout(this);x.setOrientation(1);x.setPadding(d(20),d(18),d(20),d(18));x.setBackground(pill(a,b,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=d(14);root.addView(x,lp);return x;}
 Button action(LinearLayout box,String title,Runnable callback,boolean primary){Button v=new Button(this);v.setText(title);v.setAllCaps(false);v.setTextSize(16);v.setTextColor(primary?bg:ink);v.setBackground(pill(primary?amber:0xff354954,primary?0xffffb66f:0xff354954,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,d(50));lp.topMargin=d(10);box.addView(v,lp);v.setOnClickListener(w->callback.run());return v;}
 @Override void draw(){getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(d(16),d(22),d(16),d(24));scroll.addView(root);setContentView(scroll);
 LinearLayout hero=block(root,0xff23373c,0xff434638);TextView h=text(hero,"Luz MOES",34,ink);h.setTypeface(Typeface.create("sans-serif-medium",0));status=text(hero,"Preparando conexión…",16,0xffffdcaa);power=action(hero,"Encender",()->send(0,lit?0:1),true);
 lights=block(root,card,card);panel=lights;brightness=text(lights,"Intensidad",18,ink);level=slider(false);style(level,amber);temperature=text(lights,"Temperatura del blanco",18,ink);kelvin=slider(true);style(kelvin,0xffa7d4e2);
 LinearLayout ends=new LinearLayout(this);lights.addView(ends);TextView a=text(ends,"Cálida",13,muted);a.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));text(ends,"Fría",13,muted);
 LinearLayout pairingBox=block(root,card,card);text(pairingBox,"Conexión",21,ink);found=text(pairingBox,saved()?"Bombilla guardada en este móvil":"Buscando dispositivos Matter…",15,muted);action(pairingBox,saved()?"Conectar":"Buscar cerca",()->{if(saved())connect();else startScan();},false);
 if(!saved()){qr=action(pairingBox,"Escanear QR Matter",()->openQr(),true);action(pairingBox,"Leer QR de una imagen",()->pick(),false);}
 enable(false);}
 Glow glow;
 final class Glow extends View {
   final android.graphics.Paint p=new android.graphics.Paint(1);
   Glow(){super(Luz.this);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
   protected void onDraw(android.graphics.Canvas c){float w=getWidth(),h=getHeight(),cx=w*.5f,cy=h*.50f;float mix=kelvin==null?0:kelvin.getProgress()/100f;
     int hue=Color.rgb((int)(255-56*mix),(int)(183+46*mix),(int)(102+151*mix));float radius=h*(.48f+(level==null?0:level.getProgress())*.003f);
     p.setStyle(android.graphics.Paint.Style.FILL);p.setShader(new android.graphics.RadialGradient(cx,cy,radius,new int[]{(lit?0x99000000:0x33000000)|(hue&0xffffff),hue&0xffffff},null,android.graphics.Shader.TileMode.CLAMP));c.drawCircle(cx,cy,radius,p);p.setShader(null);
     c.save();c.translate(cx,cy);float s=h/112f;c.scale(s,s);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(lit?hue:0xffc4cbbf);
     android.graphics.Path glass=new android.graphics.Path();glass.moveTo(-10,22);glass.cubicTo(-11,10,-26,6,-26,-12);glass.cubicTo(-26,-45,26,-45,26,-12);glass.cubicTo(26,6,11,10,10,22);glass.close();c.drawPath(glass,p);
     p.setColor(hue);p.setStrokeWidth(2.8f);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);c.drawLine(-9,-17,-4,13,p);c.drawLine(9,-17,4,13,p);p.setColor(0xffaebbbb);for(int y=26;y<38;y+=5)c.drawLine(-9,y,9,y,p);c.restore();}
 }
 void style(SeekBar v,int tint){v.setProgressTintList(ColorStateList.valueOf(tint));v.setThumbTintList(ColorStateList.valueOf(tint));v.setMinimumHeight(d(48));if(v==level){LinearLayout hero=(LinearLayout)power.getParent();glow=new Glow();hero.addView(glow,2,new LinearLayout.LayoutParams(-1,d(94)));}}
 @Override void enable(boolean on){super.enable(on);if(glow!=null)glow.invalidate();}
 @Override void setup(){if(saved()||pairing)return;if(!asked){asked=true;if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},7);return;}}startScan();}
 final ScanCallback ble=new ScanCallback(){public void onScanResult(int type,ScanResult result){if(scanning){nearby.add("ble:"+result.getDevice().getAddress());showFound();}}public void onScanFailed(int e){if(scanning&&nearby.isEmpty())found.setText("Bluetooth no disponible. Puedes usar el QR.");}};
 void showFound(){if(!nearby.isEmpty())found.setText("Señal Matter detectada cerca. Escanea el QR de tu bombilla para vincularla.");}
 final Runnable finishScan=()->{stopScan();if(nearby.isEmpty())found.setText("Sin señales Matter. Activa el modo de vinculación de la bombilla y vuelve a buscar.");else showFound();};
 void startScan(){if(scanning||pairing||saved())return;scanning=true;nearby.clear();found.setText("Buscando por Bluetooth y WiFi…");status.setText("Pendiente de vincular");try{BluetoothAdapter ad=BluetoothAdapter.getDefaultAdapter();if(ad!=null&&ad.isEnabled()&&checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==0){scanner=ad.getBluetoothLeScanner();if(scanner!=null){android.os.ParcelUuid id=android.os.ParcelUuid.fromString("0000fff6-0000-1000-8000-00805f9b34fb");ScanFilter filter=new ScanFilter.Builder().setServiceData(id,new byte[0]).build();scanner.startScan(Collections.singletonList(filter),new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),ble);}}}catch(RuntimeException ignored){}
 try{nsd=(NsdManager)getSystemService(NSD_SERVICE);discovery=new NsdManager.DiscoveryListener(){public void onDiscoveryStarted(String t){}public void onDiscoveryStopped(String t){}public void onStartDiscoveryFailed(String t,int e){}public void onStopDiscoveryFailed(String t,int e){}public void onServiceLost(NsdServiceInfo s){}public void onServiceFound(NsdServiceInfo s){ui.post(()->{if(scanning){nearby.add("wifi:"+s.getServiceName());showFound();}});}};nsd.discoverServices("_matterc._udp.",NsdManager.PROTOCOL_DNS_SD,discovery);}catch(RuntimeException ignored){}
 ui.postDelayed(finishScan,12000);}
 void stopScan(){scanning=false;ui.removeCallbacks(finishScan);if(scanner!=null){try{scanner.stopScan(ble);}catch(RuntimeException ignored){}scanner=null;}if(discovery!=null){try{nsd.stopServiceDiscovery(discovery);}catch(RuntimeException ignored){}discovery=null;}}
 void openQr(){stopScan();if(checkSelfPermission(android.Manifest.permission.CAMERA)!=0){requestPermissions(new String[]{android.Manifest.permission.CAMERA},8);return;}startActivityForResult(new Intent(this,Reader.class),11);}
 void pick(){stopScan();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,12);}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){if(r==7)startScan();else if(r==8){if(g.length>0&&g[0]==0)openQr();else found.setText("Puedes seleccionar una imagen del QR sin usar la cámara.");}else super.onRequestPermissionsResult(r,p,g);}
 @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(result!=RESULT_OK||data==null)return;if(r==11)auth(data.getStringExtra("qr"));if(r==12&&data.getData()!=null){found.setText("Leyendo imagen…");new Thread(()->{try{String value=Reader.image(this,data.getData());ui.post(()->auth(value));}catch(Exception e){ui.post(()->found.setText("No se reconoce un QR Matter en la imagen."));}}).start();}}
 void auth(String code){if(code==null||!code.startsWith("MT:")){found.setText("Ese QR no es de Matter. Busca el logotipo Matter junto al QR.");return;}
 new AlertDialog.Builder(this).setTitle("QR Matter reconocido").setItems(new String[]{"Bombilla nueva · configurar WiFi","Ya conectada · QR compartido"},(dlg,k)->{if(k==1)pair(code,null,true);else wifi(code);}).show();}
 void wifi(String code){LinearLayout form=new LinearLayout(this);form.setPadding(d(20),d(8),d(20),0);form.setOrientation(1);EditText ssid=new EditText(this),key=new EditText(this);ssid.setHint("Nombre de la red WiFi");ssid.setSingleLine();try{String n=((WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE)).getConnectionInfo().getSSID();if(n!=null&&!n.equals("<unknown ssid>"))ssid.setText(n.replace("\"",""));}catch(RuntimeException ignored){}
 key.setSingleLine();key.setInputType(129);key.setSaveEnabled(false);key.setHint("Contraseña WiFi");form.addView(ssid);form.addView(key);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Conectar a WiFi").setView(form).setPositiveButton("Vincular",null).setNegativeButton("Cancelar",null).create();dialog.show();dialog.getButton(-1).setOnClickListener(v->{if(ssid.length()==0){ssid.setError("Indica la red");return;}NetworkCredentials c=NetworkCredentials.forWiFi(new NetworkCredentials.WiFiCredentials(ssid.getText().toString(),key.getText().toString()));dialog.dismiss();pair(code,c,false);});}
 void pair(String code,NetworkCredentials cred,boolean network){if(pairing)return;try{stopScan();pairing=true;message("Vinculando la bombilla…");controller.pairDeviceWithCode(12345L,code,false,network,null,cred);}catch(RuntimeException e){pairing=false;message("No se pudo iniciar el alta: "+e.getMessage());}}
 @Override void connect(){if(saved()&&qr!=null){draw();qr=null;}super.connect();}
 @Override protected void onPause(){stopScan();super.onPause();}
}
