import {Chessboard, COLOR, INPUT_EVENT_TYPE, FEN} from "https://cdn.jsdelivr.net/npm/cm-chessboard@8.15.4/src/Chessboard.js";
import {Markers, MARKER_TYPE} from "https://cdn.jsdelivr.net/npm/cm-chessboard@8.15.4/src/extensions/markers/Markers.js";
import {PromotionDialog, PROMOTION_DIALOG_RESULT_TYPE} from "https://cdn.jsdelivr.net/npm/cm-chessboard@8.15.4/src/extensions/promotion-dialog/PromotionDialog.js";
import * as rules from "./alfil.js";

const MATE = 30000;
const MAX_PLY = 128;

const STATUS_TEXT = {
    WHITE_WINS_BY_CHECKMATE: "Jaque mate. Ganan las blancas.",
    BLACK_WINS_BY_CHECKMATE: "Jaque mate. Ganan las negras.",
    DRAW_BY_STALEMATE: "Tablas por ahogado.",
    DRAW_BY_REPETITION: "Tablas por triple repetición.",
    DRAW_BY_FIFTY_MOVES: "Tablas por la regla de las 50 jugadas.",
    DRAW_BY_INSUFFICIENT_MATERIAL: "Tablas por material insuficiente.",
};

const el = {
    color: document.getElementById("color"),
    time: document.getElementById("time"),
    newGame: document.getElementById("new-game"),
    undo: document.getElementById("undo"),
    status: document.getElementById("status"),
    info: document.getElementById("info"),
    moves: document.getElementById("moves"),
};

const board = new Chessboard(document.getElementById("board"), {
    position: FEN.start,
    assetsUrl: "https://cdn.jsdelivr.net/npm/cm-chessboard@8.15.4/assets/",
    style: {cssClass: "green", animationDuration: 200},
    extensions: [
        {class: Markers, props: {autoMarkers: MARKER_TYPE.square}},
        {class: PromotionDialog},
    ],
});

const worker = new Worker(new URL("./engine-worker.js", import.meta.url), {type: "module"});

const game = {
    playerColor: COLOR.white,
    moves: [],
    sans: [],
    searchId: 0,
    thinking: false,
};

worker.onmessage = (event) => {
    if (event.data.ready) {
        newGame();
        return;
    }
    const result = event.data;
    if (result.id !== game.searchId) {
        return; // answer to a position that no longer exists (new game or undo)
    }
    game.thinking = false;
    showInfo(result);
    if (result.bestMove !== "none") {
        playMove(result.bestMove);
    }
    afterMove();
};

function newGame() {
    const choice = el.color.value;
    game.playerColor = choice === "r" ? (Math.random() < 0.5 ? COLOR.white : COLOR.black) : choice;
    game.moves = [];
    game.sans = [];
    game.searchId++;
    game.thinking = false;
    rules.newGame("");
    el.info.textContent = "";
    board.setOrientation(game.playerColor);
    board.setPosition(rules.fen(), true);
    renderMoves();
    afterMove();
}

function undo() {
    if (game.moves.length === 0) {
        return;
    }
    // Take back moves until it is our turn again (usually the engine's reply and ours).
    const replay = game.moves.slice();
    do {
        replay.pop();
    } while (replay.length > 0 && (replay.length % 2 === 0 ? COLOR.white : COLOR.black) !== game.playerColor);
    game.searchId++;
    game.thinking = false;
    game.moves = [];
    game.sans = [];
    rules.newGame("");
    for (const move of replay) {
        playMove(move);
    }
    board.setPosition(rules.fen(), true);
    el.info.textContent = "";
    afterMove();
}

function sideToMove() {
    return rules.fen().split(" ")[1];
}

function playMove(uci) {
    const san = rules.san(uci);
    if (!rules.play(uci)) {
        return false;
    }
    game.moves.push(uci);
    game.sans.push(san);
    return true;
}

function afterMove() {
    board.setPosition(rules.fen(), true);
    renderMoves();
    markCheck();
    board.disableMoveInput();

    const status = rules.status();
    el.undo.disabled = game.moves.length === 0;
    if (status !== "PLAYING") {
        el.status.textContent = STATUS_TEXT[status];
        return;
    }
    if (sideToMove() === game.playerColor) {
        el.status.textContent = rules.inCheck() ? "Estás en jaque. Te toca." : "Te toca.";
        board.enableMoveInput(onMoveInput, game.playerColor);
    } else {
        el.status.textContent = "El motor está pensando…";
        game.thinking = true;
        worker.postMessage({id: game.searchId, fen: "", moves: game.moves, millis: Number(el.time.value)});
    }
}

function legalMovesFrom(square) {
    return rules.legalMoves().split(" ").filter((m) => m.startsWith(square));
}

function onMoveInput(event) {
    switch (event.type) {
        case INPUT_EVENT_TYPE.moveInputStarted: {
            const moves = legalMovesFrom(event.squareFrom);
            board.addLegalMovesMarkers(moves.map((m) => ({to: m.substring(2, 4), promotion: m[4]})));
            return moves.length > 0;
        }
        case INPUT_EVENT_TYPE.validateMoveInput: {
            board.removeLegalMovesMarkers();
            const uci = event.squareFrom + event.squareTo;
            const candidates = legalMovesFrom(event.squareFrom).filter((m) => m.startsWith(uci));
            if (candidates.length === 0) {
                return false;
            }
            if (candidates.length > 1) {
                // Promotion: let the player choose the piece.
                board.showPromotionDialog(event.squareTo, game.playerColor, (result) => {
                    if (result.type === PROMOTION_DIALOG_RESULT_TYPE.pieceSelected) {
                        playMove(uci + result.piece.charAt(1));
                    }
                    afterMove();
                });
                return true;
            }
            playMove(uci);
            setTimeout(afterMove, 0);
            return true;
        }
        case INPUT_EVENT_TYPE.moveInputCanceled:
            board.removeLegalMovesMarkers();
            return true;
    }
    return true;
}

function markCheck() {
    board.removeMarkers(MARKER_TYPE.circleDangerFilled);
    if (!rules.inCheck()) {
        return;
    }
    const placement = rules.fen().split(" ")[0];
    const king = sideToMove() === COLOR.white ? "K" : "k";
    let rank = 7;
    let file = 0;
    for (const c of placement) {
        if (c === "/") {
            rank--;
            file = 0;
        } else if (c >= "1" && c <= "8") {
            file += Number(c);
        } else {
            if (c === king) {
                board.addMarker(MARKER_TYPE.circleDangerFilled, "abcdefgh"[file] + (rank + 1));
            }
            file++;
        }
    }
}

function renderMoves() {
    el.moves.innerHTML = "";
    for (let i = 0; i < game.sans.length; i += 2) {
        const li = document.createElement("li");
        for (const san of game.sans.slice(i, i + 2)) {
            const span = document.createElement("span");
            span.textContent = san;
            li.appendChild(span);
        }
        el.moves.appendChild(li);
    }
    el.moves.scrollTop = el.moves.scrollHeight;
}

function showInfo(r) {
    // The score is from the engine's point of view; show it from white's.
    const engineIsWhite = game.playerColor === COLOR.black;
    let text;
    if (Math.abs(r.score) >= MATE - MAX_PLY) {
        const moves = Math.ceil((MATE - Math.abs(r.score)) / 2);
        const engineWins = r.score > 0;
        text = engineWins ? `El motor ve mate en ${moves}` : `El motor ve que recibe mate en ${moves}`;
    } else {
        const white = (engineIsWhite ? r.score : -r.score) / 100;
        text = `Evaluación ${white > 0 ? "+" : ""}${white.toFixed(2)}`;
    }
    const kNodes = Math.round(r.nodes / 1000).toLocaleString("es-ES");
    el.info.textContent = `${text} · profundidad ${r.depth} · ${kNodes} mil posiciones`;
}

el.newGame.addEventListener("click", newGame);
el.undo.addEventListener("click", undo);
