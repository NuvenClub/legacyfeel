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
- Dentro do jogo: use `/lfkit` para receber espada, machado de 6 de dano e escudo.
- No cliente 26.2: pressione F8 para alternar a câmera de agachamento imediata.
- Como operador: use `/legacyfeel stats` ou `/legacyfeel info <jogador>`.

O plugin aplica golpes sem recarga nem janela de invulnerabilidade, desativa sweep, fixa o machado em 6 de dano e oferece bloqueio com espada usando a redução histórica `(dano + 1) / 2`. No cliente moderno, a câmera agachada usa 1,54 bloco como na 1.8.9, o clique direito da espada permite blockhit e a penalidade por errar o cursor é removida. O escudo ignora clique direito e defende somente enquanto o jogador agacha.

Após a primeira execução, `config/legacyfeel.json` permite definir `equipAnimation` como `legacy`, `instant` ou `ticks`. No último modo, `equipAnimationTicks` aceita de 1 a 20. F8 liga ou desliga o preset sem apagar esses valores.

Veja [as instruções do laboratório](testserver/quick-lab/README.md), [o protocolo](docs/PROTOCOL.md) e [o estado atual](docs/HANDOFF.md).

O plano para transformar o cliente moderno em uma experiência Legacy+ e criar o SkyWars Laboratório 26.x está em [Legacy+ e SkyWars Laboratório](docs/ROADMAP-LEGACY-PLUS-LAB.md).

A primeira experiência jogável do Laboratório, com pacote obrigatório, kits
Mirage, Domínio e Reverso, jaulas e cosméticos de projétil, está detalhada em
[SkyWars Laboratório — primeira experiência jogável](docs/SKYWARS-LAB-VERTICAL-SLICE.md).

## Artefatos

- Mod Fabric: `mod/build/libs/legacyfeel-0.1.0.jar`
- Plugin Paper: `plugin/build/libs/legacyfeel-server-0.1.0.jar`
- Cópia instalada: `testserver/quick-lab/plugins/LegacyFeel-Server.jar`

Licença: MIT. Fontes de Minecraft e referências em `reference/` servem somente para consulta e não entram no Git nem nos artefatos.
