"""Render ZestLLM column icon as 200x200 PNG from SVG design spec."""
from PIL import Image, ImageDraw

SIZE = 200
S = SIZE / 32.0
OUT = r"D:\WORK\Project\zestLLM\docs\assets\zestllm-column-cover-200.png"


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def main():
    c1 = (0x66, 0x7E, 0xEA)
    c2 = (0x76, 0x4B, 0xA2)

    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    rx = 8 * S

    for y in range(SIZE):
        for x in range(SIZE):
            inside = True
            r = rx
            if x < r and y < r and (x - r) ** 2 + (y - r) ** 2 > r * r:
                inside = False
            if x > SIZE - r - 1 and y < r and (x - (SIZE - r - 1)) ** 2 + (y - r) ** 2 > r * r:
                inside = False
            if x < r and y > SIZE - r - 1 and (x - r) ** 2 + (y - (SIZE - r - 1)) ** 2 > r * r:
                inside = False
            if x > SIZE - r - 1 and y > SIZE - r - 1 and (x - (SIZE - r - 1)) ** 2 + (y - (SIZE - r - 1)) ** 2 > r * r:
                inside = False
            if inside:
                t = (x + y) / (2 * (SIZE - 1))
                px[x, y] = lerp(c1, c2, t) + (255,)

    draw = ImageDraw.Draw(img)

    cx, cy, cr = 10 * S, 16 * S, 3 * S
    draw.ellipse(
        [cx - cr, cy - cr, cx + cr, cy + cr],
        fill=(255, 255, 255, int(255 * 0.9)),
    )

    x1, y1, w, h, rr = 18 * S, 11 * S, 10 * S, 10 * S, 3 * S
    draw.rounded_rectangle(
        [x1, y1, x1 + w, y1 + h],
        radius=rr,
        fill=(255, 255, 255, int(255 * 0.85)),
    )

    draw.line(
        [(13 * S, 16 * S), (18 * S, 16 * S)],
        fill=(255, 255, 255, int(255 * 0.75)),
        width=max(1, int(1.4 * S)),
    )

    tri = [(23 * S, 14 * S), (26 * S, 16 * S), (23 * S, 18 * S)]
    draw.polygon(tri, fill=(255, 255, 255, int(255 * 0.8)))

    img.save(OUT, "PNG", optimize=True)
    print(f"saved {OUT} {img.size}")


if __name__ == "__main__":
    main()
