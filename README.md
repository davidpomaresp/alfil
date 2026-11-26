# Alfil

Motor de ajedrez escrito en Java desde cero.

**[Juega contra el motor en tu navegador](https://davidpomaresp.github.io/alfil/)**: eliges color
y juegas una partida completa. El motor se ejecuta en tu propio navegador, sin servidor.

## Cómo ejecutar las pruebas

Necesitas Java 21 y Maven.

```bash
mvn test
```

Para contar nodos con perft desde la línea de comandos (profundidad y FEN opcional):

```bash
mvn -q compile
java -cp engine/target/classes io.github.davidpomaresp.alfil.Perft 5
```

## Representación del tablero

Las casillas van de 0 a 63, empezando en a1 y avanzando por filas
(a1 = 0, h1 = 7, a8 = 56, h8 = 63). Cada casilla guarda un número
pequeño: los 3 bits bajos indican el tipo de pieza y el cuarto bit el color.

## Cómo está hecho

El proyecto tiene dos módulos de Maven:

- `engine`: el motor en Java puro (tablero, jugadas, evaluación y búsqueda), con sus pruebas.
- `web`: la página. TeaVM compila el mismo código del motor a JavaScript, y la
  búsqueda corre en un Web Worker para que el tablero no se congele mientras piensa.
  El tablero se dibuja con [cm-chessboard](https://github.com/shaack/cm-chessboard).

Para probar la página en local:

```bash
mvn install
cd web/target/site && python3 -m http.server 8000
# abre http://localhost:8000
```

## Generación de jugadas

Primero se generan las jugadas pseudolegales (cada pieza según sus reglas) y
después se descartan las que dejan al propio rey en jaque: se hace la jugada,
se mira si el rey está atacado y se deshace. Es más lento que generar solo
jugadas legales, pero mucho más difícil de equivocarse.

La generación se comprueba con perft contra los valores publicados de seis
posiciones de referencia y catorce posiciones que prueban una regla concreta
(al paso ilegal por clavada, enroque con jaque, promociones, ahogado...).
Desde la posición inicial, perft(6) = 119.060.324 nodos en unos 9 segundos.

## Búsqueda y evaluación

- Negamax con poda alfa-beta y profundización iterativa: busca a profundidad 1, 2, 3...
  hasta que se acaba el tiempo y juega la mejor jugada de la última profundidad completa.
- Búsqueda de quietud: al llegar al final sigue mirando capturas, para no evaluar a mitad
  de un cambio de piezas.
- Evaluación: material, un bonus según la casilla de cada pieza (distinto en medio juego
  y en final) y la pareja de alfiles.
- Mate, ahogado, triple repetición, regla de las 50 jugadas y material insuficiente.

`Bench` busca 8 posiciones fijas a una profundidad fija y da nodos y tiempo; sirve para
medir cada cambio:

```bash
java -cp engine/target/classes io.github.davidpomaresp.alfil.Bench 4
```
