package es.luzlocal.matter;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

public final class StableUpdate extends BroadcastReceiver {
    private static final String ORIGIN = "https://luz-moes-android.martint-rubens.chatgpt.site/";
    private static final int MAX_APK_BYTES = 64 * 1024 * 1024;
    final String version;
    final int code;
    final String hash;

    public StableUpdate() {
        this("", 0, "");
    }

    private StableUpdate(String version, int code, String hash) {
        this.version = version;
        this.code = code;
        this.hash = hash;
    }

    private static HttpsURLConnection connect(String file) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(ORIGIN + file).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(12000);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        return connection;
    }

    static StableUpdate check(int installedCode) throws Exception {
        HttpsURLConnection connection = connect("stable.json");
        try (InputStream input = connection.getInputStream()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                bytes.write(buffer, 0, read);
                if (bytes.size() > 4096) throw new IOException("Metadatos demasiado grandes");
            }
            JSONObject release = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            String version = release.getString("versionName");
            int code = release.getInt("versionCode");
            String hash = release.getString("sha256");
            if (!version.matches("[1-9][0-9]{0,5}\\.0") || code <= 0 || !hash.matches("[a-f0-9]{64}")) {
                throw new IOException("Metadatos de actualización inválidos");
            }
            return code > installedCode ? new StableUpdate(version, code, hash) : null;
        } finally {
            connection.disconnect();
        }
    }

    void install(Context context) throws Exception {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(context.getPackageName());
        int id = installer.createSession(params);
        boolean committed = false;
        try (PackageInstaller.Session session = installer.openSession(id)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            HttpsURLConnection connection = connect("Luz-MOES-" + version + ".apk");
            try (InputStream input = connection.getInputStream();
                 OutputStream output = session.openWrite("base.apk", 0, -1)) {
                byte[] buffer = new byte[16384];
                int size = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    size += read;
                    if (size > MAX_APK_BYTES) throw new IOException("APK demasiado grande");
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
                byte[] expected = new byte[32];
                for (int i = 0; i < expected.length; i++) {
                    expected[i] = (byte) Integer.parseInt(hash.substring(i * 2, i * 2 + 2), 16);
                }
                if (!MessageDigest.isEqual(digest.digest(), expected)) {
                    throw new IOException("La descarga no coincide con la APK publicada");
                }
                session.fsync(output);
            } finally {
                connection.disconnect();
            }
            Intent callback = new Intent(context, StableUpdate.class);
            // PackageInstaller adds the status and confirmation intent to this callback.
            PendingIntent result = PendingIntent.getBroadcast(context, id, callback,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(result.getIntentSender());
            committed = true;
        } finally {
            if (!committed) installer.abandonSession(id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation != null) context.startActivity(confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (status != PackageInstaller.STATUS_SUCCESS && status != PackageInstaller.STATUS_FAILURE_ABORTED) {
            Toast.makeText(context, "Android no pudo instalar la actualización", Toast.LENGTH_LONG).show();
        }
    }
}
