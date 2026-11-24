# Experimento 2: búsqueda y evaluación más fuertes

**Fecha:** 24/11/2026 · **Antes:** commit "Write up the move ordering experiment" · **Después:** commit "Evaluate pawn structure, mobility, open files and king shelter"

## Qué cambia

Aquí **no** cambia una sola cosa: es un paquete de técnicas conocidas, para que la
primera versión pública juegue bien. Por eso este experimento solo dice cuánto
mejora el conjunto, no cuánto aporta cada parte.

Búsqueda:
- Tabla de transposición (hash Zobrist, 2^20 entradas, 16 MB) y su jugada como primera candidata.
- Orden: jugada de la tabla, capturas MVV-LVA, dos *killer moves* por nivel, historial.
- Búsqueda de variante principal (PVS), poda de jugada nula, reducciones de jugadas tardías (LMR) y extensión de jaque.
- Generación pseudolegal en la búsqueda (la legalidad se comprueba al hacer la jugada) y generador solo de capturas para la quietud.
- No empezar una iteración nueva si ya se ha gastado la mitad del tiempo.

Evaluación (todo con valor de medio juego y de final):
- Peones pasados según la fila, doblados y aislados.
- Movilidad de caballos, alfiles, torres y damas.
- Torres en columna abierta o semiabierta.
- Peones delante del rey enrocado.

## Medida 1: nodos a igual profundidad

```bash
java -cp engine/target/classes io.github.davidpomaresp.alfil.Bench 6
```

| Versión | Nodos (profundidad 6, 8 posiciones) | Tiempo |
|---|---:|---:|
| Antes | 48.541.269 | 84,9 s |
| Después | 421.949 | 0,7 s |

Unas **115 veces menos nodos**. Aquí las jugadas elegidas sí pueden cambiar: la
evaluación es distinta, y la poda de jugada nula y las reducciones dejan de ser
exactas (aceptan un pequeño riesgo de error a cambio de mucha profundidad).
La velocidad baja algo por nodo (la evaluación hace más cosas), de ~1,2 a ~0,7
millones de nodos/s, pero se llega mucho más hondo: profundidad 8 en las 8
posiciones en 3 s.

## Medida 2: partidas

Cada versión se carga desde su propio `.jar` y juegan entre sí con las mismas
25 aperturas, colores alternados y 100 ms por jugada:

```bash
mvn -q -pl engine package -DskipTests      # en cada versión, y guarda el jar
java -cp engine/target/classes io.github.davidpomaresp.alfil.VersionMatch nuevo.jar viejo.jar 100 3
```

| Resultado (versión nueva) | Puntos | Elo estimado |
|---|---|---|
| +47 =1 −2 | 95,0 % | +512 ± 416 (95 %) |

## Conclusión

- La mejora es enorme y segura: 47 victorias de 50.
- La cifra de Elo **no es fiable**: cuando un lado gana casi todo, la fórmula se
  dispara y el margen (±416) es casi tan grande como el valor. Lo honesto es decir
  "varios cientos de Elo más fuerte".
- Esto mide fuerza **relativa** entre versiones. El Elo absoluto (contra personas y
  otros motores) solo lo dará jugar en Lichess.
