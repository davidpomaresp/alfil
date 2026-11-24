# Alfil

Motor de ajedrez escrito en Java desde cero.

> Demo para jugar en el navegador: próximamente en davidpomaresp.github.io/ajedrez

## Estado

- [x] Tablero de 64 casillas y lectura/escritura de FEN
- [ ] Generación de jugadas legales con pruebas perft
- [ ] Búsqueda negamax con poda alfa-beta y evaluación
- [ ] Compilación a JavaScript con TeaVM y tablero web
- [ ] Primer experimento medido (nodos, tiempo y partidas)

## Cómo ejecutar las pruebas

Necesitas Java 21 y Maven.

```bash
mvn test
```

## Representación del tablero

Las casillas van de 0 a 63, empezando en a1 y avanzando por filas
(a1 = 0, h1 = 7, a8 = 56, h8 = 63). Cada casilla guarda un número
pequeño: los 3 bits bajos indican el tipo de pieza y el cuarto bit el color.
