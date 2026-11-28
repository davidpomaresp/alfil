// Runs the search away from the page so the board stays responsive while the engine thinks.
import * as engine from "./alfil.js";

self.onmessage = (event) => {
    const {id, fen, moves, millis} = event.data;
    engine.newGame(fen);
    for (const move of moves) {
        engine.play(move);
    }
    const [bestMove, score, depth, nodes, elapsed] = engine.think(64, millis).split(" ");
    self.postMessage({
        id,
        bestMove,
        score: Number(score),
        depth: Number(depth),
        nodes: Number(nodes),
        millis: Number(elapsed),
    });
};

self.postMessage({ready: true});
