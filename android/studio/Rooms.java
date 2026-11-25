package es.luzlocal.matter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.*;

public final class Rooms extends Luz {
    private SharedPreferences rooms;
    final android.os.Handler handler=new android.os.Handler();
    final Map<Long,TextView> icons=new HashMap<>(), labels=new HashMap<>();
    final Set<Long> busy=new HashSet<>();
    final Map<Long,Boolean> states=new HashMap<>();
    final Set<Long> pending=new HashSet<>();boolean active;
    void draw(){} void setup(){} boolean saved(){return false;} void message(String msg){}
    final Runnable poll=new Runnable(){public void run(){if(!active)return;for(Long id:new ArrayList<>(icons.keySet()))query(id,false);handler.postDelayed(this,4000);}};
    protected void onPause(){active=false;handler.removeCallbacks(poll);super.onPause();}
    void result(long id,Boolean value,long ptr){
        runOnUiThread(()->{if(ptr!=0)controller.releaseConnectedDevicePointer(ptr);busy.remove(id);states.put(id,value);display(id);if(pending.remove(id)&&active&&icons.containsKey(id))query(id,true);});
    }
    void display(long id){TextView icon=icons.get(id),label=labels.get(id);if(icon==null)return;
        Boolean v=states.get(id);boolean on=Boolean.TRUE.equals(v);
        icon.setTextColor(v==null?0xff86939b:on?0xffffcd87:0xff67cfcb);icon.setShadowLayer(on?8:0,0,0,0xffffbc68);
        icon.setContentDescription(v==null?"Sin conexión":on?"Apagar":"Encender");label.setText(v==null?"Buscando en la WiFi…":on?"Conectada · Encendida":"Conectada · Apagada");}
    void query(long id,boolean toggle){if(controller==null)return;if(!busy.add(id)){if(toggle)pending.add(id);return;}
        SharedPreferences pref=getSharedPreferences("light-"+id,MODE_MULTI_PROCESS);
        try{controller.getConnectedDevicePointer(pref.getLong("node",12345L+id),new chip.devicecontroller.GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback(){
            public void onConnectionFailure(long node,Exception e){result(id,null,0);}
            public void onDeviceConnected(long ptr){chip.devicecontroller.ChipClusters.OnOffCluster c=new chip.devicecontroller.ChipClusters.OnOffCluster(ptr,pref.getInt("ep",1));
                Runnable read=()->c.readOnOffAttribute(new chip.devicecontroller.ChipClusters.BooleanAttributeCallback(){public void onSuccess(boolean v){result(id,v,ptr);}public void onError(Exception e){result(id,null,ptr);}});
                if(toggle)c.toggle(new chip.devicecontroller.ChipClusters.DefaultClusterCallback(){public void onSuccess(){read.run();}public void onError(Exception e){result(id,null,ptr);}});else read.run();
            }});}catch(Exception e){result(id,null,0);}
    }
    private LinearLayout list;
    private static final String LEGACY="es.luzlocal.casa";
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    public void onCreate(Bundle state){super.onCreate(state);rooms=getSharedPreferences("rooms",MODE_PRIVATE);}
    protected void onResume(){super.onResume();active=true;states.clear();render();handler.post(poll);}
    private GradientDrawable rounded(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));return d;}
    private TextView rowText(LinearLayout parent,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);parent.addView(t);return t;}
    private Button button(LinearLayout parent,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->action.run());parent.addView(b);return b;}
    private void render(){
        getWindow().setStatusBarColor(0xff111b22);getWindow().setNavigationBarColor(0xff111b22);
        icons.clear();labels.clear();ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff111b22);
        list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(18),dp(24),dp(18),dp(24));scroll.addView(list);setContentView(scroll);
        rowText(list,"Mis bombillas",32,0xffffedcf).setTypeface(Typeface.DEFAULT_BOLD);
        Button add=button(list,"＋  Añadir bombilla",()->name(-1));add.setBackground(rounded(0xffffcd87));add.setTextColor(0xff17262b);
        if(!rooms.getBoolean("hideLegacy",false)&&getPackageManager().getLaunchIntentForPackage(LEGACY)!=null)row(0,"Acceso a Casa 2.0");
        for(long id=1;id<rooms.getLong("next",1);id++)if(rooms.contains("name"+id)){
            boolean paired=getSharedPreferences("light-"+id,MODE_MULTI_PROCESS).getBoolean("paired",false);
            row(id,paired?"Control local Matter":"Configurar o conectar");
        }
    }
    class Bulb extends TextView{
        final android.graphics.Paint brush=new android.graphics.Paint(1);
        Bulb(){super(Rooms.this);}
        protected void onDraw(android.graphics.Canvas canvas){canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);float scale=dp(1)*1f;scale=getResources().getDisplayMetrics().density;canvas.scale(scale,scale);
            int color=getCurrentTextColor();brush.setColor(color);brush.setStrokeWidth(2);brush.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            brush.setStyle(color==0xffffcd87?android.graphics.Paint.Style.FILL:android.graphics.Paint.Style.STROKE);
            canvas.drawCircle(0,-4,9,brush);brush.setStyle(android.graphics.Paint.Style.STROKE);canvas.drawLine(-4,7,4,7,brush);canvas.drawLine(-2,11,2,11,brush);
            if(color==0xffffcd87){for(int a=0;a<5;a++){canvas.save();canvas.rotate(45*a-90,0,-4);canvas.drawLine(0,-17,0,-20,brush);canvas.restore();}}canvas.restore();}
    }
    private String title(long id){return rooms.getString("name"+id,id==0?"Mi primera bombilla":"Bombilla "+id);}
    private void row(long id,String subtitle){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),dp(14),dp(16),dp(10));box.setBackground(rounded(0xff23343c));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);list.addView(box,lp);
        LinearLayout line=new LinearLayout(this);line.setGravity(16);box.addView(line);
        LinearLayout name=new LinearLayout(this);name.setOrientation(1);line.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        rowText(name,title(id),22,0xffffefd9);TextView sub=rowText(name,subtitle,14,0xffbfcfce);
        if(id>0&&getSharedPreferences("light-"+id,MODE_MULTI_PROCESS).getBoolean("paired",false)){
            TextView bulb=new Bulb();line.addView(bulb);bulb.setGravity(17);bulb.setLayoutParams(new LinearLayout.LayoutParams(dp(62),dp(60)));bulb.setBackground(rounded(0xff18292f));bulb.setOnClickListener(v->query(id,true));icons.put(id,bulb);labels.put(id,sub);display(id);}
        
        TextView options = new TextView(this);
        options.setText("⋮");
        options.setTextSize(26);
        options.setTextColor(0xffd8e8e7);
        options.setGravity(Gravity.CENTER);
        options.setContentDescription("Opciones de " + title(id));
        options.setFocusable(true);
        line.addView(options, new LinearLayout.LayoutParams(dp(48), dp(54)));
        options.setOnClickListener(v -> showOptions(id, v));
        box.setOnClickListener(v -> open(id));
    }

    private void showOptions(long id, View anchor) {
        boolean locked = rooms.getBoolean("lock" + id, false);
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(4), dp(5), dp(4), dp(5));
        int width = Math.min(dp(290), getResources().getDisplayMetrics().widthPixels - dp(36));
        PopupWindow popup = new PopupWindow(menu, width, -2, true);
        popup.setBackgroundDrawable(rounded(0xdf30454f));
        popup.setElevation(dp(7));
        popup.setOutsideTouchable(true);
        if (!locked) menuItem(menu, popup, "✎", "Renombrar", () -> name(id));
        menuItem(menu, popup, locked ? "🔓" : "🔒",
            locked ? "Quitar protección de borrado" : "Proteger contra borrado", () -> {
                rooms.edit().putBoolean("lock" + id, !locked).commit();
                render();
            });
        if (!locked) {
            menuItem(menu, popup, "✕", "Borrar de este móvil", () -> confirmRemoval(id));
            if (id == 0) menuItem(menu, popup, "＋", "Vincular al panel nuevo", () -> name(-1));
        }
        popup.showAsDropDown(anchor, 0, -dp(4), Gravity.END);
    }

    private void menuItem(LinearLayout menu, PopupWindow popup, String icon, String label, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(48));
        row.setPadding(dp(8), dp(6), dp(10), dp(6));
        row.setFocusable(true);
        row.setContentDescription(label);
        TextView symbol = new TextView(this);
        symbol.setText(icon);
        symbol.setTextSize(19);
        symbol.setTextColor(0xffa7dad8);
        symbol.setGravity(Gravity.CENTER);
        symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(symbol, new LinearLayout.LayoutParams(dp(34), -2));
        TextView text = rowText(row, label, 16, 0xfff6f5ef);
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.setOnClickListener(v -> { popup.dismiss(); action.run(); });
        row.setOnFocusChangeListener((v, focused) -> v.setBackground(focused ? rounded(0x3375c9cf) : null));
        menu.addView(row);
    }

    private void confirmRemoval(long id) {
        new AlertDialog.Builder(this).setTitle("¿Borrar " + title(id) + "?")
            .setMessage("Se quitará de esta lista. Los demás controladores seguirán vinculados.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Borrar", (dialog, button) -> {
                Studio.cancelAlarm(this, id);
                if (id == 0) rooms.edit().putBoolean("hideLegacy", true).commit();
                else {
                    rooms.edit().remove("name" + id).remove("lock" + id).commit();
                    getSharedPreferences("light-" + id, MODE_MULTI_PROCESS).edit().clear().commit();
                }
                render();
            }).show();
    }
    private void name(long id){
        EditText input=new EditText(this);input.setSingleLine();if(id>=0){input.setText(title(id));input.selectAll();}else input.setHint("Por ejemplo, lámpara del salón");
        AlertDialog d=new AlertDialog.Builder(this).setTitle(id<0?"Nueva bombilla":"Nombre de la bombilla").setView(input).setNegativeButton("Cancelar",null).setPositiveButton(id<0?"Añadir":"Guardar",null).create();d.show();
        d.getButton(-1).setOnClickListener(v->{String n=input.getText().toString().trim();if(n.isEmpty()){input.setError("Escribe un nombre");return;}long target=id<0?rooms.getLong("next",1):id;SharedPreferences.Editor e=rooms.edit().putString("name"+target,n);if(id<0)e.putLong("next",target+1);if(!e.commit()){input.setError("No se pudo guardar");return;}d.dismiss();render();if(id<0)open(target);});
    }
    private void open(long id){
        if(id==0){Intent legacy=getPackageManager().getLaunchIntentForPackage(LEGACY);if(legacy!=null)startActivity(legacy);return;}
        startActivity(new Intent(this,Studio.class).putExtra("room",id).putExtra("roomName",title(id)));
    }
    public static long node(Activity a){long id=a.getIntent().getLongExtra("room",-1);if(id<1)throw new IllegalStateException("Selecciona una bombilla");return light(a,0).getLong("node",12345L+id);}
    public static SharedPreferences light(Activity a,int mode){return a.getSharedPreferences("light-"+a.getIntent().getLongExtra("room",-1),MODE_MULTI_PROCESS);}
    public static void paired(Activity a){SharedPreferences p=light(a,0);if(p.contains("candidate"))p.edit().putLong("node",p.getLong("candidate",0)).remove("candidate").commit();}
    public static String selected(Activity a){return a.getIntent().getStringExtra("roomName");}
}
