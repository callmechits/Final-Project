# Home Clock 🕰️

A terminal widget that shows Indianapolis and India time side by side and tells me whether it's a good time to call my mom, and if not, when the next good window opens.

## Project Goal

My mom lives in India and I live in Indianapolis. The 9.5 hour time difference means one of us is usually asleep, in class, or at work when the other is free, so we miss a lot of calls from each other. This project will check both of our schedules against the current time in each zone and give a clear answer: **call now** or **call at X**.

## MVP Scope

The first working version (v1.0) intends to:

- Show the current Indianapolis and India time side by side.
- Load availability windows for both of us (sleep, classes, work) from a plain-text schedule file.
- Give a **YES / NO** verdict on whether it's a good time to call, with the reason.
- If the answer is no, show when the next good window opens, with a countdown.
- Refresh every minute while running.

## Future Plans (v2.0+)

- Separate weekday and weekend schedules.
- A Windows notification when a good window opens.
- A call log with a streak display (like "last called 3 days ago").
- Launch on terminal open, and a `cron` job for scheduled checks.

## Tech Stack

| Area | Choice |
|---|---|
| Language | Java (JDK 21) for the core logic; Bash for the launcher |
| Time zones & DST | `java.time` (`ZoneId`, `ZonedDateTime`, `LocalTime`, `Duration`) |
| File reading | `java.nio.file.Files` |
| Terminal output | `System.out.printf` + ANSI escape codes for color |
| Refresh loop | `Thread.sleep` (once per minute) |
| Data storage | Plain-text `schedule.conf`, one time window per line |
| External data | None |

No external libraries will be used for the MVP.

### Schedule file format (planned)

```
# who   label   start  end
me      sleep   00:30  08:00
me      class   09:00  15:00
mom     sleep   22:30  06:30
mom     work    10:00  18:00
```

## Design Docs

- [Eisenhower Matrix](docs/Eisenhower-Matrix.pdf): (Do / Schedule / Delegate / Delete)
- [System Flowchart](docs/flowchart.pdf): how data and control flow through the program

## Project Structure

```
home-clock/
├── src/         # Java source (HomeClock.java and supporting classes)
├── tests/       # Tests using a fixed Clock
├── docs/        # Eisenhower Matrix and flowchart
├── README.md
```

`homeclock.sh` (launcher) and `schedule.conf` will be added at the repo root while I'm building it.

## Build Milestones

- [ ] 1. Set up repo and environment
- [ ] 2. Display both times side by side
- [ ] 3. Load availability windows from `schedule.conf`
- [ ] 4. Check whether each person is free (including windows that cross midnight)
- [ ] 5. Give the "good time to call?" verdict with a reason
- [ ] 6. Count down to the next good window
- [ ] 7. Refresh live and add the Bash launcher
- [ ] 8. Polish, handle config errors, and document (v1.0)

## Status

🚧 Planning complete; implementation to be started.
