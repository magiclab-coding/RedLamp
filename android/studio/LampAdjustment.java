package es.luzlocal.matter;

/** State for one control. All calls run on the Android UI thread. */
final class LampAdjustment {
    static final long SETTLE_MS = 250;
    static final long VERIFY_MS = 5000;

    int revision;
    int percent;
    int target;
    int fade;
    int inFlight;
    boolean pending;
    long readableAt;
    private long lastRead;
    private Integer lastRaw;

    void request(int revision, int percent, int target, int fade) {
        this.revision = revision;
        this.percent = percent;
        this.target = target;
        this.fade = fade;
        pending = true;
        readableAt = Long.MAX_VALUE;
        lastRaw = null;
    }

    boolean complete(int revision, long now) {
        if (inFlight != revision) return false;
        inFlight = 0;
        if (this.revision != revision) return false;
        readableAt = now + 100L * fade + SETTLE_MS;
        return true;
    }

    boolean canRead(long now) {
        return !pending || now >= readableAt;
    }

    boolean read(int revision, long sequence, long startedAt, int raw, int measuredPercent) {
        if (this.revision != revision || sequence <= lastRead || !canRead(startedAt)) return false;
        lastRead = sequence;
        if (pending) {
            if (raw != target && startedAt - readableAt < VERIFY_MS) return false;
            pending = false;
            if (raw == target) {
                lastRaw = raw;
                return false;
            }
            percent = measuredPercent;
            lastRaw = raw;
            return true;
        }
        if (lastRaw == null || raw != lastRaw) percent = measuredPercent;
        lastRaw = raw;
        return false;
    }
}
