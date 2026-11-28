# Experimento 7: tres correcciones tras una revisión externa

**Fecha:** 28/11/2026

Una revisión del código encontró tres fallos reproducibles. Los corregí, cada uno con su prueba de regresión:

| Fallo | Corrección | Prueba |
|---|---|---|
| `go infinite` seguido de `stop` al instante: la búsqueda borraba la orden al arrancar y no paraba nunca | La búsqueda ya no reinicia la señal de parada (cada `go` crea una búsqueda nueva) | `UciTest.stopRightAfterGoIsNotLost` |
| En la búsqueda de quietud, tras una captura con jaque se evaluaba la posición como si nada: un mate como `Rxa8#` valía +7 peones | En jaque se buscan todas las evasiones legales, sin evaluación estática; sin evasiones es mate | `SearchTest.quiescenceSeesMateAfterACapture` |
| La triple repetición tras `1.e4` no se detectaba: el hash incluía la casilla al paso aunque nadie pudiera capturar | La casilla al paso solo se guarda si hay un peón rival al lado | `GameTest.repetitionAfterADoublePawnPush` |

## ¿Empeora el juego?

Buscar evasiones en la quietud cuesta nodos: en `Bench 8` se pasa de 1.287.538 a 1.424.158 (+11 %). Para comprobar que no perdía fuerza jugué un SPRT de no regresión (H0: −10 Elo, H1: 0 Elo):

```bash
java -cp fix.jar io.github.davidpomaresp.alfil.VersionMatch fix.jar base.jar 100 4 --sprt -10 0 1000
```

1.000 partidas: +330 =361 −309, 51,1 %, **+7 ± 17 Elo**. El SPRT no llegó a decidir (LLR 1,59), pero el intervalo va de −10 a +24: no hay señal de que juegue peor, y ahora no se equivoca en esas posiciones.
