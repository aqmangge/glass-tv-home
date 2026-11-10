#!/usr/bin/env python3
"""Plays Belajar Ceria on a connected emulator or device the way a child would, records the screen, and checks
that every game reacted (the app logs each event under the tag "Ceria").

    python3 kids-learn/tools/playtest.py <out dir>      (the APK must already be installed)

Writes <out>/playtest.mp4, a screenshot after each game, and <out>/report.txt. Exits 1 if a check failed or the
app crashed.
"""
import os
import re
import subprocess
import sys
import time

PKG = "com.belajarceria.kids"
OUT = sys.argv[1] if len(sys.argv) > 1 else "playtest"
checks = []


def adb(*args, timeout=60):
    """Runs one adb command; one that hangs (it happens on a busy emulator) is given up on, not waited for."""
    try:
        return subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout).stdout
    except subprocess.TimeoutExpired:
        print(f"  ! adb {' '.join(args)[:60]} timed out")
        return ""


def sh(cmd):
    return adb("shell", cmd)


def nodes():
    """Every labelled piece on screen as (label, (left, top, right, bottom)). The app logs them each time a screen
    or round settles (uiautomator can't be used: the animated background never lets the screen go idle)."""
    lines = events()
    if "layout" not in lines:
        return []
    last = len(lines) - 1 - lines[::-1].index("layout")
    found = []
    for e in lines[last + 1:]:
        m = re.match(r"view (.+) (-?\d+) (-?\d+) (-?\d+) (-?\d+)$", e)
        if m:
            found.append((m.group(1), tuple(int(m.group(i)) for i in range(2, 6))))
    return found


def find(desc, exact=False):
    return [b for d, b in nodes() if (d == desc if exact else d.startswith(desc))]


def center(b):
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def tap(desc, wait=3.0):
    hits = find(desc, exact=True)
    if not hits:
        print(f"  ! nothing on screen called {desc!r}")
        return False
    x, y = center(hits[0])
    sh(f"input tap {x} {y}")
    time.sleep(wait)
    return True


def swipe(a, b, ms):
    sh(f"input swipe {a[0]} {a[1]} {b[0]} {b[1]} {ms}")


def back(wait=2.5):
    sh("input keyevent 4")
    time.sleep(wait)


def shot(name):
    sh("screencap -p /sdcard/play.png")
    adb("pull", "/sdcard/play.png", os.path.join(OUT, name + ".png"))
    print(f"  screenshot {name}.png")


def events():
    return [l.split(": ", 1)[-1].strip() for l in adb("logcat", "-d", "-s", "Ceria:I").splitlines() if "Ceria" in l]


def count(prefix, since=0):
    return sum(1 for e in events()[since:] if e.startswith(prefix))


def check(name, ok, detail=""):
    checks.append((name, ok, detail))
    print(("  PASS " if ok else "  FAIL ") + name + (f"  ({detail})" if detail else ""))


def play_learn():
    print("Learn: swipe through the flash cards")
    tap("Learn", 3)
    before = len(events())
    card = find("card", exact=True)
    if card:
        x, y = center(card[0])
        for _ in range(2):
            swipe((x + 250, y), (x - 450, y), 220)  # a quick flick to the left: next card
            time.sleep(2)
        sh(f"input tap {x} {y}")  # a tap reads it again and wobbles
        time.sleep(1.5)
    shot("p1-learn")
    check("Learn: flicking shows the next cards", count("card ", before) >= 2, f"{count('card ', before)} cards")
    back()


def play_drag(opened=False, label="Drag", picture="p2-drag-done"):
    print(f"{label}: one wrong drop, then every tile into its slot")
    if not opened:
        tap("Drag", 4.5)
    before = len(events())
    tiles = [(d[5:], b) for d, b in nodes() if d.startswith("tile:")]
    slots = {d[5:]: b for d, b in nodes() if d.startswith("slot:")}
    if len(tiles) >= 2 and len(slots) >= 2:
        word, tb = tiles[0]
        other = next(w for w in slots if w != word)
        swipe(center(tb), center(slots[other]), 1200)  # the wrong slot: it should shake and bounce back
        time.sleep(2)
        for word, tb in tiles:
            if word in slots:
                swipe(center(tb), center(slots[word]), 1200)
                time.sleep(1.6)
        time.sleep(0.2)
        shot(picture)
    check(f"{label}: a wrong drop is refused", count("drag wrong", before) == 1, f"{count('drag wrong', before)}")
    check(f"{label}: every tile lands in its slot", count("drag right", before) == len(tiles) > 0,
          f"{count('drag right', before)} of {len(tiles)}")
    time.sleep(1.5)
    check(f"{label}: the round ends with a celebration", count("celebrate", before) >= 1)
    time.sleep(3)
    back()


def play_match(tap_mode=False, opened=False, label="Match", picture=None):
    print(f"{label}: " + ("tap one, then its partner" if tap_mode else "one wrong line, then a line for every pair"))
    if not opened:
        tap("Match", 4.5)
    before = len(events())
    lefts = [(d[5:], b) for d, b in nodes() if d.startswith("left:")]
    rights = {d[6:]: b for d, b in nodes() if d.startswith("right:")}
    if lefts and rights:
        if not tap_mode:
            word, lb = lefts[0]
            other = next(w for w in rights if w != word)
            swipe(center(lb), center(rights[other]), 900)
            time.sleep(1.5)
        for word, lb in lefts:
            if word not in rights:
                continue
            if tap_mode:
                sh("input tap %d %d" % center(lb))
                time.sleep(0.8)
                sh("input tap %d %d" % center(rights[word]))
            else:
                swipe(center(lb), center(rights[word]), 900)
            time.sleep(1.4)
        time.sleep(0.2)
        shot(picture or ("p4-match-tap" if tap_mode else "p3-match-done"))
    if not tap_mode:
        check(f"{label}: a wrong line is refused", count("match wrong", before) == 1, f"{count('match wrong', before)}")
    check(f"{label}: every pair joins" + (" by tapping" if tap_mode else " by drawing a line"),
          count("match right", before) == len(lefts) > 0, f"{count('match right', before)} of {len(lefts)}")
    time.sleep(1.5)
    check(f"{label}: the round ends with a celebration", count("celebrate", before) >= 1)
    time.sleep(3)
    back()


def play_trace():
    print("Draw: trace Alif with a finger")
    tap("Draw", 4)
    before = len(events())
    board = find("board", exact=True)
    if board:
        l, t, r, b = board[0]
        x, h = (l + r) // 2, b - t
        # Alif is one tall stroke in the middle: draw it top to bottom, twice, like a careful child.
        swipe((x, t + int(h * 0.12)), (x + 10, t + int(h * 0.88)), 900)
        time.sleep(1.2)
        if count("traced", before) == 0:
            swipe((x + 12, t + int(h * 0.12)), (x + 18, t + int(h * 0.88)), 900)
            time.sleep(1.2)
        shot("p5-trace-done")
    scores = [e for e in events()[before:] if e.startswith("trace check")]
    check("Draw: tracing the letter is recognised", count("traced", before) == 1, "; ".join(scores))
    time.sleep(2.5)
    check("Draw: tracing ends with a celebration", count("celebrate", before) >= 1)
    time.sleep(2.5)
    shot("p6-trace-next")
    back()


def wait_question(i, timeout=15):
    """Waits until question i is on screen and its buttons are logged; returns the right answer's index."""
    end = time.time() + timeout
    while time.time() < end:
        ev = events()
        mark = [k for k, e in enumerate(ev) if e.startswith(f"exam q {i} ")]
        if mark and "layout" in ev[mark[-1]:]:
            return int(ev[mark[-1]].split()[-1])
        time.sleep(0.5)
    return None


def answer_test(wrong, picture, at):
    """Answers the 25 questions on screen, all right but question wrong + 1; returns how many were answered."""
    before = len(events())
    for i in range(25):
        right = wait_question(i)
        if right is None:
            print(f"  ! question {i + 1} never appeared")
            break
        if i == at:
            shot(picture)
        opts = {d: b for d, b in nodes() if d.startswith("opt:")}
        pick = right if i != wrong else (right + 1) % max(1, len(opts))
        b = opts.get(f"opt:{pick}")
        if b:
            sh("input tap %d %d" % center(b))
        time.sleep(0.4)
    time.sleep(1)
    return count("exam answer", before)


def play_exam():
    print("Ujian: the Bahasa Melayu part, 24 right and one wrong on purpose")
    # the home screen scrolls now (eight cards), so open the test directly
    sh(f"am start -S -W -n {PKG}/.MainActivity --es open exam")
    time.sleep(6)
    before = len(events())
    tap("start:0", 1)
    shot_taken = False
    for i in range(25):
        right = wait_question(i)
        if right is None:
            print(f"  ! question {i + 1} never appeared")
            break
        if i == 1:
            # The voice button: Melayu -> English (photographed) -> Senyap -> Melayu.
            v0 = len(events())
            tap("voice", 1.5)
            shot("p7b-exam-english")
            tap("voice", 1)
            tap("voice", 1)
            modes = [e.split()[-1] for e in events()[v0:] if e.startswith("exam voice")]
            check("Ujian: the voice button cycles Melayu, English, Senyap", modes == ["en", "off", "ms"], " ".join(modes))
        if i == 2 and not shot_taken:
            shot("p7-exam-question")
            shot_taken = True
        opts = {d: b for d, b in nodes() if d.startswith("opt:")}
        pick = right if i != 3 else (right + 1) % len(opts)  # question 4 answered wrongly
        b = opts.get(f"opt:{pick}")
        if b:
            sh("input tap %d %d" % center(b))
        time.sleep(0.4)
    time.sleep(2.5)
    shot("p8-exam-result")
    ev = events()[before:]
    check("Ujian: all 25 questions answered", count("exam answer", before) == 25, f"{count('exam answer', before)}")
    check("Ujian: the mark is 24/25 (one wrong on purpose)", "exam result 0 24/25" in ev,
          next((e for e in ev if e.startswith("exam result")), "no result"))
    check("Ujian: the result is celebrated", count("celebrate", before) >= 1)
    time.sleep(2)
    if tap("Lihat jawapan", 2):
        shot("p9-exam-review")
    back()


def play_heroes():
    print("Hero Squad: drag tiles in the hero theme; heroes zap right answers and fly by")
    sh(f"am start -S -W -n {PKG}/.MainActivity --es theme hero --es open 0,2")
    time.sleep(6)
    before = len(events())
    tiles = [(d[5:], b) for d, b in nodes() if d.startswith("tile:")]
    slots = {d[5:]: b for d, b in nodes() if d.startswith("slot:")}
    for k, (word, tb) in enumerate(tiles):
        if word in slots:
            swipe(center(tb), center(slots[word]), 1000)
            time.sleep(1.0 if k else 0.9)
            if k == 0:
                shot("p10-hero-zap")
            time.sleep(1.4)
    time.sleep(0.6)
    shot("p11-hero-squad")
    check("Heroes: a hero zaps each right answer", count("hero zap", before) == len(tiles) > 0,
          f"{count('hero zap', before)} of {len(tiles)}")
    time.sleep(16)  # long enough for a hero to run or fly by on its own
    sh(f"am start -S -W -n {PKG}/.MainActivity --es theme classic")
    time.sleep(3)


def play_babies():
    print("Kawan Comel: drag tiles in the baby theme; a baby pops up for each right answer")
    sh(f"am start -S -W -n {PKG}/.MainActivity --es theme baby --es open 1,2")
    time.sleep(6)
    before = len(events())
    tiles = [(d[5:], b) for d, b in nodes() if d.startswith("tile:")]
    slots = {d[5:]: b for d, b in nodes() if d.startswith("slot:")}
    for k, (word, tb) in enumerate(tiles):
        if word in slots:
            swipe(center(tb), center(slots[word]), 1000)
            time.sleep(1.0)
            if k == 0:
                shot("p12-baby-pop")
            time.sleep(1.2)
    time.sleep(0.8)
    shot("p13-baby-wave")
    check("Babies: a baby pops up for each right answer", count("baby pop", before) == len(tiles) > 0,
          f"{count('baby pop', before)} of {len(tiles)}")
    time.sleep(18)  # long enough for a baby to swing, munch, play or sneeze on its own
    sh(f"am start -S -W -n {PKG}/.MainActivity --es theme classic")
    time.sleep(3)


def play_music():
    print("Music corner: piano chord, drums, a guitar strum, a xylophone glissando")
    before = len(events())
    tap("music", 3.5)
    v = find("piano", exact=True)
    if v:
        l, t, r, b = v[0]
        w = r - l
        # a C major chord (three fingers at once is not possible with input; play C E G quickly) and a slide
        for frac in (0.03, 0.17, 0.30, 0.50):
            sh(f"input tap {int(l + w * frac)} {int(b - (b - t) * 0.2)}")
        swipe((int(l + w * 0.05), int(b - (b - t) * 0.2)), (int(l + w * 0.6), int(b - (b - t) * 0.2)), 600)
        time.sleep(0.3)
        shot("p14-music-piano")
    tap("instrument:1", 2)
    v = find("drums", exact=True)
    if v:
        l, t, r, b = v[0]
        for fx, fy in ((0.50, 0.70), (0.27, 0.70), (0.09, 0.30), (0.91, 0.27)):  # kick, snare, hi-hat, crash
            sh(f"input tap {int(l + (r - l) * fx)} {int(t + (b - t) * fy)}")
        time.sleep(0.2)
        shot("p15-music-drums")
    tap("instrument:2", 2)
    v = find("guitar", exact=True)
    if v:
        l, t, r, b = v[0]
        h = b - t
        pw = min((r - l) * 0.22, 190 * 2.625)
        sh(f"input tap {int(l + pw * 0.25)} {int(t + h * 0.125)}")  # the C chord button
        time.sleep(0.5)
        swipe((int(l + (r - l) * 0.6), int(t + h * 0.08)), (int(l + (r - l) * 0.6), int(t + h * 0.92)), 400)  # strum
        time.sleep(0.3)
        shot("p16-music-guitar")
    tap("instrument:3", 2)
    v = find("xylophone", exact=True)
    if v:
        l, t, r, b = v[0]
        swipe((int(l + (r - l) * 0.03), int((t + b) / 2)), (int(l + (r - l) * 0.97), int((t + b) / 2)), 900)
        time.sleep(0.3)
        shot("p17-music-xylophone")
    ev = events()[before:]
    piano = sum(1 for e in ev if e.startswith("music piano"))
    drums = sum(1 for e in ev if e.startswith("music drum"))
    chord = [e for e in ev if e.startswith("music chord")]
    strum = sum(1 for e in ev if e.startswith("music guitar"))
    xylo = sum(1 for e in ev if e.startswith("music xylophone"))
    check("Music: the piano plays its keys, and a slide plays each key in turn", piano >= 6, f"{piano} notes")
    check("Music: every drum hit sounds", drums == 4, f"{drums} hits")
    check("Music: a C chord strum plays its strings", chord == ["music chord C"] and strum >= 4,
          f"{chord} {strum} strings")
    check("Music: a xylophone glissando strikes the bars", xylo >= 8, f"{xylo} bars")
    back()


def play_mandarin():
    print("Cakap Mandarin: word cards and the microphone, a conversation, a speech with applause")
    sh(f"pm grant {PKG} android.permission.RECORD_AUDIO")
    before = len(events())
    sh(f"am start -S -W -n {PKG}/.MainActivity --es open zh")
    time.sleep(6)
    tap("zh level 0", 3.5)
    tap("next", 1.5)
    tap("next", 1.5)
    tap("mic", 1)
    # the emulator has no recogniser, so the app records instead; it stops on its own after six seconds
    time.sleep(8)
    shot("p18-zh-words")
    # A second tap while listening means "I've finished": it must answer, and the microphone must work again after.
    m0 = len(events())
    tap("mic", 1.5)
    tap("mic", 3)
    tap("mic", 1.5)
    tap("mic", 3)
    mev = events()[m0:]
    check("Mandarin: tapping the microphone again finishes, and it listens again every time",
          sum(e.startswith("zh mic listening") for e in mev) == 2 and sum(e.startswith("zh mic stars") for e in mev) == 2,
          " / ".join(e for e in mev if e.startswith("zh mic")))
    back()
    back()
    sh(f"am start -S -W -n {PKG}/.MainActivity --es open zh,2,0")
    time.sleep(6)
    for k in range(4):
        time.sleep(1.5)  # the friend speaks first; the replies appear after
        tap("reply:wrong", 1.5)
        tap("reply:right", 3)
        if k == 1:
            shot("p19-zh-chat")
        tap("skip", 3.5)
    time.sleep(1)
    shot("p20-zh-chat-done")
    sh(f"am start -S -W -n {PKG}/.MainActivity --es open zh,3,0")
    time.sleep(6)
    shot("p21-zh-stage")
    for _ in range(7):
        tap("next", 1.2)
    time.sleep(1.5)
    shot("p22-zh-applause")
    sh(f"am start -S -W -n {PKG}/.MainActivity --es open zh,4")
    time.sleep(6)
    play_drag(opened=True, label="Mandarin drag", picture="p23-zh-drag")
    sh(f"am start -S -W -n {PKG}/.MainActivity --es open zh,5")
    time.sleep(6)
    play_match(opened=True, label="Mandarin match", picture="p24-zh-match")
    sh(f"am start -S -W -n {PKG}/.MainActivity --es open zh,6")
    time.sleep(6)
    e0 = len(events())
    tap("start:4", 1)
    answered = answer_test(wrong=7, picture="p25-zh-exam-question", at=4)
    time.sleep(2.5)
    shot("p26-zh-exam-result")
    xev = events()[e0:]
    check("Mandarin test: all 25 questions answered", answered == 25, f"{answered}")
    check("Mandarin test: the mark is 24/25 (one wrong on purpose)", "exam result 4 24/25" in xev,
          next((e for e in xev if e.startswith("exam result")), "no result"))
    ev = events()[before:]
    phrases = [e for e in ev if e.startswith("zh phrase")]
    check("Mandarin: word cards go forward", len(phrases) >= 3, f"{len(phrases)} cards")
    check("Mandarin: the microphone listens and scores", any(e.startswith("zh mic stars") for e in ev),
          next((e for e in ev if e.startswith("zh mic")), "no mic"))
    check("Mandarin: a wrong reply is refused, a right one goes on", sum(e.startswith("zh chat wrong") for e in ev) == 4
          and sum(e.startswith("zh chat right") for e in ev) == 4, f"{[e for e in ev if e.startswith('zh chat')]}")
    check("Mandarin: the conversation is finished", any(e.startswith("zh chat done") for e in ev))
    check("Mandarin: the speech ends with applause", any(e.startswith("zh stage applause") for e in ev))
    sh(f"am start -S -W -n {PKG}/.MainActivity")
    time.sleep(3)


def record(name):
    """Starts a screen recording (Android stops each one after 3 minutes)."""
    return subprocess.Popen(["adb", "shell", "screenrecord", "--time-limit", "180", "--bit-rate", "6000000",
                             f"/sdcard/{name}.mp4"])


def stop(rec, name):
    sh("pkill -INT screenrecord")
    try:
        rec.wait(timeout=20)
    except subprocess.TimeoutExpired:
        rec.kill()
    time.sleep(3)
    adb("pull", f"/sdcard/{name}.mp4", os.path.join(OUT, f"{name}.mp4"))


def main():
    os.makedirs(OUT, exist_ok=True)
    sh("settings put secure immersive_mode_confirmations confirmed")
    # The app logs every screen's layout for this test: a bigger log keeps a long run's events from wrapping away.
    adb("logcat", "-G", "16M")
    adb("logcat", "-c")
    sh(f"am start -S -W -n {PKG}/.MainActivity")
    time.sleep(6)
    rec = record("playtest")
    time.sleep(2)
    shot("p0-home")

    print("Home: open ABC")
    tap("ABC", 3)
    play_learn()
    play_drag()
    play_match()
    back()

    print("Home: open Alif Ba Ta")
    tap("Alif Ba Ta", 3)
    play_trace()
    back()

    print("Home: open Islamic Words")
    tap("Islamic Words", 3)
    play_match(tap_mode=True)
    back()

    stop(rec, "playtest")

    rec = record("mandarin")
    play_mandarin()
    stop(rec, "mandarin")

    rec = record("music")
    play_music()
    stop(rec, "music")

    rec = record("exam")
    time.sleep(1)
    play_exam()
    stop(rec, "exam")

    rec = record("heroes")
    play_heroes()
    stop(rec, "heroes")

    rec = record("babies")
    play_babies()
    stop(rec, "babies")

    crash = adb("logcat", "-d", "-b", "crash")
    check("No crash", PKG not in crash, crash.strip()[:300])
    with open(os.path.join(OUT, "report.txt"), "w") as f:
        for name, ok, detail in checks:
            f.write(("PASS  " if ok else "FAIL  ") + name + (f"  ({detail})" if detail else "") + "\n")
        f.write("\nEvents logged by the app:\n" + "\n".join(events()) + "\n")
    failed = [c for c in checks if not c[1]]
    print(f"\n{len(checks) - len(failed)} of {len(checks)} checks passed")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
