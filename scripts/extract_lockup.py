#!/usr/bin/env python3
"""Recreate the transparent app lockup from the approved source image."""

from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets" / "source" / "rp_lockup_checkerboard.png"
OUTPUTS = [
    ROOT / "assets" / "branding" / "jesty_rp_header_lockup.png",
    ROOT / "res" / "drawable-nodpi" / "jesty_rp_header_lockup.png",
]


def extract(path: Path) -> Image.Image:
    rgb = np.asarray(Image.open(path).convert("RGB")).astype(np.int16)
    hi, lo = rgb.max(axis=2), rgb.min(axis=2)
    saturation = (hi - lo) / np.maximum(hi, 1)
    candidate = (hi >= 68) & (hi <= 249) & (saturation < 0.38)
    height, width = candidate.shape
    background = np.zeros_like(candidate)
    queue = deque()
    for x in range(width):
        queue.extend(((0, x), (height - 1, x)))
    for y in range(height):
        queue.extend(((y, 0), (y, width - 1)))
    while queue:
        y, x = queue.popleft()
        if background[y, x] or not candidate[y, x]:
            continue
        background[y, x] = True
        if y: queue.append((y - 1, x))
        if y + 1 < height: queue.append((y + 1, x))
        if x: queue.append((y, x - 1))
        if x + 1 < width: queue.append((y, x + 1))
    alpha = np.where(background, 0, 255).astype(np.uint8)
    result = Image.fromarray(np.dstack((rgb.astype(np.uint8), alpha)), "RGBA")
    return result.crop(result.getbbox())


lockup = extract(SOURCE)
for output in OUTPUTS:
    output.parent.mkdir(parents=True, exist_ok=True)
    lockup.save(output, optimize=True)
