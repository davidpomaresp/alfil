package io.github.davidpomaresp.alfil;

import java.util.ArrayList;
import java.util.List;

/**
 * Every number the evaluation uses, in one array, so a tuner can change them.
 * Each block is a run of consecutive values; the constant is the index of its first one.
 * Values are in centipawns.
 */
final class EvalParams {

    private static final List<String> NAMES = new ArrayList<>();
    private static final List<Integer> DEFAULTS = new ArrayList<>();

    // Material by piece type (index 0 = pawn ... 4 = queen).
    static final int MATERIAL_MG = block("material_mg", 100, 320, 330, 500, 900);
    static final int MATERIAL_EG = block("material_eg", 100, 320, 330, 500, 900);

    // Square bonuses, built from a few numbers rather than 64 per piece.
    static final int PAWN_ADVANCE_MG = block("pawn_advance_mg", 3);
    static final int PAWN_CENTER_MG = block("pawn_center_mg", 15);
    static final int PAWN_RANK_EG = block("pawn_rank_eg", 0, 5, 10, 20, 35, 60);     // ranks 2..7
    static final int KNIGHT_RING = block("knight_ring", 20, 10, 0, -25);              // centre .. edge
    static final int BISHOP_RING = block("bishop_ring", 10, 8, 0, -10);
    static final int ROOK_SEVENTH_MG = block("rook_seventh_mg", 15);
    static final int ROOK_SEVENTH_EG = block("rook_seventh_eg", 10);
    static final int ROOK_CENTER_FILE_MG = block("rook_center_file_mg", 5);
    static final int QUEEN_RING = block("queen_ring", 5, 3, 0, -5);
    static final int KING_FILE_MG = block("king_file_mg", 15, 20, 10, 0, -5, 0, 25, 15); // a..h on first rank
    static final int KING_RANK_MG = block("king_rank_mg", -20);                          // per rank up
    static final int KING_RING_EG = block("king_ring_eg", 25, 12, 0, -20);

    static final int BISHOP_PAIR_MG = block("bishop_pair_mg", 30);
    static final int BISHOP_PAIR_EG = block("bishop_pair_eg", 30);
    static final int PASSED_MG = block("passed_mg", 5, 10, 15, 25, 40, 60);              // ranks 2..7
    static final int PASSED_EG = block("passed_eg", 10, 15, 25, 45, 70, 110);
    static final int DOUBLED_MG = block("doubled_mg", -10);
    static final int DOUBLED_EG = block("doubled_eg", -20);
    static final int ISOLATED_MG = block("isolated_mg", -10);
    static final int ISOLATED_EG = block("isolated_eg", -15);
    static final int ROOK_OPEN_MG = block("rook_open_mg", 20);
    static final int ROOK_OPEN_EG = block("rook_open_eg", 10);
    static final int ROOK_SEMI_OPEN_MG = block("rook_semi_open_mg", 10);
    static final int ROOK_SEMI_OPEN_EG = block("rook_semi_open_eg", 5);
    static final int KING_SHIELD_MG = block("king_shield_mg", 12);
    static final int MOBILITY_MG = block("mobility_mg", 4, 5, 2, 1);                    // N, B, R, Q
    static final int MOBILITY_EG = block("mobility_eg", 4, 5, 4, 2);

    static final int COUNT = NAMES.size();

    private EvalParams() {
    }

    private static int block(String name, int... values) {
        int start = NAMES.size();
        for (int i = 0; i < values.length; i++) {
            NAMES.add(values.length == 1 ? name : name + "[" + i + "]");
            DEFAULTS.add(values[i]);
        }
        return start;
    }

    static int[] defaults() {
        int[] values = new int[DEFAULTS.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = DEFAULTS.get(i);
        }
        return values;
    }

    static String name(int index) {
        return NAMES.get(index);
    }
}
