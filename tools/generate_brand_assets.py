"""Draw original Metric Laundry assets; no external image downloads are required."""
from pathlib import Path
from PIL import Image, ImageDraw
import math

ROOT = Path(__file__).resolve().parents[1] / "src/resources/images"
ROOT.mkdir(parents=True, exist_ok=True)
GREEN, DARK, MINT = "#1E8E3E", "#145A32", "#C8E6C9"


def canvas(w, h):
    image = Image.new("RGBA", (w * 3, h * 3))
    return image, ImageDraw.Draw(image)


def save(image, name, w, h):
    image.resize((w, h), Image.Resampling.LANCZOS).save(ROOT / name)


image, d = canvas(64, 64)
d.arc((66, 12, 126, 78), 180, 385, fill=GREEN, width=9)
d.line([(99, 69), (24, 123), (24, 135), (165, 135), (165, 123), (99, 78)], fill=GREEN, width=9, joint="curve")
d.ellipse((117, 30, 170, 62), fill=GREEN)
d.line((126, 63, 158, 35), fill=DARK, width=3)
save(image, "logo.png", 64, 64)


def basket(name, w=240, h=160):
    image, d = canvas(w, h)
    # Draw at 3x resolution in a 720 by 480 coordinate system.
    d.ellipse((105, 403, 648, 455), fill=(20, 90, 50, 24))
    d.rounded_rectangle((203, 37, 404, 313), 35, fill="#FFFFFF")
    d.polygon([(203, 55), (149, 98), (184, 160), (220, 143), (220, 308), (389, 308), (389, 140), (421, 160), (456, 98), (404, 55), (357, 71), (249, 71)], fill="#FFFFFF")
    d.arc((255, 29, 357, 110), 0, 180, fill=MINT, width=11)
    d.rounded_rectangle((369, 118, 558, 312), 29, fill="#A7D9B4")
    d.rounded_rectangle((404, 137, 579, 322), 25, fill="#D8EEDB")
    d.line((422, 157, 555, 157), fill="#8DC79C", width=8)
    d.polygon([(123, 230), (623, 230), (579, 412), (169, 412)], fill=GREEN)
    d.rounded_rectangle((110, 217, 636, 258), 18, fill="#50AB69")
    d.arc((237, 152, 498, 294), 180, 360, fill=DARK, width=16)
    for x in range(180, 580, 43):
        d.line((x, 278, x + 12, 385), fill="#6EBA7F", width=11)
    d.line((165, 310, 595, 310), fill="#6EBA7F", width=8)
    d.line((169, 355, 584, 355), fill="#6EBA7F", width=8)
    save(image, name, w, h)


basket("basket.png")

for name, kind in [("washing.png", "wash"), ("dry-cleaning.png", "hanger"), ("folding.png", "fold")]:
    image, d = canvas(140, 92)
    d.ellipse((70, 15, 350, 265), fill="#E8F5E9")
    if kind == "wash":
        d.rounded_rectangle((130, 24, 293, 252), 20, fill="white", outline=GREEN, width=7)
        d.line((135, 76, 287, 76), fill=MINT, width=6)
        d.ellipse((153, 96, 269, 214), fill="#C8E6C9", outline=GREEN, width=7)
        d.arc((162, 123, 260, 198), 0, 180, fill=GREEN, width=6)
        d.ellipse((150, 43, 163, 56), fill=GREEN)
        d.line((233, 48, 269, 48), fill=GREEN, width=6)
    elif kind == "hanger":
        d.arc((181, 29, 238, 91), 180, 390, fill=GREEN, width=8)
        d.line([(211, 83), (99, 177), (99, 196), (322, 196), (322, 177), (211, 95)], fill=GREEN, width=8, joint="curve")
        d.line((156, 192, 156, 245), fill="#91CBA0", width=6)
        d.line((264, 192, 264, 245), fill="#91CBA0", width=6)
    else:
        for y, color in [(165, "#75B987"), (120, "#A7D9B4"), (75, "#E0EFE3")]:
            d.rounded_rectangle((106, y, 315, y + 57), 15, fill=color, outline=GREEN, width=4)
            d.line((132, y + 40, 289, y + 40), fill=GREEN, width=3)
    save(image, name, 140, 92)

image, d = canvas(170, 170)
for i in range(7):
    x, y = 15 + i * 17, 158 - i * 19
    d.ellipse((x * 3, (y - 31) * 3, (x + 43) * 3, y * 3), fill=(85, 152, 92, 50 + i * 8))
    d.ellipse(((x - 23) * 3, (y - 12) * 3, (x + 13) * 3, (y + 12) * 3), fill=(85, 152, 92, 45 + i * 7))
d.line((35, 495, 393, 93), fill=(30, 142, 62, 110), width=5)
save(image, "leaves.png", 170, 170)
print(f"Created six original PNG assets in {ROOT}")
