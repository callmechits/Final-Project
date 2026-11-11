import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class HomeClock {
    static final ZoneId INDY = ZoneId.of("America/Indiana/Indianapolis");
    static final ZoneId IST  = ZoneId.of("Asia/Kolkata");
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE d MMM  hh:mm a");// Example output: "Wed 11 Nov  08:30 PM"
    static final int COL = 25;
    static final String RULE = "\u2500".repeat(COL * 2 + 1); // Trying to have a row of '-' before and after the matrix is printed.

    static void showTimes(Clock clock) {
        ZonedDateTime me  = ZonedDateTime.now(clock.withZone(INDY));
        ZonedDateTime mom = ZonedDateTime.now(clock.withZone(IST));
        System.out.println();
        System.out.println(RULE);  
        System.out.printf("%-30s %-22s%n", "Indianapolis (you)", "India (Mom)");
        System.out.printf("%-30s %-22s%n", me.format(FMT), mom.format(FMT));
        System.out.println(RULE);
        System.out.println();
    }

    public static void main(String[] args) {
        showTimes(Clock.systemUTC());
    }
}
