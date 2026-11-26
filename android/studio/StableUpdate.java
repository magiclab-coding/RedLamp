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
import org.json.JSONArray;

public final class StableUpdate extends BroadcastReceiver {
    private static final String REPO = "magiclab-coding/RedLamp";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases";
    private static final int MAX_APK_BYTES = 64 * 1024 * 1024;
    final String version, hash, assetUrl;
    final boolean preview;

    public StableUpdate() { this("", "", "", false); }

    private StableUpdate(String version, String hash, String assetUrl, boolean preview) {
        this.version = version;
        this.hash = hash;
        this.assetUrl = assetUrl;
        this.preview = preview;
    }

    private static HttpsURLConnection connect(String url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "RedLamp-Android");
        connection.setInstanceFollowRedirects(false);
        return connection;
    }

    private static long rank(String version) {
        if (!version.matches("[0-9]{1,6}[.][0-9]{1,6}")) return -1;
        int dot = version.indexOf('.');
        return Long.parseLong(version.substring(0, dot)) * 1000000L
                + Long.parseLong(version.substring(dot + 1));
    }

    static StableUpdate check(String installed, boolean includePreviews) throws Exception {
        StableUpdate best = null;
        long current = rank(installed);
        for (int page = 1; ; page++) {
            HttpsURLConnection connection = connect(API + "?per_page=50&page=" + page);
            try {
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                try (InputStream input = connection.getInputStream()) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        bytes.write(buffer, 0, read);
                        if (bytes.size() > 4000000) throw new IOException("Respuesta demasiado grande");
                    }
                    JSONArray releases = new JSONArray(bytes.toString("UTF-8"));
                    for (int i = 0; i < releases.length(); i++) {
                        JSONObject release = releases.getJSONObject(i);
                        boolean preview = release.getBoolean("prerelease");
                        if (release.getBoolean("draft") || (preview && !includePreviews)) continue;
                        String tag = release.getString("tag_name");
                        if (!tag.startsWith("v")) continue;
                        String version = tag.substring(1);
                        long candidate = rank(version);
                        if (candidate <= current) continue;
                        String name = "Luz-MOES-" + version + ".apk";
                        String url = "https://github.com/" + REPO + "/releases/download/" + tag + "/" + name;
                        JSONArray assets = release.getJSONArray("assets");
                        for (int j = 0; j < assets.length(); j++) {
                            JSONObject asset = assets.getJSONObject(j);
                            if (!name.equals(asset.optString("name")) || !url.equals(asset.optString("browser_download_url"))) continue;
                            String digest = asset.optString("digest");
                            if (!digest.matches("sha256:[a-f0-9]{64}")) throw new IOException("Firma de descarga no disponible");
                            best = new StableUpdate(version, digest.substring(7), url, preview);
                            current = candidate;
                        }
                    }
                    if (releases.length() < 50) return best;
                }
            } finally { connection.disconnect(); }
        }
    }

    private HttpsURLConnection download() throws IOException {
        String url = assetUrl;
        for (int i = 0; i < 5; i++) {
            HttpsURLConnection connection = connect(url);
            int status = connection.getResponseCode();
            if (status == 200) return connection;
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (status < 300 || status >= 400 || location == null) throw new IOException("Descarga HTTP " + status);
            URL next = new URL(new URL(url), location);
            String host = next.getHost();
            if (!"https".equals(next.getProtocol()) || !(host.equals("github.com") || host.endsWith(".githubusercontent.com"))) {
                throw new IOException("Destino de descarga inesperado");
            }
            url = next.toString();
        }
        throw new IOException("Demasiadas redirecciones");
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
            HttpsURLConnection connection = download();
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
