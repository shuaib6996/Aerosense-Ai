// Test normalization with synthetic hand landmarks
const landmarksFar = [
    { x: 0.50, y: 0.55, z: 0 }, // 0: wrist
    { x: 0.48, y: 0.53, z: 0 }, // 1
    { x: 0.47, y: 0.52, z: 0 }, // 2
    { x: 0.46, y: 0.51, z: 0 }, // 3
    { x: 0.45, y: 0.50, z: 0 }, // 4
    { x: 0.49, y: 0.50, z: 0 }, // 5: index mcp
    { x: 0.49, y: 0.47, z: 0 }, // 6
    { x: 0.49, y: 0.44, z: 0 }, // 7
    { x: 0.49, y: 0.41, z: 0 }, // 8: index tip
    { x: 0.50, y: 0.49, z: 0 }, // 9: mid mcp
    { x: 0.50, y: 0.45, z: 0 }, // 10
    { x: 0.50, y: 0.42, z: 0 }, // 11
    { x: 0.50, y: 0.39, z: 0 }, // 12
    { x: 0.51, y: 0.50, z: 0 }, // 13: ring mcp
    { x: 0.51, y: 0.46, z: 0 }, // 14
    { x: 0.51, y: 0.43, z: 0 }, // 15
    { x: 0.51, y: 0.40, z: 0 }, // 16
    { x: 0.52, y: 0.51, z: 0 }, // 17: pinky mcp
    { x: 0.52, y: 0.48, z: 0 }, // 18
    { x: 0.52, y: 0.45, z: 0 }, // 19
    { x: 0.52, y: 0.42, z: 0 }  // 20
];

// User close (landmarks are 3x larger)
const landmarksClose = landmarksFar.map(pt => ({
    x: 0.5 + (pt.x - 0.5) * 3,
    y: 0.5 + (pt.y - 0.5) * 3,
    z: pt.z * 3
}));

function normalize(lm, w, h, userScale = 1.0) {
    const palmLength = Math.hypot(lm[9].x - lm[0].x, lm[9].y - lm[0].y);
    const palmWidth = Math.hypot(lm[17].x - lm[5].x, lm[17].y - lm[5].y);
    const rawSpan = Math.max(0.035, (palmLength * 0.7) + (palmWidth * 0.5));

    const centerRawX = (lm[0].x + lm[5].x + lm[9].x + lm[17].x) * 0.25;
    const centerRawY = (lm[0].y + lm[5].y + lm[9].y + lm[17].y) * 0.25;
    const mirrorCenterX = 1 - centerRawX;

    const baseTarget = Math.min(w, h) * 0.36;
    const targetSpan = baseTarget * userScale;
    const scaleFactor = targetSpan / rawSpan;

    const cx = w * 0.5;
    const cy = h * 0.54;

    return lm.map(pt => ({
        x: Math.round((cx + ((1 - pt.x) - mirrorCenterX) * scaleFactor) * 10) / 10,
        y: Math.round((cy + (pt.y - centerRawY) * scaleFactor) * 10) / 10
    }));
}

const w = 68, h = 84;
const farNorm = normalize(landmarksFar, w, h);
const closeNorm = normalize(landmarksClose, w, h);

console.log('Far Wrist:', farNorm[0], 'Close Wrist:', closeNorm[0]);
console.log('Far Index Tip:', farNorm[8], 'Close Index Tip:', closeNorm[8]);
console.log('Difference in span:', Math.hypot(farNorm[8].x - farNorm[0].x, farNorm[8].y - farNorm[0].y),
    Math.hypot(closeNorm[8].x - closeNorm[0].x, closeNorm[8].y - closeNorm[0].y));
