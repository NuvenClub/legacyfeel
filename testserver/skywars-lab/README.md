# SkyWars Laboratório local

Servidor Paper 26.2 isolado na porta `25576`. O pacote obrigatório é servido
somente em `127.0.0.1:25578` enquanto `start.ps1` estiver aberto.

O servidor carrega duas camadas independentes:

- `LegacyFeel-Server`: PvP legado, handshake e telemetria;
- `SkyWarsLab`: pacote obrigatório, kits e cosméticos modernos.

O build também instala `OldCombatMechanics` e `PacketEvents`, usando as versões
já validadas no quick lab. Nenhum arquivo do NuvenClub de produção é alterado.

## Preparar sem iniciar

```powershell
.\build.ps1
```

## Iniciar para teste manual

```powershell
.\start.ps1
```

O fluxo dentro do jogo é:

1. aceitar e aguardar o pacote;
2. conferir `/swlab status`;
3. usar `/swlab enter`;
4. alternar com `/swlab kit dominio|reverso|mirage`;
5. escolher `/swlab cage prisma|cryo|confetti|sanctuary`;
6. escolher `/swlab projectile flames|ninja|mystic|cloud`;
7. usar `/swlab effects full|reduced` para comparar carga visual.

Este é o primeiro ambiente jogável das habilidades. Ainda não há fila, baús,
espectador, vitória ou reset automático de arena; esses elementos entram após
validarmos os três kits e o pacote em cliente real.

O servidor clássico em `25575` não é alterado nem interrompido por este
laboratório.
