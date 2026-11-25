# Experimento 5: poda de futilidad inversa y SEE

**Fecha:** 25/11/2026 · **Base:** commit "Use 50 openings in matches"

A partir de aquí cada cambio se mide por separado contra la misma base, con
**50 aperturas × 2 colores = 100 partidas** a 100 ms por jugada:

```bash
mvn -q -pl engine package -DskipTests   # en la base y en el cambio, guardando cada jar
java -cp engine/target/alfil.jar io.github.davidpomaresp.alfil.VersionMatch cambio.jar base.jar 100 3
java -cp engine/target/classes io.github.davidpomaresp.alfil.Bench 8
```

## A. Poda de futilidad inversa (descartada)

Cerca de las hojas (profundidad ≤ 6), si la evaluación estática ya supera beta en
80 centipeones por cada nivel que queda, se corta sin buscar.

| | Base | Cambio |
|---|---:|---:|
| Nodos, Bench 8 | 2.095.575 | 1.443.435 (−31 %) |
| Partidas | | +31 =37 −32, 49,5 %, **−3 ± 54 Elo** |

Mira menos nodos, pero en partidas no se nota ninguna mejora. **No la incorporo.**
Puede que con otro margen (80 por nivel es una suposición) funcionara; queda pendiente.

## B. SEE en la búsqueda de quietud (incorporada, mejora probable)

*Static exchange evaluation*: antes de probar una captura en la quietud, se calcula
el saldo de material del intercambio completo en esa casilla (cada bando
recaptura con su pieza más barata y puede parar cuando quiera). Si sale negativo
(por ejemplo, dama come peón defendido), la captura no se prueba. 7 pruebas en `SeeTest`,
incluida una de rayos X (una torre detrás de la dama que recaptura).

| | Base | Cambio |
|---|---:|---:|
| Nodos, Bench 8 | 2.095.575 | 1.210.941 (−42 %) |
| Partidas (4 tandas de 100) | | +137 =145 −118, 52,4 %, **+17 ± 27 Elo** |

Tandas sueltas: 51,5 %, 54 %, 46 % y 58 %. Con 100 partidas una tanda puede salir
negativa por puro azar, como se ve.

**Conclusión:** el intervalo (−11 a +44) todavía incluye el cero, así que no está
demostrado al 95 %. La probabilidad de que sea mejor ronda el 88 %. La incorporo porque
es una técnica estándar, reduce los nodos un 42 % y todas las pruebas apuntan en la
misma dirección, pero la anoto como **mejora probable, no demostrada**.

## Lo que enseña este experimento

Las mejoras grandes (ordenar jugadas, tabla de transposición) se ven con 50 partidas.
Las de +10 o +20 Elo necesitan **miles**: el margen baja con la raíz del número de
partidas (100 partidas ±55, 400 partidas ±27, 1.600 partidas ±14). Los motores serios
usan un test estadístico secuencial (SPRT) que juega hasta que el resultado es
concluyente. Es el siguiente paso natural para el banco de pruebas.
