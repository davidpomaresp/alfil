# Alfil

Motor de ajedrez escrito en Java desde cero.

> Demo para jugar en el navegador: próximamente en davidpomaresp.github.io/ajedrez

## Estado

- [x] Tablero de 64 casillas y lectura/escritura de FEN
- [x] Generación de jugadas legales con pruebas perft
- [ ] Búsqueda negamax con poda alfa-beta y evaluación
- [ ] Compilación a JavaScript con TeaVM y tablero web
- [ ] Primer experimento medido (nodos, tiempo y partidas)

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

## Generación de jugadas

Primero se generan las jugadas pseudolegales (cada pieza según sus reglas) y
después se descartan las que dejan al propio rey en jaque: se hace la jugada,
se mira si el rey está atacado y se deshace. Es más lento que generar solo
jugadas legales, pero mucho más difícil de equivocarse.

La generación se comprueba con perft contra los valores publicados de seis
posiciones de referencia y catorce posiciones que prueban una regla concreta
(al paso ilegal por clavada, enroque con jaque, promociones, ahogado...).
Desde la posición inicial, perft(6) = 119.060.324 nodos en unos 9 segundos.
