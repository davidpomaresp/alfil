# Experimento 3: Elo estimado contra Stockfish limitado

**Fecha:** 25/11/2026 · **Versión:** commit "Explain how to run the bot on Lichess"

## Método

Stockfish 16 (paquete de Ubuntu) con `UCI_LimitStrength=true` y `UCI_Elo=N` juega
a propósito con una fuerza aproximada de N. `UciMatch` enfrenta a los dos programas
por UCI con reloj real:

```bash
mvn -q -pl engine package -DskipTests
java -cp engine/target/alfil.jar io.github.davidpomaresp.alfil.UciMatch \
  "java -jar engine/target/alfil.jar" "/usr/games/stockfish" 20000 200 3 \
  -b UCI_LimitStrength=true -b UCI_Elo=2000
```

- 25 aperturas × 2 colores = 50 partidas por nivel.
- 20 s + 0,2 s por jugada para cada uno. 3 partidas a la vez en 4 núcleos.
- Alfil con 16 MB de tabla de transposición y un hilo. Stockfish con su configuración por defecto (un hilo).

## Resultados

| Stockfish limitado a | Resultado de Alfil | Puntos | Diferencia | Estimación de Alfil |
|---|---|---|---|---|
| 2000 | +38 =3 −9 | 79 % | +230 ± 122 | ~2230 |
| 2400 | +23 =2 −25 | 48 % | −14 ± 97 | ~2390 |

## Conclusión

- Las dos medidas no coinciden del todo (2230 frente a 2390), pero sus márgenes se
  solapan. Una estimación razonable es **unos 2300 ± 100 en la escala de Stockfish limitado**.
- Ese Elo está calibrado contra listas de motores con otro control de tiempo. Sirve
  para comparar versiones de Alfil entre sí con una referencia externa, **no** es el
  Elo que tendría contra personas en Lichess.
- Para afinarlo harían falta más partidas y más niveles.
