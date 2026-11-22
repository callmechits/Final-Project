import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeClock {
    static final ZoneId INDY = ZoneId.of("America/Indiana/Indianapolis");
    static final ZoneId IST  = ZoneId.of("Asia/Kolkata");
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE d MMM  hh:mm a"); // Example output: "Wed 11 Nov  08:30 PM"
    static final int COL = 25;
    static final String RULE = "\u2500".repeat(COL * 2 + 1); // Trying to have a row of '-' before and after the matrix is printed.
    
    record Window(String who, String label, LocalTime start, LocalTime end, Set<DayOfWeek> days) {}

    static final Map<String, DayOfWeek> DAY_SHORT = Map.of(
            "mon", DayOfWeek.MONDAY,    
            "tue", DayOfWeek.TUESDAY,
            "wed", DayOfWeek.WEDNESDAY, 
            "thu", DayOfWeek.THURSDAY,
            "fri", DayOfWeek.FRIDAY,    
            "sat", DayOfWeek.SATURDAY,
            "sun", DayOfWeek.SUNDAY
        );    

    // Gets the day of one single line
    static DayOfWeek parseDay(String name) {        
        String use = name.toLowerCase();
        if (DAY_SHORT.containsKey(use)) {
            return DAY_SHORT.get(use);
        }
        try {
            return DayOfWeek.valueOf(use.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown day " + name);
        }
    }

    // Parses the day name; can be solo, list, weekend, weekday, all
    static Set<DayOfWeek> parseDays(String use1) {      
        switch (use1.toLowerCase()) {
            case "all" -> {
                return EnumSet.allOf(DayOfWeek.class);
            }
            
            case "weekday" -> {
                return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
            }

            case "weekend" -> {
                return EnumSet.range(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            }
        }

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String name : use1.split(",")) {
            days.add(parseDay(name));
        }
        return days;
    }

    // Parses time as HH:mm, also I check the midnight boundary just in case
    static LocalTime parseTime(String t) {
        return t.equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(t);    // Return 24:00 if its midnight else just the parsed time.
    }

    // Parses schedule.conf into usable parts
    static List<Window> loadWindows(Path file) throws IOException {
        List<Window> windows = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();

            if (line.isEmpty() || line.startsWith(("#"))) continue;
                String[] f = line.split("\\s+");
                if(f.length != 5) {
                    throw new IllegalArgumentException("line " + (i + 1) + ": expected 5 fields " + "(who label start end day), got " + f.length);
                }
                try {
                    windows.add(new Window(f[0].toLowerCase(), f[1], parseTime(f[2]), parseTime(f[3]), parseDays(f[4])));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("line " + (i+1) + ": bad time (use HH:mm, 24h)");
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("line " + (i+1) + ": " + e.getMessage());
                }
            }
        return windows;
    }   

    static void showTimes(Clock clock) {
        ZonedDateTime me  = ZonedDateTime.now(clock.withZone(INDY));
        ZonedDateTime mom = ZonedDateTime.now(clock.withZone(IST));
        System.out.println();
        System.out.println(RULE);  
        System.out.printf("%-30s %-22s%n", "Indianapolis (me)", "India (Mom)");
        System.out.printf("%-30s %-22s%n", me.format(FMT), mom.format(FMT));
        System.out.println(RULE);
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
        Path conf = Path.of(args.length > 0 ? args[0] : "schedule.conf");

        showTimes(Clock.systemUTC());

        for (Window w : loadWindows(conf)) {
            System.out.printf("%-4s %-8s %s -> %s  %s%n", w.who(), w.label(), w.start(), w.end(), w.days());
        }
    }
}
