# GTO-Additions

Addon para **GregTech Odyssey 0.6.0-dev9**, com máquinas, barramentos, hatches e
covers adicionais. Mod ID: `gtoa`. Versão: **0.1.0-dev9**. Licença: **LGPL-3.0-only**.

## Compatibilidade

Esta versão foi desenvolvida para a instalação original do **GTO 0.6.0-dev9**.

| Componente | Versão alvo |
| --- | --- |
| Minecraft | 1.20.1 |
| Mod loader | Forge 47.4.20 ou posterior da série 47 |
| Java | 21 ou superior para executar; JDK 21 para compilar |
| GTOCore / GTOLib | 26.9.5 |
| GTCEu, fork do GTO | 26.9.70 |
| Applied Energistics 2, incluído no GTO | 15.269.3 |
| LDLib | 1.0.52.a |
| DataSyncLib | 26.9.4 |

GTOCore e GTCEu têm versões fixadas nos metadados do addon. Outras versões do GTO,
GTCEu padrão, Fabric e NeoForge não têm suporte confirmado. As integrações usam
APIs e pontos de registro específicos do dev9; mudar o modpack exige revisar o port.
Instale a mesma versão do addon no **cliente e no servidor**.

## Recursos

- **Universal Factory:** port do GTNA, com textura e receitas originais, 41 tipos
  de receita, threads, paralelismo e aquecimento. Modos Legacy, Shared Budget e Unlimited.
- **Primitive Stone Furnace:** multiblock de pedra com processamento sem EU ou combustível.
- **Buses e hatches estendidos:** mais slots/capacidade, com produção configurável nas saídas.
- **Covers de produção:** multiplicam saídas, reduzem duração e consumo de energia.
- **Entangled Miner e Oil Drill:** singleblocks Steam–EV com cartões de depósito/campo.
- **Magic Generators ULV–MAX:** geração baseada em um End Crystal acima da máquina.
- **Termostato da Primitive Distillation Tower:** controle automático de temperatura.

Consulte [recursos, estruturas, receitas e configurações](docs/FEATURES.md).
Textos da interface disponíveis em português do Brasil e inglês.

## Instalação

1. Use uma instância do **GregTech Odyssey 0.6.0-dev9** e feche o jogo/servidor.
2. Baixe o jar do addon em [Releases](https://github.com/Raishxn/GTO-Additions/releases).
3. Coloque `gto-additions-0.1.0-dev9.jar` na pasta `mods` e remova versões anteriores do addon.
4. Inicie o jogo. As dependências vêm da instalação do modpack.

Para instalar uma compilação local com backup e atualização dos caches:

```sh
python3 scripts/install_dev9.py "/caminho/da/instancia/minecraft"
```

Se uma atualização conservar modelos, traduções ou receitas antigos, consulte o
[guia de compatibilidade e atualização](docs/COMPATIBILITY.md).

## Compilar a partir do código

Use **JDK 21**, Python 3 e uma instalação original do dev9 para preparar as APIs:

```sh
python3 scripts/prepare_dev9.py "/caminho/da/instancia/minecraft"
./gradlew build
```

No Windows, use `gradlew.bat build`. O resultado fica em
`build/libs/gto-additions-0.1.0-dev9.jar`. A primeira compilação requer acesso à
internet para as ferramentas de compilação. Os jars de dependências em `libs/`
são locais, não entram no repositório nem no jar publicado.

## Estado de validação

Compilação e **55 testes automatizados** aprovados. O funcionamento da Universal
Factory foi confirmado em jogo no dev9 após corrigir a retomada de receitas e o
estado ocioso. Outros recursos e cenários de persistência/integração ainda exigem
validação adicional; esta é uma versão inicial para o modpack alvo.

Veja o [histórico de alterações](CHANGELOG.md). Para relatar um problema, informe
versões, receita, estrutura e anexos relevantes de `logs/latest.log` ou crash report.

## Créditos e licença

Desenvolvido por **Raishxn**. Código sob [LGPL-3.0-only](LICENSE), acompanhado do
texto da [GPL v3](COPYING). Ports e recursos de terceiros estão documentados em
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). As dependências do modpack
mantêm suas próprias licenças e são distribuídas pelos respectivos projetos.
