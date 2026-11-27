import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Have to run from the root instead of inside the repo
 *   javac -d out src/HomeClock.java tests/HomeClockTest.java
 *   java -cp out HomeClockTest
 * Date configured: 2026-11-16 is a Monday.
 * 
 * The program on its own shows errors around HomeClock and
 * thus it needs to be run together with HomeClock to actually work.
 */
public class HomeClockTest {

    static int total = 0;
    static int failed = 0;

    // Test Schedule
    static final List<HomeClock.Window> W = HomeClock.parseWindows(List.of(
            "me  class 10:00 14:30 Monday",
            "me  sleep 22:30 06:30 Monday",     // crosses midnight
            "mom sleep 21:00 24:00 all",        // 24:00 = at the end of day
            "mom sleep 00:00 05:30 weekday",
            "mom work  10:30 13:30 all"));

    static ZonedDateTime indy(String iso) {
        return LocalDateTime.parse(iso).atZone(HomeClock.INDY);
    }
    static ZonedDateTime ist(String iso) {
        return LocalDateTime.parse(iso).atZone(HomeClock.IST);
    }

    // Comparing the window's label to what is expected
    static void expect(String name, String expected, String who, ZonedDateTime t) {
        HomeClock.Window w = HomeClock.busyWindow(W, who, t);
        String actual = (w == null) ? null : w.label();
        total++;

        if (Objects.equals(expected, actual)) {
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + "  expected=" + expected + " actual=" + actual);
        }
    }

    static Clock clockAtIndy(String iso) {
        return Clock.fixed(indy(iso).toInstant(), ZoneOffset.UTC);
    }

    // Checking verdict at a set Indy time
    static void expectVerdict(String name, boolean good, String reason, String indyIso) {
        HomeClock.Verdict v = HomeClock.verdict(W, clockAtIndy(indyIso));
        total++;

        if (v.good() == good && v.reason().equals(reason)) {
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + "  expected=" + good + "/" + reason + " actual=" + v.good() + "/" + v.reason());
        }
    }

    static void check(String name, boolean ok) {
        total++;
        if (ok) {
            System.out.println("PASS  " + name);
        } else { 
            failed++; System.out.println("FAIL  " + name); 
        }
    }

    public static void main(String[] args) {
        // testing same day window where start is inclusive and end is exclusive
        expect("me Mon 09:59 free before class",  null,    "me", indy("2026-11-16T09:59"));
        expect("me Mon 10:00 class starts",       "class", "me", indy("2026-11-16T10:00"));
        expect("me Mon 14:29 still in class",     "class", "me", indy("2026-11-16T14:29"));
        expect("me Mon 14:30 class over",         null,    "me", indy("2026-11-16T14:30"));

        // checks the day and tries to filter it out
        expect("me Wed 10:30 no class on Wed",    null,    "me", indy("2026-11-18T10:30"));

        // window crossing midnight (22:30 -> 06:30)
        expect("me Mon 22:29 free",               null,    "me", indy("2026-11-16T22:29"));
        expect("me Mon 22:30 sleep starts",       "sleep", "me", indy("2026-11-16T22:30"));
        expect("me Mon 23:59 asleep",             "sleep", "me", indy("2026-11-16T23:59"));
        expect("me Tue 00:00 still asleep",       "sleep", "me", indy("2026-11-17T00:00"));
        expect("me Tue 06:29 still asleep",       "sleep", "me", indy("2026-11-17T06:29"));
        expect("me Tue 06:30 awake",              null,    "me", indy("2026-11-17T06:30"));
        expect("me Tue 22:30 no Tue window",      null,    "me", indy("2026-11-17T22:30"));

        // ends at 24:00 (stored as 00:00) and weekday/all test
        expect("mom Mon 20:59 free",              null,    "mom", ist("2026-11-16T20:59"));
        expect("mom Mon 21:00 asleep",            "sleep", "mom", ist("2026-11-16T21:00"));
        expect("mom Mon 23:59 asleep",            "sleep", "mom", ist("2026-11-16T23:59"));
        expect("mom Tue 00:00 weekday sleep",     "sleep", "mom", ist("2026-11-17T00:00"));
        expect("mom Tue 05:29 asleep",            "sleep", "mom", ist("2026-11-17T05:29"));
        expect("mom Tue 05:30 awake",             null,    "mom", ist("2026-11-17T05:30"));
        expect("mom Sat 00:00 not a weekday",     null,    "mom", ist("2026-11-21T00:00"));
        expect("mom Sat 23:00 'all' still sleeps","sleep", "mom", ist("2026-11-21T23:00"));

        // each person is judged in own time zone
        // 2026-11-17T04:30Z = Mon 23:30 in Indy (UTC-5), Tue 10:00 in India (UTC+5:30).
        Clock c = Clock.fixed(Instant.parse("2026-11-17T04:30:00Z"), ZoneOffset.UTC);
        check("Indy local day is Monday (not UTC Tuesday)", HomeClock.localNow(c, "me").getDayOfWeek() == DayOfWeek.MONDAY);
        check("India local day is Tuesday", HomeClock.localNow(c, "mom").getDayOfWeek() == DayOfWeek.TUESDAY);
        check("me asleep via Clock at Mon 23:30", !HomeClock.isFree(W, "me", HomeClock.localNow(c, "me")));
        check("mom free via Clock at Tue 10:00 IST", HomeClock.isFree(W, "mom", HomeClock.localNow(c, "mom")));

        // verdict (Nov 2026 = EST, India is +10:30 ahead)
        expectVerdict("both free -> YES", true, "Good time to call", "2026-11-16T09:00");
        expectVerdict("only me busy (sleep)", false, "You're asleep", "2026-11-16T22:45");
        expectVerdict("only mom busy (sleep)", false, "Mom is asleep", "2026-11-17T11:00");
        expectVerdict("only mom busy (work)", false, "Mom is at work", "2026-11-18T01:00");
        expectVerdict("both busy lists both", false, "You're in class; Mom is asleep", "2026-11-16T10:30");
        expectVerdict("both busy (sleep + work)", false, "You're asleep; Mom is at work", "2026-11-17T02:00");
        check("describe: unknown label still readable", HomeClock.describe("me", "gym").equals("You're busy (gym)"));

        // rejecting bad input
        try {
            HomeClock.parseWindows(List.of("dad sleep 00:00 06:00 all"));
            check("unknown person rejected", false);
        } catch (IllegalArgumentException e) {
            check("unknown person rejected", true);
        }

        System.out.println();
        System.out.println((total - failed) + "/" + total + " passed");
        if (failed > 0) System.exit(1);
    }
}
