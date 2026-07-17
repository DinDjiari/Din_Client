#!/usr/bin/env python3
"""Synthesize the Dindijari Client UI sound set as OGG Vorbis files.

All sounds are generated from scratch (sine/triangle partials + envelopes),
so they are original works with no third-party material (released CC0).
"""
import numpy as np
import soundfile as sf
import os

SR = 44100
OUT = "/home/user/Din_Client/src/main/resources/assets/dindijariclient/sounds"
os.makedirs(OUT, exist_ok=True)


def t(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def env(x, attack, release, dur):
    """Attack/release envelope over duration dur."""
    n = len(x)
    e = np.ones(n)
    a = int(SR * attack)
    r = int(SR * release)
    if a > 0:
        e[:a] = np.linspace(0, 1, a)
    if r > 0:
        e[-r:] *= np.linspace(1, 0, r) ** 1.5
    return x * e


def sine(freq, dur, detune=0.0):
    x = t(dur)
    return np.sin(2 * np.pi * (freq + detune) * x)


def sweep(f0, f1, dur):
    x = t(dur)
    freq = np.linspace(f0, f1, len(x))
    phase = 2 * np.pi * np.cumsum(freq) / SR
    return np.sin(phase)


def norm(x, peak):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def write(name, data):
    sf.write(os.path.join(OUT, name + ".ogg"), data.astype(np.float32), SR,
             format="OGG", subtype="VORBIS")
    print("wrote", name, len(data) / SR, "s")


# ui_hover — very soft, short tick: filtered sine blip at 2.2 kHz.
dur = 0.045
s = sine(2200, dur) * np.exp(-t(dur) * 90)
s = env(s, 0.002, 0.02, dur)
write("ui_hover", norm(s, 0.35))

# ui_click — two-partial click, quick decay (1.4 kHz + 0.9 kHz).
dur = 0.07
s = (sine(1400, dur) * 0.8 + sine(900, dur) * 0.5) * np.exp(-t(dur) * 60)
s = env(s, 0.001, 0.03, dur)
write("ui_click", norm(s, 0.5))

# toggle_on — rising two-note motif (660 -> 880 Hz) with soft harmonics.
dur = 0.16
a = sine(660, 0.07) * np.exp(-t(0.07) * 25)
b = sine(880, 0.11) * np.exp(-t(0.11) * 18) + 0.3 * sine(1760, 0.11) * np.exp(-t(0.11) * 30)
s = np.concatenate([a, b * 0.9])
s = env(s, 0.004, 0.05, len(s) / SR)
write("toggle_on", norm(s, 0.5))

# toggle_off — falling counterpart (880 -> 660 Hz).
a = sine(880, 0.07) * np.exp(-t(0.07) * 25)
b = sine(660, 0.11) * np.exp(-t(0.11) * 18) + 0.3 * sine(1320, 0.11) * np.exp(-t(0.11) * 30)
s = np.concatenate([a, b * 0.9])
s = env(s, 0.004, 0.05, len(s) / SR)
write("toggle_off", norm(s, 0.5))

# notify — friendly pop: short upward chirp with a bell partial.
dur = 0.18
s = sweep(950, 1500, dur) * np.exp(-t(dur) * 20)
s += 0.4 * sine(2400, dur) * np.exp(-t(dur) * 35)
s = env(s, 0.003, 0.06, dur)
write("notify", norm(s, 0.5))

# dialog_open — soft swell: slow upward sweep, gentle attack.
dur = 0.22
s = sweep(320, 620, dur) * 0.8 + 0.35 * sweep(640, 1240, dur)
s *= np.exp(-t(dur) * 8)
s = env(s, 0.04, 0.09, dur)
write("dialog_open", norm(s, 0.42))

# error — subdued buzz: detuned low pair with tremolo.
dur = 0.28
x = t(dur)
s = (sine(220, dur) + sine(233, dur)) * 0.5
s *= 0.6 + 0.4 * np.sin(2 * np.pi * 28 * x)  # tremolo
s *= np.exp(-x * 7)
s = env(s, 0.006, 0.09, dur)
write("error", norm(s, 0.5))

print("done")
