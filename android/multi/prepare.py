from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path("decoded")
android = "{http://schemas.android.com/apk/res/android}"
ET.register_namespace("android", android[1:-1])
m = ET.parse(root / "AndroidManifest.xml")
m.getroot().set("package", "es.luzlocal.multi")
app = m.find("application")
detail = app.find("activity")
selector = ET.SubElement(app,"activity",{android+"name":"es.luzlocal.matter.Rooms",android+"exported":"true"})
intent = detail.find("intent-filter")
detail.remove(intent)
selector.append(intent)
detail.set(android+"exported","false")
detail.set(android+"process",":controller")
m.write(root/"AndroidManifest.xml",encoding="utf-8",xml_declaration=True)
for p,old,new in [(root/"apktool.yml","versionCode: 3","versionCode: 4"),(root/"apktool.yml","versionName: 2.0","versionName: 2.1"),(root/"res/values/strings.xml","Luz MOES Casa","Luz MOES · Mis bombillas")]:
    s=p.read_text()
    assert old in s
    p.write_text(s.replace(old,new))
helper="Les/luzlocal/matter/Rooms;"
lamp=root/"smali/es/luzlocal/matter/Lamp.smali"
luz=root/"smali_classes2/es/luzlocal/matter/Luz.smali"
for p,n in [(lamp,2),(luz,1)]:
    s,count=re.subn(r"const-wide/16 (v[0-9]+), 0x3039",rf"invoke-static/range {{p0 .. p0}}, {helper}->node(Landroid/app/Activity;)J\n    move-result-wide \1",p.read_text())
    assert count==n
    if p==lamp:
        s,count=re.subn(r"invoke-virtual \{p0, v0\}, Les/luzlocal/matter/Lamp;->getPreferences\(I\)",f"invoke-static {{p0, v0}}, {helper}->light(Landroid/app/Activity;I)",s)
        assert count==2
        s=s.replace("SharedPreferences$Editor;->apply()V","SharedPreferences$Editor;->commit()Z")
    else:
        s=s.replace('const-string v3, "Luz MOES"',f'invoke-static/range {{p0 .. p0}}, {helper}->selected(Landroid/app/Activity;)Ljava/lang/String;\n    move-result-object v3')
        # Give each control session its own native callbacks and stop its timers
        # when returning to the picker, which lives in a separate process.
        s+="""
.method protected onDestroy()V
    .locals 1
    invoke-super {p0}, Les/luzlocal/matter/Lamp;->onDestroy()V
    invoke-virtual {p0}, Landroid/app/Activity;->isFinishing()Z
    move-result v0
    if-eqz v0, :done
    invoke-static {}, Landroid/os/Process;->myPid()I
    move-result v0
    invoke-static {v0}, Landroid/os/Process;->killProcess(I)V
    :done
    return-void
.end method
"""
    p.write_text(s)
