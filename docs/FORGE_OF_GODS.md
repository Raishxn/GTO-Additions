# Forge of Gods (port do GTNA)

Port 1:1 do sistema Forge of Gods do GregTech Nexus Addon (GTNA 0.5.2, por sua vez portado do GTNH
GT5-Unofficial `a3e1e112`) para o GTO 0.6.0-dev9 (GTOCore 26.9.5, fork GTCEu 26.9.70).

## O que foi portado

- Regras puras (`godforge/api`): `GodforgeData`, `GodforgeMath`, `GodforgeUpgrade` (31 upgrades),
  `GodforgeUpgradeStorage`, `GodforgeOverclock`, `GodforgeStarColor`, `GodforgePlasmaTable`. Teste de paridade
  numérica com o GTNH (`GodforgeMathTest`).
- Controlador `gtoa:forge_of_gods` com as estruturas `.mbs` do GTNA (principal, ativa, anéis 2/3 e versões "air"),
  bateria/combustível, anéis absorvidos ao acender a estrela, milestones, inversão, ejeção de shards,
  `/gtoa godforge shards <n>`.
- Módulos `godforge_smelting/molten/plasma/exotic_module`: energia da rede wireless GTMThings do dono (a mesma
  que o Eye of Harmony do GTO usa), calor/paralelos/velocidade/voltagem empurrados pelo forge a cada 5 s, OC GT5.
- GUI completa (painel, combustível, árvore de upgrades, milestones, cosméticos da estrela), render da estrela/feixe
  com os shaders do GTNA, som em loop, tooltips e idiomas EN/PT.
- Tipo de receita `gtoa:godforge_plasma`; o Molten usa Alloy Blast Smelter + Alloy Smelter do GTO. Receitas da
  progressão via `GTRecipeBuilder` base (o `RecipeBuilder` nativo do GTO descarta receitas de addons).

## Adaptações ao GTO

| GTNA | GTO-Additions |
| --- | --- |
| Casings/anéis/lente/bobina Hypogen próprios | Blocos que o GTO já registra (mesmas texturas); só o Harmonic Phonon Transmission Conduit é novo |
| Graviton Shard (material gem) | Item `gtoa:graviton_shard` |
| Thread Hatch do GTNA | Thread Hatch do GTO; acima do tier liberado (GPCI/CD/END) o módulo **não processa** (no GTNA forma e só não adiciona threads) |
| Nexus Terminal constrói anéis 2/3 | Construção manual dos anéis (sem sub-padrão no terminal do GTO) |
| Wireless GTNA | `WirelessEnergyContainer` do GTMThings (time do dono) |
| Tier de plasma / multi-step | Chaves de dados `tier` e `special` do GTO |

## Substituições de materiais/itens (receitas)

Mesmo tier do GTNH; quantidades, durações e EU/t do GTNA mantidos.

| GTNH/GTNA | GTO |
| --- | --- |
| Creon | Cosmic Neutronium |
| Mellion | Shirabon |
| Six-Phased Copper | Awakened Draconium |
| Supercondutor UIV | Taranium (cabo UXV do GTO) |
| Supercondutor UMV | Crystal Matrix (cabo OpV do GTO) |
| Metastable Oganesson (sólido) | Heavy Quark Degenerate Matter |
| Rhugnor (engrenagens) | Celestial Tungsten |
| Ichorium | Starmetal |
| Dragon Metal | Draconium |
| Universium | Chaos |
| Quantum | Quantum Metal |
| Stellar Alloy | Infinity |
| Phonon Medium | Dimensionally Shifted Superfluid |
| Orundum (lente) | Diamante |
| Baterias ZPM4 / ZPM6 | Extremely / Insanely Max Battery |
| Ultimate Battery (casing) | Transcendent Max Battery |
| Fusion Mk4/Mk5 (Advanced Fusion) | Kuangbiao One Giant Fusion Reactor |
| Transcendent Plasma Mixer | Hyperdimensional Plasma Fusion Core |
| Plasma Forge Bridge Casing | Dimensional Bridge Casing |
| Cosmic Fabric Manipulator | Cosmic Fabric |
| Field Restriction Coil T3 | Advanced Fusion Coil |
| Special Laser Lens | Non-Linear Optical Lens |
| Nanites do GTNA | Nanites do GTO (sem a do material → Cosmic Neutronium) |
| Superdense plate | Dense plate ×7 (máx. 64) |
| Boundary/Spatial casing, Stabilisation fields, Astral Array Fabricator (EOH) | Dimensional Stability Casing, Dimension Injection Casing, Spacetime Compression Field Generator, 1 Eye of Harmony |
| Pesquisa 48M CWU | Peças 8192 CWU/t·256M; módulos 16384·1G; Forja 32768·2G (OpV) |

Plasmas da tabela do GTNH cujo material não tem plasma no GTO são ignorados.

## Validação

Build + 58 testes (incluindo paridade numérica e o hook de receitas aplicado ao `Data` real do GTOCore 26.9.5).
Jogo dev9 iniciado: registro, receitas (`Data loading` sem erros do addon) e mundo carregam.
**Ainda não testado em jogo:** formação da estrutura, GUI, render da estrela, módulos processando, persistência.
