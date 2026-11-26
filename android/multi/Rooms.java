package es.luzlocal.matter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.*;

public final class Rooms extends Activity {
    private SharedPreferences rooms;
    private LinearLayout list;
    private static final String LEGACY="es.luzlocal.casa";
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    public void onCreate(Bundle state){super.onCreate(state);rooms=getSharedPreferences("rooms",MODE_PRIVATE);}
    protected void onResume(){super.onResume();render();}
    private GradientDrawable rounded(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));return d;}
    private TextView text(LinearLayout parent,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);parent.addView(t);return t;}
    private Button button(LinearLayout parent,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->action.run());parent.addView(b);return b;}
    private void render(){
        getWindow().setStatusBarColor(0xff111b22);getWindow().setNavigationBarColor(0xff111b22);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff111b22);
        list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(18),dp(24),dp(18),dp(24));scroll.addView(list);setContentView(scroll);
        text(list,"Mis bombillas",32,0xffffedcf).setTypeface(Typeface.DEFAULT_BOLD);
        Button add=button(list,"＋  Añadir bombilla",()->name(-1));add.setBackground(rounded(0xffffcd87));add.setTextColor(0xff17262b);
        if(getPackageManager().getLaunchIntentForPackage(LEGACY)!=null)row(0,"Vinculada en Casa 2.0");
        for(long id=1;id<rooms.getLong("next",1);id++)if(rooms.contains("name"+id)){
            boolean paired=getSharedPreferences("light-"+id,MODE_MULTI_PROCESS).getBoolean("paired",false);
            row(id,paired?"Control local Matter":"Configurar o conectar");
        }
    }
    private String title(long id){return rooms.getString("name"+id,id==0?"Mi primera bombilla":"Bombilla "+id);}
    private void row(long id,String subtitle){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),dp(14),dp(16),dp(10));box.setBackground(rounded(0xff23343c));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);list.addView(box,lp);
        text(box,title(id),22,0xffffefd9);text(box,subtitle,14,0xffbfcfce);
        LinearLayout actions=new LinearLayout(this);box.addView(actions);button(actions,"Abrir",()->open(id));button(actions,"Renombrar",()->name(id));box.setOnClickListener(v->open(id));
    }
    private void name(long id){
        EditText input=new EditText(this);input.setSingleLine();if(id>=0){input.setText(title(id));input.selectAll();}else input.setHint("Por ejemplo, lámpara del salón");
        AlertDialog d=new AlertDialog.Builder(this).setTitle(id<0?"Nueva bombilla":"Nombre de la bombilla").setView(input).setNegativeButton("Cancelar",null).setPositiveButton(id<0?"Añadir":"Guardar",null).create();d.show();
        d.getButton(-1).setOnClickListener(v->{String n=input.getText().toString().trim();if(n.isEmpty()){input.setError("Escribe un nombre");return;}long target=id<0?rooms.getLong("next",1):id;SharedPreferences.Editor e=rooms.edit().putString("name"+target,n);if(id<0)e.putLong("next",target+1);if(!e.commit()){input.setError("No se pudo guardar");return;}d.dismiss();render();if(id<0)open(target);});
    }
    private void open(long id){
        if(id==0){Intent legacy=getPackageManager().getLaunchIntentForPackage(LEGACY);if(legacy!=null)startActivity(legacy);return;}
        startActivity(new Intent().setClassName(this,"es.luzlocal.matter.Luz").putExtra("room",id).putExtra("roomName",title(id)));
    }
    public static long node(Activity a){long id=a.getIntent().getLongExtra("room",-1);if(id<1)throw new IllegalStateException("Selecciona una bombilla");return 12345L+id;}
    public static SharedPreferences light(Activity a,int mode){return a.getSharedPreferences("light-"+(node(a)-12345L),MODE_MULTI_PROCESS);}
    public static String selected(Activity a){return a.getIntent().getStringExtra("roomName");}
}
