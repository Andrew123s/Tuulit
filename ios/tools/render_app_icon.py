"""Renders the iOS app icon (1024x1024, opaque) from the same mascot paths as Android's
adaptive launcher icon (app/src/main/res/drawable/ic_launcher_foreground.xml).

Usage: python ios/tools/render_app_icon.py
"""
import math
import os
import re

from PIL import Image, ImageDraw

SIZE = 1024
SS = 4  # supersampling
BG = "#F6F0E4"
OUT = os.path.join(os.path.dirname(__file__), "..", "Tuulit", "Resources", "Assets.xcassets", "AppIcon.appiconset", "AppIcon-1024.png")

# (path, fill, stroke, stroke width) in the 120x120 mascot space, copied from the Android vector.
LAYERS = [
    ("M54 63C40 55 28 63 26 86c7 0 12-12 26-15z", "#D6BE98", "#221E1A", 3),
    ("M66 63c12-9 24-6 27 7-7 0-12-4-25 1z", "#D6BE98", "#221E1A", 3),
    ("M51 66C47 44 50 22 62 13c6-4 10 0 8 9-2 14-4 30-4 44z", "#FBF7EF", "#221E1A", 3),
    ("M63.5 13.5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z", "#221E1A", None, 0),
    ("M49 65c-1 22 13 38 37 41l4-7c-17-6-24-18-24-34z", "#F2C230", "#221E1A", 3),
    ("M86 99l5 1.5-2 7-5-1.5z", "#221E1A", "#221E1A", 3),
    ("M57 64c-14 0-22 14-17 32 7 1 9-13 18-22z", "#F2C230", "#221E1A", 3),
    ("M62 64c12-6 25 0 26 16-7 1-9-8-23-8z", "#F2C230", "#221E1A", 3),
    ("M54.9 37a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0zM62.4 36a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0 -4.2 0z", "#221E1A", None, 0),
    ("M57.5 42.5q3.8 3.4 7.4 -0.6", None, "#221E1A", 2.2),
]


def tokens(d):
    for m in re.finditer(r"[MmLlHhVvCcSsQqTtAaZz]|-?\d*\.?\d+(?:e-?\d+)?", d):
        yield m.group(0)


def arc_points(x1, y1, rx, ry, phi, large, sweep, x2, y2, n=24):
    # Endpoint to centre parameterisation (SVG spec F.6.5).
    phi = math.radians(phi)
    cp, sp = math.cos(phi), math.sin(phi)
    dx, dy = (x1 - x2) / 2, (y1 - y2) / 2
    x1p, y1p = cp * dx + sp * dy, -sp * dx + cp * dy
    rx, ry = abs(rx), abs(ry)
    lam = (x1p ** 2) / (rx ** 2) + (y1p ** 2) / (ry ** 2)
    if lam > 1:
        rx, ry = rx * math.sqrt(lam), ry * math.sqrt(lam)
    num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
    den = rx * rx * y1p * y1p + ry * ry * x1p * x1p
    coef = math.sqrt(max(0, num / den)) if den else 0
    if large == sweep:
        coef = -coef
    cxp, cyp = coef * rx * y1p / ry, -coef * ry * x1p / rx
    cx = cp * cxp - sp * cyp + (x1 + x2) / 2
    cy = sp * cxp + cp * cyp + (y1 + y2) / 2

    def ang(ux, uy, vx, vy):
        a = math.atan2(ux * vy - uy * vx, ux * vx + uy * vy)
        return a

    t1 = ang(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry)
    dt = ang((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry)
    if not sweep and dt > 0:
        dt -= 2 * math.pi
    elif sweep and dt < 0:
        dt += 2 * math.pi
    pts = []
    for i in range(1, n + 1):
        t = t1 + dt * i / n
        x, y = rx * math.cos(t), ry * math.sin(t)
        pts.append((cp * x - sp * y + cx, sp * x + cp * y + cy))
    return pts


def subpaths(d):
    toks = list(tokens(d))
    i, cmd = 0, None
    x = y = sx = sy = 0.0
    lastc = None
    out, cur = [], []

    def num():
        nonlocal i
        v = float(toks[i]); i += 1
        return v

    while i < len(toks):
        if re.match(r"[A-Za-z]", toks[i]):
            cmd = toks[i]; i += 1
        rel = cmd.islower()
        c = cmd.upper()
        if c == "Z":
            if cur:
                out.append((cur, True)); cur = []
            x, y = sx, sy
            lastc = None
            continue
        if c == "M":
            if cur:
                out.append((cur, False))
            nx, ny = num(), num()
            x, y = (x + nx, y + ny) if rel else (nx, ny)
            sx, sy = x, y
            cur = [(x, y)]
            cmd = "l" if rel else "L"
            lastc = None
        elif c == "L":
            nx, ny = num(), num()
            x, y = (x + nx, y + ny) if rel else (nx, ny)
            cur.append((x, y)); lastc = None
        elif c == "H":
            nx = num(); x = x + nx if rel else nx; cur.append((x, y)); lastc = None
        elif c == "V":
            ny = num(); y = y + ny if rel else ny; cur.append((x, y)); lastc = None
        elif c in "CS":
            if c == "C":
                x1, y1 = num(), num()
                if rel: x1, y1 = x + x1, y + y1
            else:
                x1, y1 = (2 * x - lastc[0], 2 * y - lastc[1]) if lastc else (x, y)
            x2, y2, ex, ey = num(), num(), num(), num()
            if rel: x2, y2, ex, ey = x + x2, y + y2, x + ex, y + ey
            for k in range(1, 21):
                t = k / 20
                mt = 1 - t
                cur.append((mt ** 3 * x + 3 * mt * mt * t * x1 + 3 * mt * t * t * x2 + t ** 3 * ex,
                            mt ** 3 * y + 3 * mt * mt * t * y1 + 3 * mt * t * t * y2 + t ** 3 * ey))
            lastc = (x2, y2); x, y = ex, ey
        elif c in "QT":
            if c == "Q":
                qx, qy = num(), num()
                if rel: qx, qy = x + qx, y + qy
            else:
                qx, qy = (2 * x - lastc[0], 2 * y - lastc[1]) if lastc else (x, y)
            ex, ey = num(), num()
            if rel: ex, ey = x + ex, y + ey
            for k in range(1, 21):
                t = k / 20
                mt = 1 - t
                cur.append((mt * mt * x + 2 * mt * t * qx + t * t * ex, mt * mt * y + 2 * mt * t * qy + t * t * ey))
            lastc = (qx, qy); x, y = ex, ey
        elif c == "A":
            rx, ry, rot, large, sweep, ex, ey = num(), num(), num(), num(), num(), num(), num()
            if rel: ex, ey = x + ex, y + ey
            cur.extend(arc_points(x, y, rx, ry, rot, int(large), int(sweep), ex, ey))
            x, y = ex, ey; lastc = None
    if cur:
        out.append((cur, False))
    return out


def main():
    img = Image.new("RGB", (SIZE * SS, SIZE * SS), BG)
    draw = ImageDraw.Draw(img)
    # Android: 108-unit viewport, mascot translated (20, 18) and scaled 0.6.
    unit = SIZE * SS / 108

    def tx(p):
        return ((20 + p[0] * 0.6) * unit, (18 + p[1] * 0.6) * unit)

    for d, fill, stroke, sw in LAYERS:
        for pts, closed in subpaths(d):
            P = [tx(p) for p in pts]
            if fill and len(P) > 2:
                draw.polygon(P, fill=fill)
            if stroke and sw:
                w = max(1, int(sw * 0.6 * unit))
                seq = P + ([P[0]] if closed else [])
                draw.line(seq, fill=stroke, width=w, joint="curve")
                r = w / 2
                for q in (seq[0], seq[-1]):
                    draw.ellipse([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=stroke)
    img = img.resize((SIZE, SIZE), Image.LANCZOS)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT, "PNG")
    print("wrote", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
