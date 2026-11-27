# Alfil

Un motor de ajedrez que estoy haciendo en Java desde cero.

**[Juega contra él en el navegador](https://davidpomaresp.github.io/alfil/)**. Eliges color y juegas. No hay servidor: el motor corre en tu propio navegador.

Ahora mismo juega a unos 2.300 Elo contra otros motores. Lo calculé enfrentándolo a Stockfish con la fuerza limitada, así que tómalo como una aproximación. No es su Elo contra personas.

## Qué hace

- Genera todas las jugadas legales, incluidos el enroque, la captura al paso y las promociones. 
- Busca con alfa-beta y profundización iterativa. Encima lleva lo habitual en un motor: tabla de transposición, búsqueda de quietud, movimiento nulo, LMR, ventanas de aspiración...
- Evalúa las posiciones con material, la casilla de cada pieza, la estructura de peones, la movilidad y la seguridad del rey.
- Habla UCI, así que puedes usarlo en Arena, Cute Chess o cualquier interfaz de ajedrez.

## Cómo lo mido

Cada cambio lo pruebo antes de quedármelo. Comparo nodos a la misma profundidad y juego partidas contra la versión anterior, con las mismas aperturas y alternando colores. Desde hace poco uso un SPRT para decidir cuándo hay suficientes partidas. Algunas ideas no funcionaron, como ajustar la evaluación con Texel tuning, y también lo dejo apuntado.

Los resultados están en [docs/experimentos](docs/experimentos).



## Cómo está organizado

- `engine`: el motor en Java puro, con sus pruebas.
- `web`: la página. [TeaVM](https://teavm.org) compila el mismo código del motor a JavaScript, y la búsqueda va en un Web Worker para que el tablero no se congele. El tablero lo dibuja [cm-chessboard](https://github.com/shaack/cm-chessboard).

## Lo próximo

- Ponerlo como bot en Lichess para saber su Elo de verdad contra personas.
- Cambiar el tablero a bitboards para que vaya más rápido.
