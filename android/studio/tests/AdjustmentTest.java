package es.luzlocal.matter;

public final class AdjustmentTest {
    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        LampAdjustment level = new LampAdjustment();
        LampAdjustment temp = new LampAdjustment();
        level.read(0, 1, 100, 50, 20);
        temp.read(0, 1, 100, 330, 10);
        level.request(1, 80, 203, 100);
        level.inFlight = 1;
        expect(!level.canRead(4000) && level.percent == 80, "Wait for command acknowledgement");
        expect(level.complete(1, 4500), "Acknowledge latest command");
        level.read(0, 2, 200, 50, 20);
        level.read(1, 3, 14499, 190, 75);
        expect(level.percent == 80 && level.pending, "Ignore readings before and during transition");
        expect(!level.canRead(14749) && level.canRead(14750), "Wait 10 seconds plus settling time after acknowledgement");
        level.read(1, 4, 14750, 200, 79);
        expect(level.percent == 80 && level.pending, "Ignore an intermediate sample after settling");
        expect(temp.percent == 10 && temp.canRead(5000), "A level adjustment cannot block temperature reads");
        level.read(1, 6, 15000, 203, 81);
        level.read(1, 5, 14800, 50, 20);
        level.read(1, 7, 15100, 203, 81);
        expect(level.percent == 80 && !level.pending, "Ignore out-of-order samples and preserve the requested percentage after quantization");
        level.read(1, 8, 16000, 60, 24);
        expect(level.percent == 24, "Honor a subsequent independent adjustment");

        temp.request(1, 70, 190, 100);
        temp.inFlight = 1;
        temp.request(2, 30, 260, 100);
        expect(!temp.complete(1, 18000) && temp.inFlight == 0 && temp.percent == 30, "Older acknowledgement cannot settle the new target");
        temp.inFlight = 2;
        expect(temp.complete(2, 18100), "Acknowledge the latest temperature");
        temp.read(2, 9, 28200, 190, 70);
        expect(temp.percent == 30 && temp.pending, "Ignore an old temperature during the new transition");
        expect(!temp.read(2, 10, 33349, 190, 70) && temp.pending, "Wait for a confirming sample");
        expect(temp.read(2, 11, 33350, 190, 70) && temp.percent == 70, "Report measured value after failed confirmation");
        temp.request(3, 60, 210, 0);
        temp.inFlight = 3;
        expect(temp.complete(3, 33500) && temp.readableAt == 33750, "Instantaneous mode still allows the device to settle");
        temp.read(3, 12, 33750, 210, 59);
        expect(temp.percent == 60 && !temp.pending, "Confirm instantaneous temperature");
        System.out.println("PASS: Matter adjustment state");
    }
}
