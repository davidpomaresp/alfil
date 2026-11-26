# Experimento 6: ventanas de aspiración (primer SPRT)

**Fecha:** 26/11/2026 · **Base:** commit "Add SPRT to version matches"

## Qué cambia

Con profundización iterativa, la puntuación de una profundidad suele parecerse
mucho a la de la anterior. En vez de buscar la raíz con la ventana completa
(−∞, +∞), desde profundidad 4 se busca con una ventana estrecha alrededor de la
última puntuación (±25 centipeones). Si la puntuación real cae fuera, se amplía
la ventana (el doble cada vez) y se repite. Una ventana estrecha produce más cortes.

## Método: SPRT

A partir de aquí las comparaciones usan un test secuencial (SPRT): se juegan
tandas de 100 partidas (50 aperturas × 2 colores, 100 ms por jugada) hasta que la
razón de verosimilitud (LLR) cruza uno de los dos límites:

- H0: el cambio vale 0 Elo. H1: el cambio vale 15 Elo. α = β = 0,05, límites ±2,94.

```bash
java -cp engine/target/alfil.jar io.github.davidpomaresp.alfil.VersionMatch \
  cambio.jar base.jar 100 4 --sprt 0 15 1000
```

## Resultado

| Partidas | Resultado | LLR |
|---:|---|---:|
| 100 | +45 =32 −23 | 1,36 |
| 200 | +79 =72 −49 | 1,79 |
| 300 | +119 =107 −74 | 2,68 |
| 400 | +160 =144 −96 | **3,89** → H1 aceptada |

Total: 58,0 %, **+56 ± 27 Elo**. A igual profundidad los nodos casi no cambian
(Bench 8: 1.287.538 frente a 1.210.941), así que la ganancia en partidas viene
sobre todo de terminar antes cada iteración con el tiempo limitado.

## Conclusión

Mejora demostrada con el criterio del SPRT. Se incorpora.
