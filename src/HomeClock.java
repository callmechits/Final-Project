import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
    static final boolean COLOR = System.getenv("NO_COLOR") == null;
    static final String GREEN = COLOR ? "\u001B[32m" : "";
    static final String RED = COLOR ? "\u001B[31m" : "";
    static final String BOLD = COLOR ? "\u001B[1m" : "";
    static final String RESET = COLOR ? "\u001B[0m" : "";
    
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

    /**
     * Parses schedule lines: who label start end day. Blank lines and
     * lines starting with '#' are skipped
     */
    static List<Window> parseWindows(List<String> lines) {
        List<Window> windows = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] f = line.split("\\s+");
            if(f.length != 5) {
                throw new IllegalArgumentException("line " + (i+1) + ": expected 5 fields " + "(who label start end day, got" + f.length);
            }
            try {
                String who = f[0].toLowerCase();
                if(!who.equals("me") && !who.equals("mom")) {
                    throw new IllegalArgumentException("who must be 'me' or 'mom', got '" + f[0] + "'");
                }
                windows.add(new Window(who, f[1], parseTime(f[2]), parseTime(f[3]), parseDays(f[4])));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("line " + (i+1) + ": bad time (use HH:mm, 24h)");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("line " + (i+1) + ": " + e.getMessage());
            }
        }
        return windows;
    }

    static List<Window> loadWindows(Path file) throws IOException {
        return parseWindows(Files.readAllLines(file));
    }   

    // Milestone 4
    // Finding timezone of person
    static ZoneId zoneOf(String who) {
        return who.equals("me") ? INDY : IST;
    }

    // Finding time for given 'who'
    static ZonedDateTime localNow(Clock clock, String who) {
        return ZonedDateTime.now(clock.withZone(zoneOf(who)));
    }

    static Window busyWindow(List<Window> windows, String who, ZonedDateTime local) {
        LocalTime t = local.toLocalTime();
        DayOfWeek today = local.getDayOfWeek();
        DayOfWeek yesterday = today.minus(1);

        for (Window w : windows) {
            if (!w.who().equals(who)) continue;

            if (w.end().isAfter(w.start())) {
                // For the same day: start <= t < end
                if (w.days().contains(today) && !t.isBefore(w.start()) && t.isBefore(w.end())) {
                    return w;
                }
            } else {
                // When the start and end time cross midnight in between
                boolean evening = w.days().contains(today) && !t.isBefore(w.start());
                boolean morning = w.days().contains(yesterday) && t.isBefore(w.end());
                if (evening || morning) {
                    return w;
                }
            }
        }
        return null;
    } 

    static boolean isFree(List<Window> windows, String who, ZonedDateTime local) {
        return busyWindow(windows, who, local) == null;
    }


    // Milestone 5
    record Verdict(boolean good, String reason) {}

    // Uses data from schedule file to describe situations
    static String describe(String who, String label) {
        String subject = who.equals("me") ? "You're" : "Mom is";
        String state = switch (label.toLowerCase()) {
            case "sleep" -> "asleep";
            case "class" -> "in class";          
            case "work" -> "at work";           
            case "study" -> "studying";          
            default -> "busy (" + label + ")";
        };
        return subject + " " + state;
    }

    // Returning YES if both people are free; otherwise NO + listing reasons
    static Verdict verdict(List<Window> windows, Clock clock) {
        List<String> reasons = new ArrayList<>();
        for (String who : new String[] {"me", "mom"}) {
            Window w = busyWindow(windows, who, localNow(clock, who));
            if (w != null) {
                reasons.add(describe(who, w.label()));
            }  
        }
        if (reasons.isEmpty()) {
                return new Verdict(true, "Good time to call");
            }
        return new Verdict(false, String.join("; ", reasons));
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

    // Prints green YES and red NO along with reason why
    static void showVerdict(Verdict v) {
        if (v.good()) {
            System.out.println(BOLD + GREEN + "YES  " + v.reason() + RESET);
        } else {
            System.out.println(BOLD + RED + "NO   " + v.reason() + RESET);
        }
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
        Path conf = Path.of(args.length > 0 ? args[0] : "schedule.conf");

        Clock clock = Clock.systemUTC();
        if (args.length > 1) {
            clock = Clock.fixed(LocalDateTime.parse(args[1]).atZone(INDY).toInstant(), ZoneOffset.UTC);
        }
        
        List<Window> windows = loadWindows(conf);

        showTimes(clock);
        showVerdict(verdict(windows, clock));
    }
}
