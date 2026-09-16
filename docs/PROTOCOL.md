# PROTOCOL — versão 2

Canal `legacyfeel:handshake`. A versão atual é 2 e a mínima aceita é 1.

## Wire format

O codec `ByteBufCodecs.stringUtf8(8191)` escreve um comprimento VarInt seguido
do JSON UTF-8. O plugin usa o mesmo enquadramento. Comprimento negativo, acima
de 8191 ou maior que o corpo disponível é rejeitado. JSON inválido é ignorado.

## Negociação

O cliente v2 envia:

```json
{
  "t": "HELLO",
  "v": 2,
  "minV": 1,
  "mod": "0.1.0",
  "mc": "26.2",
  "loader": "fabric",
  "features": { "legacyCombat": true }
}
```

O servidor responde `WELCOME` usando a versão solicitada, desde que esteja no
intervalo 1–2. Para preservar compatibilidade com a implementação antiga, o
cliente tenta v2 primeiro e, se não receber resposta, espera 50 ticks e envia
um único fallback v1. A espera ultrapassa o rate limit de dois segundos do
servidor v1.

## Perfis v2

`WELCOME` e `POLICY` podem conter:

```json
{
  "t": "WELCOME",
  "v": 2,
  "serverProfile": "MODERN_LEGACY_COMBAT",
  "rulesetId": "legacyfeel-1.7-v1",
  "capabilities": ["legacyCombat", "shieldOnSneak", "classicBlockPrediction"],
  "rules": { "shieldOnSneak": true, "armorSwap": true },
  "forceOff": []
}
```

Perfis reconhecidos:

- `CLASSIC_PARITY`: backend 1.8.8 traduzido para cliente moderno;
- `MODERN_LEGACY_COMBAT`: backend moderno com regras de combate legado;
- `SKYWARS_LAB`: arena moderna experimental;
- `VANILLA_SAFE`: integração ausente ou perfil conservador.

`rulesetId` identifica de modo estável as regras usadas na partida.
`capabilities` declara recursos suportados pelo servidor. `forceOff`, quando
presente, substitui o conjunto de recursos proibidos no cliente.

## Correção de previsão de blocos

`classicBlockPrediction` só é aplicada quando:

1. o perfil recebido é `CLASSIC_PARITY`;
2. a capability foi anunciada;
3. o preset e a opção local estão ligados;
4. a capability não aparece em `forceOff`.

O cliente agrupa os ACKs sintéticos recebidos do ViaVersion e os aplica quatro
ticks depois por padrão, configurável entre 1 e 10 ticks. Atualizações reais do
bloco recebidas nesse intervalo atualizam o estado conhecido pelo mecanismo
vanilla. Se o servidor rejeitar a colocação, o estado previsto ainda é desfeito
quando o prazo termina. Isso evita transformar a correção em sustentação sobre
blocos fantasmas permanentes.

## Simulador local

Um operador do laboratório pode alternar políticas sem reiniciar:

```text
/legacyfeel policy classic
/legacyfeel policy modern
/legacyfeel policy lab
/legacyfeel policy vanilla_safe
/legacyfeel policy config
```

`config` volta ao perfil definido no arquivo. A mudança envia `POLICY` para os
clientes modificados conectados. O simulador não altera dano, mundo ou plugins;
ele serve para conferir negociação e ativação client-side.

## Ordem e estado

- codecs são registrados antes da conexão;
- a tentativa de HELLO dura no máximo cinco segundos;
- estado, capabilities e políticas são limpos ao desconectar;
- sem plugin, recursos locais seguros continuam disponíveis e recursos que
  exigem perfil de servidor permanecem desligados;
- `ClientInfo` guarda versão negociada, mod, Minecraft, loader e features;
- relatórios de combate incluem perfil, ruleset e versões do cliente;
- clientes vanilla e 1.8.9 não recebem payload sem registrar o canal.

## Testes pendentes

- troca real de backend no Velocity e reenvio do HELLO;
- latência do handshake em conexão remota;
- fuzz de payloads e política parcial;
- ponte ninja e torre vertical em 1.8.9 e 26.x, com 0/50/100/200 ms;
- colocação aceita, rejeitada por colisão, região protegida e anticheat;
- confirmação de que quatro ticks bastam para o ViaVersion usado pela rede.
