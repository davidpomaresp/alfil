# Alfil en Lichess

Alfil habla UCI, así que puede jugar en Lichess con
[lichess-bot](https://github.com/lichess-bot-devs/lichess-bot), el puente oficial
(en Python) entre la API de bots de Lichess y cualquier motor UCI.

## 1. Cuenta de bot

1. Crea una cuenta **nueva** en Lichess (una cuenta de bot no puede haber jugado partidas).
2. Con esa cuenta, crea un token con el permiso `bot:play` en
   <https://lichess.org/account/oauth/token/create?scopes[]=bot:play>. No lo subas nunca al repositorio.
3. Convierte la cuenta en bot (no se puede deshacer):

   ```bash
   curl -d '' https://lichess.org/api/bot/account/upgrade -H "Authorization: Bearer TU_TOKEN"
   ```

## 2. Preparar lichess-bot

Necesitas Java 21 y Python 3.

```bash
mvn -q -pl engine package -DskipTests          # genera engine/target/alfil.jar

git clone https://github.com/lichess-bot-devs/lichess-bot.git
cd lichess-bot
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt

mkdir -p engines
cp ../alfil/engine/target/alfil.jar engines/
printf '#!/bin/sh\nexec java -jar "$(dirname "$0")/alfil.jar"\n' > engines/alfil
chmod +x engines/alfil
cp config.yml.default config.yml
```

## 3. Configuración

En `config.yml` cambia solo estas claves (el resto puede quedarse como viene en
`config.yml.default`; si algún nombre no coincide, manda el de ese archivo):

```yaml
token: ""                  # vacío: se lee de la variable LICHESS_BOT_TOKEN
engine:
  dir: "./engines/"
  name: "alfil"
  protocol: "uci"
  ponder: false
  uci_options:
    Hash: 64
challenge:
  concurrency: 1           # una partida a la vez: el motor usa un solo hilo
  accept_bot: true
  variants:
    - standard
  time_controls:
    - bullet
    - blitz
    - rapid
  modes:
    - casual
    - rated
```

## 4. Arrancar

```bash
export LICHESS_BOT_TOKEN=TU_TOKEN
python3 lichess-bot.py
```

El bot juega mientras el programa esté abierto. Para que juegue siempre hace falta
una máquina encendida todo el tiempo (por ejemplo una máquina virtual gratuita).

## Registro de Elo por versión

| Versión | Fecha | Partidas | Elo bullet | Elo blitz | Elo rapid |
|---|---|---|---|---|---|
| | | | | | |
