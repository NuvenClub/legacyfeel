# LegacyFeel

Projeto em reconhecimento: mod Fabric exclusivamente visual, plugin Paper responsável pelas regras e ambiente Velocity + Paper + Via + Grim. Alvo mantido em Minecraft 26.2.

**Ainda não existe mod ou plugin instalável.** Foram preparados o build para gerar fontes e os documentos da Fase 1. Nenhuma feature, servidor ou partida foi testada.

## Revisão

- [Reconhecimento e versões](docs/RECON.md)
- [Tabela prevista de mixins](docs/MIXINS.md)
- [Decisões que precisam de aprovação](docs/APPROVAL.md)
- [Estado para retomar](docs/HANDOFF.md)
- [Pedido original preservado](docs/REQUEST.md)

ID e canal propostos: `legacyfeel` e `legacyfeel:handshake`. Nome do servidor, pacote Java e licença ainda dependem do usuário. Não foi aplicada uma licença de distribuição ao projeto.

## Limites

O mod não altera movimento, altura real dos olhos, hitbox, reach, dano, cooldown ou penalidade de miss. Não inclui assets da Mojang. O espelho de escudo com supressão de pacote está bloqueado por contradição no pedido. O handshake informa capacidades declaradas, não certifica integridade do cliente.

Os clones e fontes decompilados em `reference/` são somente consulta e ficam fora do Git e do build. Não redistribuir fontes, binários ou assets do Minecraft. O template Fabric é CC0; seu aviso está em `mod/TEMPLATE-LICENSE.txt`.

## Desenvolvimento nesta máquina

No PowerShell, a partir desta pasta:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/java25/jdk-25.0.4.1+1').Path
Set-Location mod
../.tools/gradle/gradle-9.5.1/bin/gradle.bat genSources --console=plain --no-daemon
```

Java 25 e Gradle foram baixados localmente com verificação SHA-256. O wrapper também está incluído; seu download ficou parado nesta sessão e a distribuição local foi usada com sucesso. `runClient` e scripts do servidor serão preparados nas fases seguintes, após aprovação.
