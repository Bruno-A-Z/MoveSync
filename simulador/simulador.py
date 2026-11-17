"""
Simulador de sensores (substitui o ESP32 na demo).
Publica leituras via MQTT no RabbitMQ.

    pip install paho-mqtt
    python simulador/simulador.py                 # cruzamento 1, vias 1,2,3, localhost
    python simulador/simulador.py --emergencia    # injeta um veículo de emergência na via 2 de vez em quando
    python simulador/simulador.py --parar-via 3   # simula o sensor da via 3 fora do ar (testa o fallback)

Tópico: sensores/{cruzamentoId}/{viaId}   Payload: {"viaId":1,"quantidadeVeiculos":12,"tamanhoFilaMetros":40,"veiculoEmergencia":false}
"""
import argparse
import json
import random
import time

import paho.mqtt.client as mqtt


def criar_cliente(host, porta, usuario, senha):
    try:
        cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)  # paho-mqtt 2.x
    except AttributeError:
        cliente = mqtt.Client()  # paho-mqtt 1.x
    cliente.username_pw_set(usuario, senha)
    cliente.connect(host, porta, keepalive=60)
    cliente.loop_start()
    return cliente


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="localhost")
    ap.add_argument("--porta", type=int, default=1883)
    ap.add_argument("--usuario", default="guest")
    ap.add_argument("--senha", default="guest")
    ap.add_argument("--cruzamento", type=int, default=1)
    ap.add_argument("--vias", type=int, nargs="+", default=[1, 2, 3])
    ap.add_argument("--intervalo", type=float, default=2.0, help="segundos entre rodadas de leituras")
    ap.add_argument("--emergencia", action="store_true")
    ap.add_argument("--parar-via", type=int, default=None, help="não publica leituras desta via")
    args = ap.parse_args()

    cliente = criar_cliente(args.host, args.porta, args.usuario, args.senha)
    print(f"Publicando em sensores/{args.cruzamento}/<via> -> {args.host}:{args.porta} (Ctrl+C para parar)")

    rodada = 0
    try:
        while True:
            # a cada 20 rodadas, outra via fica congestionada
            congestionada = args.vias[(rodada // 20) % len(args.vias)]
            for via in args.vias:
                if via == args.parar_via:
                    continue
                if via == congestionada:
                    veiculos, fila = random.randint(18, 35), random.randint(80, 180)
                else:
                    veiculos, fila = random.randint(0, 8), random.randint(0, 30)
                emergencia = args.emergencia and via == 2 and rodada % 45 == 44
                payload = {
                    "viaId": via,
                    "quantidadeVeiculos": veiculos,
                    "tamanhoFilaMetros": fila,
                    "veiculoEmergencia": emergencia,
                }
                cliente.publish(f"sensores/{args.cruzamento}/{via}", json.dumps(payload))
                print(f"  via {via}: {payload}")
            rodada += 1
            time.sleep(args.intervalo)
    except KeyboardInterrupt:
        pass
    finally:
        cliente.loop_stop()
        cliente.disconnect()


if __name__ == "__main__":
    main()
