package gg.shard.client.cosmetics;

import java.util.ArrayList;
import java.util.List;

/**
 * The bandana's geometry and how the square artwork is cut onto it. Pure maths, unit-tested.
 *
 * <p>Coordinates are the player model's head space in model pixels: the head cube spans x and z
 * -4..4 and y -8..0 (y grows downwards, the face looks towards -z), the hat layer sits 0.5 px
 * outside it. The bandana is a thin shell {@link #SHELL} px outside the head, like a do-rag:
 * <ul>
 *   <li>top: the centre of the art (the ringed-moon emblem), upright for someone in front;</li>
 *   <li>a narrow forehead band continuing the top's front edge into the art's bottom border;</li>
 *   <li>sides: the art's left and right borders, joined seamlessly to the top's side edges, reaching
 *       down further towards the back of the head;</li>
 *   <li>back: the lower half of the art (ring, MINECRAFT, crescents and border), upright from behind;</li>
 *   <li>a small knot at the back with two short tails hanging from it.</li>
 * </ul>
 * Texture coordinates are fractions of the square art (0..1, v downwards).
 */
public final class BandanaMesh {
    private BandanaMesh() {}

    /** Distance from the head surface to the shell (the hat layer is at 0.5). */
    public static final float SHELL = 0.6f;
    /** Half the shell's width: head half-width plus {@link #SHELL}. */
    public static final float E = 4f + SHELL;
    public static final float TOP = -8f - SHELL;
    /** Lower edge of the forehead band (eyes are at y -4..-3; this stays on the hairline). */
    public static final float FRONT_HEM = -6.35f;
    /** Lower edge at the back of the head. */
    public static final float BACK_HEM = -3.2f;

    /**
     * The art square the top shows: everything inside the moon-and-star border (the dotted frame,
     * the four crescents and the emblem). Its edges sit just inside the border's inner line.
     */
    public static final float TOP_U0 = 0.125f, TOP_U1 = 0.875f, TOP_V0 = 0.125f, TOP_V1 = 0.875f;
    /** The forehead band continues the top downwards through the art's bottom border to its stitched edge. */
    public static final float BAND_V1 = 0.988f;
    /** Outer edge of the art used at every hem (just outside the dashed stitching). */
    public static final float BORDER_OUT = 0.012f;
    /** The back panel: the lower half of the art, a little narrower than the whole. */
    public static final float BACK_U0 = 0.1f, BACK_U1 = 0.9f, BACK_V0 = 0.5f, BACK_V1 = 0.988f;

    /** One quad: four corners of x, y, z, u, v (20 floats) and the outward normal. */
    public record Quad(float[] v, float nx, float ny, float nz) {
        public float x(int i) { return v[i * 5]; }
        public float y(int i) { return v[i * 5 + 1]; }
        public float z(int i) { return v[i * 5 + 2]; }
        public float u(int i) { return v[i * 5 + 3]; }
        public float w(int i) { return v[i * 5 + 4]; }
    }

    /** A point of a face: position and art coordinates. */
    record P(float x, float y, float z, float u, float v) {
        P lerp(P o, float t) {
            return new P(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t, u + (o.u - u) * t, v + (o.v - v) * t);
        }
    }

    /** Lower edge of the side at depth {@code z}: from the forehead band down to the back hem. */
    public static float sideHem(float z) {
        float t = (z + E) / (2 * E);
        // Eased so the line stays high over the temples and drops behind the ears.
        t = t * t * (3 - 2 * t);
        return FRONT_HEM + (BACK_HEM - FRONT_HEM) * t;
    }

    /** Art v along the top (and the sides' upper edge) at depth {@code z}: back edge TOP_V0, front TOP_V1. */
    public static float topV(float z) {
        return TOP_V0 + (TOP_V1 - TOP_V0) * (E - z) / (2 * E);
    }

    public static List<Quad> build() {
        List<Quad> out = new ArrayList<>();
        // Top. Corners: a = back-left (-x), b = back-right, c = front-left, d = front-right.
        grid(out, new P(-E, TOP, E, TOP_U0, TOP_V0), new P(E, TOP, E, TOP_U1, TOP_V0),
                new P(-E, TOP, -E, TOP_U0, TOP_V1), new P(E, TOP, -E, TOP_U1, TOP_V1), 4, 4, 0, -1, 0);
        // Forehead band, from the top's front edge down to the hem.
        grid(out, new P(-E, TOP, -E, TOP_U0, TOP_V1), new P(E, TOP, -E, TOP_U1, TOP_V1),
                new P(-E, FRONT_HEM, -E, TOP_U0, BAND_V1), new P(E, FRONT_HEM, -E, TOP_U1, BAND_V1), 4, 1, 0, 0, -1);
        // Sides, in slices so the slanted hem keeps the border straight along it.
        int slices = 10;
        for (int side = -1; side <= 1; side += 2) {
            float x = side * E;
            float uIn = side < 0 ? TOP_U0 : TOP_U1;
            float uOut = side < 0 ? BORDER_OUT : 1 - BORDER_OUT;
            for (int i = 0; i < slices; i++) {
                float z0 = -E + 2 * E * i / slices;
                float z1 = -E + 2 * E * (i + 1) / slices;
                P a = new P(x, TOP, z0, uIn, topV(z0));
                P b = new P(x, TOP, z1, uIn, topV(z1));
                P c = new P(x, sideHem(z0), z0, uOut, topV(z0));
                P d = new P(x, sideHem(z1), z1, uOut, topV(z1));
                quad(out, a, b, d, c, side, 0, 0);
            }
        }
        // Back: upright for someone behind, whose left is the player's left (+x).
        grid(out, new P(E, TOP, E, BACK_U0, BACK_V0), new P(-E, TOP, E, BACK_U1, BACK_V0),
                new P(E, BACK_HEM, E, BACK_U0, BACK_V1), new P(-E, BACK_HEM, E, BACK_U1, BACK_V1), 4, 2, 0, 0, 1);
        knot(out);
        return out;
    }

    /** Knot size and place: centred on the back, sitting on the hem. */
    public static final float KNOT_W = 2.2f, KNOT_H = 1.7f, KNOT_D = 1.0f, KNOT_Y = BACK_HEM - 0.75f;
    /** Plain night sky with a few stars, for the knot's sides. */
    static final float KNOT_U0 = 0.30f, KNOT_U1 = 0.40f, KNOT_V0 = 0.15f, KNOT_V1 = 0.23f;
    /** The star-in-a-ring medallion from the art's corner, on the knot's back. */
    static final float MEDAL_U0 = 0.05f, MEDAL_U1 = 0.11f, MEDAL_V0 = 0.057f, MEDAL_V1 = 0.103f;
    /** Tails: length, width, how far they splay sideways and lean back (degrees). */
    public static final float TAIL_LEN = 3.6f, TAIL_W = 1.3f, TAIL_SPLAY = 20f, TAIL_LEAN = 22f;

    private static void knot(List<Quad> out) {
        float x0 = -KNOT_W / 2, x1 = KNOT_W / 2;
        float y0 = KNOT_Y - KNOT_H / 2, y1 = KNOT_Y + KNOT_H / 2;
        float z0 = E - 0.05f, z1 = E + KNOT_D;
        // Back face (+z), top, bottom, left and right; the front touches the back panel.
        quad(out, new P(x1, y0, z1, MEDAL_U0, MEDAL_V0), new P(x0, y0, z1, MEDAL_U1, MEDAL_V0),
                new P(x0, y1, z1, MEDAL_U1, MEDAL_V1), new P(x1, y1, z1, MEDAL_U0, MEDAL_V1), 0, 0, 1);
        quad(out, new P(x0, y0, z0, KNOT_U0, KNOT_V0), new P(x1, y0, z0, KNOT_U1, KNOT_V0),
                new P(x1, y0, z1, KNOT_U1, KNOT_V1), new P(x0, y0, z1, KNOT_U0, KNOT_V1), 0, -1, 0);
        quad(out, new P(x0, y1, z1, KNOT_U0, KNOT_V0), new P(x1, y1, z1, KNOT_U1, KNOT_V0),
                new P(x1, y1, z0, KNOT_U1, KNOT_V1), new P(x0, y1, z0, KNOT_U0, KNOT_V1), 0, 1, 0);
        quad(out, new P(x0, y0, z0, KNOT_U0, KNOT_V0), new P(x0, y0, z1, KNOT_U1, KNOT_V0),
                new P(x0, y1, z1, KNOT_U1, KNOT_V1), new P(x0, y1, z0, KNOT_U0, KNOT_V1), -1, 0, 0);
        quad(out, new P(x1, y0, z1, KNOT_U0, KNOT_V0), new P(x1, y0, z0, KNOT_U1, KNOT_V0),
                new P(x1, y1, z0, KNOT_U1, KNOT_V1), new P(x1, y1, z1, KNOT_U0, KNOT_V1), 1, 0, 0);
        // Two tails from under the knot, splayed and leaning away from the neck.
        for (int side = -1; side <= 1; side += 2) {
            double splay = Math.toRadians(TAIL_SPLAY) * side;
            double lean = Math.toRadians(TAIL_LEAN);
            // Direction down the tail (y down), sideways by splay, backwards by lean.
            float dx = (float) (Math.sin(splay) * Math.cos(lean));
            float dy = (float) (Math.cos(splay) * Math.cos(lean));
            float dz = (float) Math.sin(lean);
            // Across the tail: horizontal, perpendicular to the splay.
            float ax = (float) Math.cos(splay), ay = (float) -Math.sin(splay);
            float sx = side * KNOT_W * 0.22f, sy = y1 - 0.2f, sz = E + KNOT_D * 0.55f;
            float hw = TAIL_W / 2;
            float ex = sx + dx * TAIL_LEN, ey = sy + dy * TAIL_LEN, ez = sz + dz * TAIL_LEN;
            // Normal of the tail plane, pointing away from the head (+z-ish).
            float nx = ay * dz, ny = -ax * dz, nz = ax * dy - ay * dx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= len;
            ny /= len;
            nz /= len;
            if (nz < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
            // A strip of the side border, so the tails carry the little moons.
            float u0 = side < 0 ? BORDER_OUT : TOP_U1, u1 = side < 0 ? TOP_U0 : 1 - BORDER_OUT;
            quad(out, new P(sx - ax * hw, sy - ay * hw, sz, u0, 0.3f), new P(sx + ax * hw, sy + ay * hw, sz, u1, 0.3f),
                    new P(ex + ax * hw, ey + ay * hw, ez, u1, 0.7f), new P(ex - ax * hw, ey - ay * hw, ez, u0, 0.7f), nx, ny, nz);
        }
    }

    /** A face from four corners (a b on the first edge, c d on the opposite one), split nu x nv. */
    private static void grid(List<Quad> out, P a, P b, P c, P d, int nu, int nv, float nx, float ny, float nz) {
        for (int j = 0; j < nv; j++) {
            float t0 = (float) j / nv, t1 = (float) (j + 1) / nv;
            P l0 = a.lerp(c, t0), r0 = b.lerp(d, t0), l1 = a.lerp(c, t1), r1 = b.lerp(d, t1);
            for (int i = 0; i < nu; i++) {
                float s0 = (float) i / nu, s1 = (float) (i + 1) / nu;
                quad(out, l0.lerp(r0, s0), l0.lerp(r0, s1), l1.lerp(r1, s1), l1.lerp(r1, s0), nx, ny, nz);
            }
        }
    }

    private static void quad(List<Quad> out, P a, P b, P c, P d, float nx, float ny, float nz) {
        float[] v = new float[20];
        P[] ps = {a, b, c, d};
        for (int i = 0; i < 4; i++) {
            v[i * 5] = ps[i].x;
            v[i * 5 + 1] = ps[i].y;
            v[i * 5 + 2] = ps[i].z;
            v[i * 5 + 3] = ps[i].u;
            v[i * 5 + 4] = ps[i].v;
        }
        out.add(new Quad(v, nx, ny, nz));
    }
}
