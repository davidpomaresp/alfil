# Experimento 1: probar las capturas primero

**Fecha:** 24/11/2026 · **Versión base:** `v0.1.0` · **Cambio:** commit "Search captures first"

## Qué cambia

En la versión base la búsqueda principal prueba las jugadas en el orden en que
salen del generador. El cambio ordena las capturas primero, de la víctima más
valiosa con el atacante menos valioso (MVV-LVA) a la menos valiosa, y después
las jugadas tranquilas. Es lo único que cambia.

Para comparar dentro del mismo código, el orden se puede desactivar con
`Search.setMoveOrdering(false)`. He comprobado que la versión `v0.1.0` y la
versión nueva con el orden desactivado dan exactamente los mismos nodos en el
banco de pruebas (6.705.624 a profundidad 4).

## Hipótesis

Alfa-beta da siempre el mismo resultado sea cual sea el orden, pero poda más
cuanto antes encuentra la mejor jugada. Las capturas suelen ser las mejores,
así que esperaba **las mismas jugadas y puntuaciones con menos nodos**, y por
tanto más profundidad en el mismo tiempo.

## Medida 1: nodos y tiempo a igual profundidad

```bash
mvn -q install -DskipTests
java -cp engine/target/classes io.github.davidpomaresp.alfil.Bench 4 --no-ordering
java -cp engine/target/classes io.github.davidpomaresp.alfil.Bench 4
```

| Posición | Jugada | Nodos sin orden | Nodos con orden | Reducción |
|---|---|---:|---:|---:|
| Inicial | b1c3 | 8.735 | 8.541 | −2 % |
| Kiwipete | e2a6 | 3.164.995 | 48.268 | −98 % |
| Posición 3 | b4f4 | 5.398 | 2.885 | −47 % |
| Posición 6 | c3d5 | 2.729.660 | 683.037 | −75 % |
| Gambito de dama | b1d2 | 72.128 | 31.294 | −57 % |
| Italiana | c1e3 | 330.115 | 182.008 | −45 % |
| Medio juego | f3e5 | 393.213 | 133.373 | −66 % |
| Final de peones | f2f4 | 1.380 | 1.237 | −10 % |
| **Total** | | **6.705.624** | **1.090.643** | **−84 %** |
| Tiempo total | | 13.183 ms | 2.122 ms | −84 % |

En las 8 posiciones la jugada y la puntuación son idénticas, como predice la teoría.
La velocidad (~510.000 nodos/s) apenas cambia: ordenar cuesta poco y el ahorro sale
de mirar menos posiciones. Donde más gana es en las posiciones con muchas capturas
(Kiwipete), y donde menos, en la inicial y en el final de peones, que casi no tienen.

## Medida 2: partidas

```bash
java -cp engine/target/classes io.github.davidpomaresp.alfil.Match 100 3
```

- 25 aperturas cortas (en `Match.OPENINGS`), cada una jugada dos veces con los colores cambiados: 50 partidas.
- Mismo control para los dos: 100 ms por jugada, sin libro de aperturas.
- 3 partidas a la vez en una máquina de 4 núcleos (las dos versiones sufren igual la carga).

| Resultado (con orden) | Puntos | Elo estimado |
|---|---|---|
| +33 =5 −12 | 71,0 % | **+156 ± 104** (95 %) |

## Conclusión

- **Eficiencia: resultado claro.** −84 % de nodos a igual profundidad, sin cambiar las
  jugadas. Se puede repetir exactamente: los nodos no dependen de la máquina.
- **Fuerza: mejora, pero la cifra es imprecisa.** Con 50 partidas el margen es enorme:
  el intervalo va de unos +50 a unos +260 Elo. Lo que sí se puede afirmar es que la
  versión con orden es más fuerte. Para dar una cifra fiable harían falta cientos de
  partidas (el margen baja con la raíz del número de partidas: 4 veces más partidas
  para la mitad de margen).
- El tiempo depende de la máquina y de la carga; los nodos no. Por eso se dan los dos.
