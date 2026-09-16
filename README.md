# LegacyFeel

Experimento isolado do NuvenClub para comparar Minecraft 1.8.9 e 26.2 no mesmo servidor Paper. O projeto usa os padrões de pacote e organização do NuvenClub, sem alterar seu repositório.

## Teste rápido

Abra o PowerShell em `testserver/quick-lab` e execute:

```powershell
./start.ps1
```

O script baixa versões fixadas com verificação de checksum, testa e compila o plugin e o mod, e abre o Paper em `127.0.0.1:25575`.

- Cliente 1.8.9: conecte diretamente, sem mod.
- Cliente 26.2: instale Fabric Loader, Fabric API e `mod/build/libs/legacyfeel-0.1.0.jar`.
- Dentro do jogo: use `/lfkit` para receber a espada de comparação.
- No cliente 26.2: pressione F8 para alternar a câmera de agachamento imediata.
- Como operador: use `/legacyfeel stats` ou `/legacyfeel info <jogador>`.

O plugin aplica velocidade de ataque sem recarga, desativa sweep, permite troca de armadura e oferece bloqueio com espada usando a redução histórica `(dano + 1) / 2`. O perfil do mod modifica somente câmera e renderização da mão. O espelho visual de escudo ao agachar continua desligado até o teste dirigido com clientes reais e Grim.

Após a primeira execução, `config/legacyfeel.json` permite definir `equipAnimation` como `legacy`, `instant` ou `ticks`. No último modo, `equipAnimationTicks` aceita de 1 a 20. F8 liga ou desliga o preset sem apagar esses valores.

Veja [as instruções do laboratório](testserver/quick-lab/README.md), [o protocolo](docs/PROTOCOL.md) e [o estado atual](docs/HANDOFF.md).

## Artefatos

- Mod Fabric: `mod/build/libs/legacyfeel-0.1.0.jar`
- Plugin Paper: `plugin/build/libs/legacyfeel-server-0.1.0.jar`
- Cópia instalada: `testserver/quick-lab/plugins/LegacyFeel-Server.jar`

Licença: MIT. Fontes de Minecraft e referências em `reference/` servem somente para consulta e não entram no Git nem nos artefatos.
