package es.luzlocal.matter;
import android.app.*;
import android.content.*;
import android.os.*;
import android.webkit.*;
import android.widget.*;
import android.net.Uri;
import chip.devicecontroller.NetworkCredentials;
import chip.devicecontroller.ChipClusters.*;
import chip.devicecontroller.ChipStructs.GeneralDiagnosticsClusterNetworkInterface;
import org.json.*;
import java.util.*;

public final class Studio extends Luz {
 WebView web;final Handler main=new Handler(Looper.getMainLooper());final JSONObject facts=new JSONObject();
 String notice="";boolean ready,visible,wantAlarm,askedCamera;long expires;
 long retryAfter, readSequence, powerRead;
 final LampAdjustment brightnessAdjustment = new LampAdjustment();
 final LampAdjustment temperatureAdjustment = new LampAdjustment();
 final Runnable paint=new Runnable(){public void run(){if(visible){if(saved()&&!pairing&&!linking&&pointer==0&&SystemClock.elapsedRealtime()>=retryAfter){retryAfter=SystemClock.elapsedRealtime()+4000;connect();}sync();main.postDelayed(this,500);}}};
 SharedPreferences prefs(){return Rooms.light(this,0);}
 protected void onCreate(Bundle b){super.onCreate(b);wantAlarm=getIntent().hasExtra("due");expires=SystemClock.elapsedRealtime()+35000;}
 protected void onResume(){super.onResume();visible=true;main.removeCallbacks(paint);main.post(paint);}
 protected void onPause(){visible=false;main.removeCallbacks(paint);super.onPause();}
 @Override void draw(){super.draw();getWindow().setStatusBarColor(0xff08090c);getWindow().setNavigationBarColor(0xff08090c);
 if(web==null){web=new WebView(this);web.setBackgroundColor(0xff08090c);WebSettings opt=web.getSettings();opt.setJavaScriptEnabled(true);opt.setAllowContentAccess(false);opt.setAllowFileAccess(false);
 web.addJavascriptInterface(new Bridge(),"LampApp");web.setWebViewClient(new WebViewClient(){public boolean shouldOverrideUrlLoading(WebView w,String u){return true;}public void onPageFinished(WebView w,String u){ready=true;sync();}});web.loadUrl("file:///android_asset/studio.html");}setContentView(web);}
 @Override void openQr(){stopScan();if(checkSelfPermission(android.Manifest.permission.CAMERA)!=0&&!askedCamera){askedCamera=true;requestPermissions(new String[]{android.Manifest.permission.CAMERA},18);return;}startActivityForResult(new Intent(this,CameraPage.class),11);}
 public void onRequestPermissionsResult(int r,String[] p,int[] g){if(r==18)openQr();else super.onRequestPermissionsResult(r,p,g);}
 @Override void pair(String code,NetworkCredentials cred,boolean network){if(pairing||linking)return;stopScan();long id=saved()?(new java.security.SecureRandom().nextLong()&0x3fffffffffffffffL)+1:Rooms.node(this);
 prefs().edit().putLong("candidate",id).commit();try{pairing=true;if(pointer!=0){controller.releaseConnectedDevicePointer(pointer);pointer=0;endpoint=0;enable(false);}message("Vinculando…");controller.pairDeviceWithCode(id,code,false,network,null,cred);}catch(Exception e){pairing=false;message("Alta fallida: "+e.getMessage());}}
 void sharedCode(){EditText field=new EditText(this);field.setSingleLine();field.setHint("Pega el código temporal de Tuya");new AlertDialog.Builder(this).setTitle("Compartir mediante Matter").setMessage("Genera un código para otro ecosistema en Tuya, con ambos dispositivos en la misma WiFi.").setView(field).setNegativeButton("Cancelar",null).setPositiveButton("Vincular",(d,w)->{String c=field.getText().toString().trim();if(!c.startsWith("MT:"))c=c.replace(" ","").replace("-","");if(c.startsWith("MT:")||c.matches("[0-9]{11}|[0-9]{21}"))pair(c,null,true);else note("Código Matter no reconocido");}).show();}
 void note(String s){runOnUiThread(()->{notice=s;sync();});}
 void fact(String k,String v){runOnUiThread(()->{try{facts.put(k,v);}catch(Exception ignored){}sync();});}
 void diagnostics(){if(pointer==0)return;try{fact("IP local",controller.getIpAddress(Rooms.node(this)));BasicInformationCluster b=new BasicInformationCluster(pointer,0);
 b.readProductNameAttribute(str("Producto"));b.readSoftwareVersionStringAttribute(str("Firmware"));
 new WiFiNetworkDiagnosticsCluster(pointer,0).readRssiAttribute(new WiFiNetworkDiagnosticsCluster.RssiAttributeCallback(){public void onSuccess(Integer v){if(v!=null)fact("Señal WiFi",v+" dBm");}public void onError(Exception e){}});
 new GeneralDiagnosticsCluster(pointer,0).readNetworkInterfacesAttribute(new GeneralDiagnosticsCluster.NetworkInterfacesAttributeCallback(){public void onError(Exception e){}public void onSuccess(List<GeneralDiagnosticsClusterNetworkInterface> data){for(GeneralDiagnosticsClusterNetworkInterface n:data){if(!Boolean.TRUE.equals(n.isOperational)||n.hardwareAddress==null||n.hardwareAddress.length!=6)continue;StringBuilder mac=new StringBuilder();for(byte x:n.hardwareAddress){if(mac.length()>0)mac.append(':');mac.append(String.format(Locale.ROOT,"%02x",x&255));}fact("MAC",mac.toString());break;}}});}catch(Exception e){note("Diagnóstico no disponible");}}
 CharStringAttributeCallback str(String k){return new CharStringAttributeCallback(){public void onError(Exception e){}public void onSuccess(String s){fact(k,s);}};}
 void sync(){if(!ready||web==null)return;try{JSONObject s=new JSONObject();s.put("name",Rooms.selected(this));s.put("status",pointer!=0&&power.isEnabled()?(lit?"Conectada · Encendida":"Conectada · Apagada"):(saved()?"Buscando la bombilla en la WiFi…":"Pendiente de vincular"));if(endpoint>0&&prefs().getInt("ep",0)!=endpoint)prefs().edit().putInt("ep",endpoint).apply();s.put("scan",found.getText().toString());s.put("online",pointer!=0&&power.isEnabled());s.put("paired",saved());s.put("on",lit);s.put("b",brightnessAdjustment.percent);s.put("br",brightnessAdjustment.revision);s.put("t",temperatureAdjustment.percent);s.put("tr",temperatureAdjustment.revision);s.put("k",kelvinAt(temperatureAdjustment.percent));s.put("kw",kelvinAt(0));s.put("kc",kelvinAt(100));s.put("note",notice);s.put("facts",facts);s.put("node",Long.toString(Rooms.node(this)));s.put("zone",TimeZone.getDefault().getID());s.put("fade",prefs().getInt("fade",2));s.put("schedule",prefs().getLong("due",0));s.put("schOn",prefs().getBoolean("schOn",false));s.put("mode",prefs().getString("mode",""));s.put("db",prefs().getInt("db",100));s.put("dt",prefs().getInt("dt",40));web.evaluateJavascript("window.update("+s.toString()+")",null);}catch(Exception ignored){}
 if(wantAlarm){long due=getIntent().getLongExtra("due",0);if(due!=prefs().getLong("due",0)||!allowed()){wantAlarm=false;return;}if(System.currentTimeMillis()-due>120000||SystemClock.elapsedRealtime()>expires){wantAlarm=false;prefs().edit().remove("due").commit();note("Horario vencido sin conexión");}else if(pointer!=0&&power.isEnabled()){wantAlarm=false;DefaultClusterCallback ack=cb(()->{if(prefs().getLong("due",0)==due)prefs().edit().remove("due").apply();note("Acción confirmada");refresh();});OnOffCluster lamp=new OnOffCluster(pointer,endpoint);if(prefs().getBoolean("schOn",false))lamp.on(ack);else lamp.off(ack);}}}
 boolean allowed(){long id=getIntent().getLongExtra("room",-1);SharedPreferences p=getSharedPreferences("rooms",MODE_MULTI_PROCESS);return p.contains("name"+id);}
 float kelvinValue(int n) { return 2700 + percent(n) * 38; }
 int kelvinAt(int n) { return Math.round(kelvinValue(n)); }
 int miredAt(int n) { return Math.round(1000000f / kelvinValue(n)); }
 int percent(int n) { return Math.max(0, Math.min(100, n)); }
 boolean sameDevice(long ptr, int ep) { return ptr != 0 && ptr == pointer && ep == endpoint; }

 @Override void refresh() {
     if (Looper.myLooper() != Looper.getMainLooper()) {
         main.post(this::refresh);
         return;
     }
     long ptr = pointer;
     int ep = endpoint;
     if (ptr == 0 || ep == 0) return;
     long sequence = ++readSequence;
     new OnOffCluster(ptr, ep).readOnOffAttribute(new BooleanAttributeCallback() {
         public void onSuccess(boolean value) {
             main.post(() -> {
                 if (!sameDevice(ptr, ep) || sequence <= powerRead) return;
                 powerRead = sequence;
                 lit = value;
                 power.setText(lit ? "Apagar" : "Encender");
                 enable(true);
                 message(lit ? "Encendida" : "Apagada");
                 sync();
             });
         }
         public void onError(Exception e) {
             main.post(() -> {
                 if (!sameDevice(ptr, ep) || sequence <= powerRead) return;
                 powerRead = sequence;
                 lost(e);
             });
         }
     });
     readAdjustment(brightnessAdjustment, ptr, ep, sequence);
     readAdjustment(temperatureAdjustment, ptr, ep, sequence);
 }

 void readAdjustment(LampAdjustment adjustment, long ptr, int ep, long sequence) {
     long started = SystemClock.elapsedRealtime();
     if (!adjustment.canRead(started)) return;
     int revision = adjustment.revision;
     boolean isLevel = adjustment == brightnessAdjustment;
     java.util.function.Consumer<Integer> receive = raw -> main.post(() -> {
         if (!sameDevice(ptr, ep) || raw == null || (!isLevel && raw <= 0)) return;
         int measured = isLevel
             ? Math.round(100f * (raw - minLevel) / Math.max(1, maxLevel - minLevel))
             : Math.round(100f * (1000000f / raw - kelvinValue(0)) / (kelvinValue(100) - kelvinValue(0)));
         if (adjustment.read(revision, sequence, started, raw, percent(measured))) {
             notice = isLevel ? "Intensidad solicitada sin confirmar" : "Temperatura solicitada sin confirmar";
         }
         sync();
     });
     if (isLevel) {
         new LevelControlCluster(ptr, ep).readCurrentLevelAttribute(new LevelControlCluster.CurrentLevelAttributeCallback() {
             public void onSuccess(Integer value) { receive.accept(value); }
             public void onError(Exception ignored) {}
         });
     } else {
         new ColorControlCluster(ptr, ep).readColorTemperatureMiredsAttribute(new IntegerAttributeCallback() {
             public void onSuccess(int value) { receive.accept(value); }
             public void onError(Exception ignored) {}
         });
     }
 }
 int levelAt(int n){return minLevel+Math.round((maxLevel-minLevel)*n/100f);}
 DefaultClusterCallback cb(Runnable done){return new DefaultClusterCallback(){public void onSuccess(){runOnUiThread(done);}public void onError(Exception e){String m=String.valueOf(e.getMessage());note(m.toLowerCase(Locale.ROOT).contains("timeout")?"Sin respuesta de la bombilla. El cambio no está confirmado.":"Ajuste sin confirmar · "+m);}};}
 void adjust(LampAdjustment adjustment, int selected, int revision) {
     if (revision <= adjustment.revision) return;
     int value = percent(selected);
     if (pointer == 0 || !power.isEnabled()) {
         adjustment.revision = revision;
         adjustment.pending = false;
         note("Bombilla sin conexión");
         return;
     }
     int raw = adjustment == brightnessAdjustment ? levelAt(value) : miredAt(value);
     adjustment.request(revision, value, raw, prefs().getInt("fade", 2));
     if (adjustment.inFlight == 0) sendAdjustment(adjustment);
 }

 void sendAdjustment(LampAdjustment adjustment) {
     long ptr = pointer;
     int ep = endpoint;
     int revision = adjustment.revision;
     adjustment.inFlight = revision;
     DefaultClusterCallback reply = new DefaultClusterCallback() {
         public void onSuccess() { main.post(() -> finishAdjustment(adjustment, revision, ptr, ep, null)); }
         public void onError(Exception e) { main.post(() -> finishAdjustment(adjustment, revision, ptr, ep, e)); }
     };
     try {
         if (!sameDevice(ptr, ep)) throw new IllegalStateException("Bombilla sin conexión");
         if (adjustment == brightnessAdjustment) {
             new LevelControlCluster(ptr, ep).moveToLevel(reply, adjustment.target, adjustment.fade, 1, 1);
         } else {
             new ColorControlCluster(ptr, ep).moveToColorTemperature(reply, adjustment.target, adjustment.fade, 1, 1);
         }
     } catch (Exception e) {
         reply.onError(e);
     }
 }

 void finishAdjustment(LampAdjustment adjustment, int revision, long ptr, int ep, Exception error) {
     if (adjustment.inFlight != revision) return;
     long now = SystemClock.elapsedRealtime();
     if (!adjustment.complete(revision, now)) {
         if (adjustment.pending) sendAdjustment(adjustment);
         return;
     }
     if (error != null || !sameDevice(ptr, ep)) note("Sin confirmación de la bombilla");
     main.postDelayed(() -> {
         if (adjustment.revision == revision && visible) refresh();
     }, adjustment.readableAt - now);
 }
 void startup(String mode,int b,int t){if(pointer==0)return;final long ptr=pointer;final int ep=endpoint;boolean memory=mode.equals("memory");DefaultClusterCallback finish=cb(()->{prefs().edit().putString("mode",mode).putInt("dt",t).putInt("db",b).apply();note("La bombilla ha confirmado los tres ajustes de arranque");});
 new LevelControlCluster(ptr,ep).writeStartUpCurrentLevelAttribute(cb(()->{if(pointer!=ptr)return;new ColorControlCluster(ptr,ep).writeStartUpColorTemperatureMiredsAttribute(cb(()->{if(pointer!=ptr)return;new OnOffCluster(ptr,ep).writeStartUpOnOffAttribute(finish,memory?null:Integer.valueOf(1));}),memory?null:Integer.valueOf(miredAt(t)));}),memory?null:Integer.valueOf(levelAt(b)));}
 static PendingIntent alarm(Context c,long id,String name,long due,int flags){Intent i=new Intent(c,Studio.class).setData(Uri.parse("moes-light://alarm/"+id)).putExtra("room",id).putExtra("roomName",name).putExtra("due",due);return PendingIntent.getActivity(c,0,i,flags|PendingIntent.FLAG_IMMUTABLE);}
 static void cancelAlarm(Context c,long id){PendingIntent pi=alarm(c,id,"",0,PendingIntent.FLAG_NO_CREATE);if(pi!=null){((AlarmManager)c.getSystemService(ALARM_SERVICE)).cancel(pi);pi.cancel();}c.getSharedPreferences("light-"+id,MODE_MULTI_PROCESS).edit().remove("due").commit();}
 void schedule(int minute,boolean on){long id=getIntent().getLongExtra("room",-1);cancelAlarm(this,id);if(minute<0){note("Horario cancelado");return;}Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,minute/60);c.set(Calendar.MINUTE,minute%60);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DATE,1);setDue(id,c.getTimeInMillis(),on);} void setDue(long id,long due,boolean on){prefs().edit().putLong("due",due).putBoolean("schOn",on).commit();((AlarmManager)getSystemService(ALARM_SERVICE)).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,alarm(this,id,Rooms.selected(this),due,PendingIntent.FLAG_UPDATE_CURRENT));note("Horario guardado en este móvil");}
 final class Bridge{@JavascriptInterface public void act(String key,int a,int b,String arg){runOnUiThread(()->{if(!allowed()){finish();return;}try{notice="";switch(key){
 case "back":finish();break;case "scan":openQr();break;case "share":sharedCode();break;case "connect":if(saved())connect();else startScan();break;
 case "info":diagnostics();break;case "copy":((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Luz MOES",arg));note("Copiado");break;
 case "power":send(0,lit?0:1);break;case "brightness":adjust(brightnessAdjustment,a,b);break;case "temp":adjust(temperatureAdjustment,a,b);break;
 case "fade":prefs().edit().putInt("fade",Math.max(0,Math.min(100,a))).apply();note("Transición guardada");break;
 case "default":startup(arg,a,b);break;case "quiet":if(pointer!=0)new OnOffCluster(pointer,endpoint).writeStartUpOnOffAttribute(cb(()->{prefs().edit().remove("mode").apply();note("Arranque confirmado por la bombilla");}),a==1?Integer.valueOf(0):null);break;
 case "schedule":schedule(a,b==1);break;case "timer":setDue(getIntent().getLongExtra("room",-1),System.currentTimeMillis()+60000L*Math.max(1,a),b==1);break;
 }sync();}catch(Exception ex){note("No se pudo aplicar: "+ex.getMessage());}});}}
 @Override public void onBackPressed(){if(web==null){super.onBackPressed();return;}web.evaluateJavascript("window.goBack()",v->{if(!"true".equals(v))finish();});}
}
