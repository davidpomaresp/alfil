# Experimento 4: Texel tuning (resultado negativo)

**Fecha:** 25/11/2026 · **Versión:** commit "Add self-play data generation and Texel tuner"

## Idea

En vez de inventarme los 75 números de la evaluación (valor de las piezas, bonus por
casilla, peones pasados, movilidad...), que el motor los ajuste a partir de partidas.
Una puntuación `s` en centipeones se convierte en resultado esperado con
`1 / (1 + 10^(-K·s/400))`, y se buscan los pesos que minimizan el error cuadrático
medio frente al resultado real de cada posición (1, ½ o 0).

## Método

```bash
mvn -q -pl engine package -DskipTests
# 3.000 partidas de Alfil contra sí mismo, profundidad 5, 8 jugadas aleatorias al principio
java -cp engine/target/alfil.jar io.github.davidpomaresp.alfil.SelfPlay selfplay.txt 3000 5 1 7
# ajuste por coordenadas: cada peso ±4 hasta que nada mejora, luego ±1
java -Xmx6g -cp engine/target/alfil.jar io.github.davidpomaresp.alfil.Tuner selfplay.txt 15
```

- Datos: 257.122 posiciones tranquilas (sin jaque y sin capturas que cambien la evaluación),
  etiquetadas con el resultado final de su partida.
- K ajustado: 0,90. Error inicial 0,095588, final 0,093471 (−2,2 %).

## Resultado en partidas

Pesos ajustados contra pesos originales, mismo código, 50 partidas por control:

| Control | Resultado (ajustados) | Puntos | Elo |
|---|---|---|---|
| 100 ms por jugada | +17 =7 −26 | 41 % | −63 ± 93 |
| 300 ms por jugada | +11 =17 −22 | 39 % | −78 ± 81 |

## Conclusión

**Los pesos ajustados juegan peor** (unos −70 Elo en 100 partidas; los dos controles
apuntan en la misma dirección). No los he incorporado: el motor sigue con los pesos a mano.
El ajustador sí queda en el repositorio.

Por qué creo que ha fallado:
- **Datos pobres.** Partidas a profundidad 5 de un motor de ~2300 tienen muchos errores,
  y el resultado de la partida etiqueta posiciones que se decidieron mucho después.
- **Se ajusta al ruido.** Algunos valores no tienen sentido de ajedrez (rey en el centro
  penalizado en el final, torre en columna abierta negativa en el final, peones doblados
  positivos en medio juego). Señal de sobreajuste a las particularidades de estos datos.
- **Error más bajo no es igual a jugar mejor.** El ajustador predice mejor el resultado
  de *estas* partidas, que no es lo mismo que elegir mejores jugadas.

Qué probaría después: muchas más partidas (decenas de miles) a más profundidad,
separar datos de entrenamiento y de validación para detectar el sobreajuste, y ajustar
primero pocos parámetros (material y peones pasados) antes que todos a la vez.
