# Quick lab

Servidor Paper 26.2 direto em `127.0.0.1:25575`, isolado do NuvenClub e voltado à comparação rápida. Ele aceita cliente 26.2 e, por ViaVersion/ViaBackwards/ViaRewind, cliente 1.8.9. Offline mode existe somente para o loopback local. O OldCombatMechanics 2.6.0 fornece o pipeline clássico de knockback, vara, ferramentas, críticos, regeneração, armadura e bloqueio; o arquivo `oldcombatmechanics-config.yml` fixa a resistência em 20 ticks, alcance máximo em 3 blocos e margem de hitbox em 0,1.

No PowerShell:

```powershell
./install.ps1
./start.ps1
```

`start.ps1` testa e compila plugin/mod, copia o plugin e abre o console do Paper. Coloque `../../mod/build/libs/legacyfeel-0.1.0.jar` na instância Fabric 26.2. O cliente 1.8.9 entra sem mod. Use `/lfkit`, `/legacyfeel info` e F8 no cliente modificado para alternar o preset legado/vanilla.

Comparação inicial: ficar parado no mesmo bloco, gravar agachar/levantar 10 vezes; repetir com F8. Depois testar ataques contínuos, blockhit com a espada, machado e defesa do escudo ao agachar. O clique direito do escudo não deve levantá-lo.

Este laboratório omite Velocity deliberadamente. A topologia completa será usada para validar troca de backend e forwarding depois da comparação visual direta.
