# Universal Factory: levantamento para GTO dev9

Data: 2026-10-02. O levantamento abaixo fundamenta o port implementado no addon.
Receitas e textura originais do GTNA foram preservadas; veja README.md para uso e
limites da validação. Não houve instalação automática nem teste no modpack completo.

## Escopo e evidência

Alvo do addon: GTO 0.6.0-dev9, GTOCore/GTOLib 26.9.5 e GTCEu 26.9.70.
As declarações de tipos foram conferidas no jar local `libs/gtocore-26.9.5.jar`.
Os usos e condições foram investigados no checkout local `GTOCore-Main`.
O checkout difere do dev9 em alguns tipos de pesquisa; ele serve como evidência de
uso e comportamento do código local, não como uma contagem das receitas carregadas no dev9.
Não foram carregados os registros do jogo nem contadas as receitas em runtime.

Fontes principais:

- GTNA: `common/data/GTNAMachines.java`, `common/machine/multiblock/electric/UniversalFactoryMachine.java`,
  `common/machine/trait/GTNAMultipleRecipesLogic.java` e `config/GTNABalance.java`.
- GTO: `common/data/GTORecipeTypes.java`, `common/recipe/RecipeTypeModify.java`,
  `common/data/machines/` e `data/recipe/`.

## Os 32 tipos originais

Nenhuma declaração da lista original está ausente da API do dev9. A existência do
campo não prova que o tipo tenha receitas carregadas; não remover um tipo apenas
por parecer antigo. Há usos explícitos de Brewing e Fermenting no código local.

- `BENDER_RECIPES`
- `COMPRESSOR_RECIPES`
- `FORGE_HAMMER_RECIPES`
- `CUTTER_RECIPES`
- `EXTRUDER_RECIPES`
- `LATHE_RECIPES`
- `WIREMILL_RECIPES`
- `FORMING_PRESS_RECIPES`
- `POLARIZER_RECIPES`
- `LASER_ENGRAVER_RECIPES`
- `FLUID_SOLIDFICATION_RECIPES`
- `ASSEMBLER_RECIPES`
- `ARC_FURNACE_RECIPES`
- `CIRCUIT_ASSEMBLER_RECIPES`
- `CANNER_RECIPES`
- `CENTRIFUGE_RECIPES`
- `THERMAL_CENTRIFUGE_RECIPES`
- `ELECTROLYZER_RECIPES`
- `SIFTER_RECIPES`
- `MACERATOR_RECIPES`
- `EXTRACTOR_RECIPES`
- `CHEMICAL_RECIPES`
- `MIXER_RECIPES`
- `CHEMICAL_BATH_RECIPES`
- `ORE_WASHER_RECIPES`
- `LARGE_CHEMICAL_RECIPES`
- `PACKER_RECIPES`
- `DISTILLERY_RECIPES`
- `AUTOCLAVE_RECIPES`
- `FLUID_HEATER_RECIPES`
- `BREWING_RECIPES`
- `FERMENTING_RECIPES`

## Acréscimos escolhidos pelo usuário: 9 (total definido: 41)

Sete tipos próprios do GTO e dois tipos básicos ausentes da lista original.
O usuário selecionou somente os nove abaixo, mantendo os 32 originais.
Todos os campos abaixo existem no jar do dev9 e têm usos explícitos em builders/
geradores de receitas no checkout consultado; a quantidade de chamadas no código
não equivale à quantidade de receitas geradas/carregadas.

| Campo | Motivo |
| --- | --- |
| `DEHYDRATOR_RECIPES` | Desidratação; também aparece em cadeias químicas e materiais. |
| `UNPACKER_RECIPES` | Desempacotamento separado de PACKER no GTO. |
| `CLUSTER_RECIPES` | Laminação por múltiplos rolos; peças e mica. |
| `ROLLING_RECIPES` | Laminação de materiais e peças. |
| `LAMINATOR_RECIPES` | Laminação/revestimento; também aparece na cadeia de pesquisa. |
| `LOOM_RECIPES` | Tecelagem e fabricação de fios/tecidos. |
| `LASER_WELDER_RECIPES` | Soldagem de peças, tubos e componentes. |
| `ALLOY_SMELTER_RECIPES` | Ligas e outros processos básicos de fusão. |
| `ELECTROMAGNETIC_SEPARATOR_RECIPES` | Separação de minerais e materiais. |

PACKER deve continuar junto de UNPACKER: o GTO separa os dois tipos.
CHEMICAL e LARGE_CHEMICAL devem continuar: no código consultado, LARGE_CHEMICAL inclui
CHEMICAL como proxy e também possui receitas próprias. O motor precisa respeitar a
receita/saída codificada pelo Pattern Buffer para resolver entradas ambíguas.
ALLOY_SMELTER existe no GTO, mas não faz parte dos 32 tipos da Universal Factory
do GTNA; por isso conta como um acréscimo, sem duplicação.

## Tipos fora do escopo escolhido

| Tipo | Observação para o port |
| --- | --- |
| `DISASSEMBLY_RECIPES` | Desmontagem; o tipo admite até 16 saídas de itens e 4 de fluidos. Rever roteamento e capacidade de saída. |
| `ELECTROPLATING_RECIPES` | Eletrodeposição; a máquina dedicada usa lógica elétrica. É um candidato à ampliação, mas substitui outro multiblock. |
| `THREE_DIMENSIONAL_PRINTER_RECIPES` | Impressão 3D; revisar máquina e condições antes de incluir. |
| `FIBER_EXTRUSION_RECIPES` | Extrusão de fibras; aparece junto de Wiremill em outra máquina. Revisar requisitos. |
| `PRECISION_ASSEMBLER_RECIPES` | Montagem de precisão com máquina específica; revisar requisitos e progressão. |
| `CHEMICAL_VAPOR_DEPOSITION_RECIPES`, `PHYSICAL_VAPOR_DEPOSITION_RECIPES` | Deposição especializada; não assumir equivalência ao Chemical Reactor. |
| `SINTERING_FURNACE_RECIPES`, `CRYSTALLIZATION_RECIPES`, `POLYMERIZATION_REACTOR_RECIPES` | Há máquinas dedicadas com bobinas no código local; aquecimento do GTNA não substitui essas condições. |
| `DRAWING_RECIPES` | Torre dedicada com classe própria. Exige revisão do comportamento. |

O conjunto definido é de 41 tipos. ARC_GENERATOR, EVAPORATION e FURNACE, sugeridos
no levantamento inicial, foram excluídos da proposta pelo usuário. Os tipos da tabela
acima também ficam fora do port solicitado. Não acrescentar outros tipos sem uma
nova instrução do usuário.

Não incluir automaticamente combustíveis/geração de energia, mineração e coleta,
receitas mágicas, pesquisa/scanners, Assembly Line, PCB Factory, fusão/fissão ou
processos espaciais. Dependem de capacidades, condições ou comportamento fora do
processamento geral. BLAST/VACUUM/DISTILLATION também exigem uma revisão própria:
bobinas/temperatura, resfriamento e roteamento de fluidos por camada, respectivamente.

## Comportamento confirmado pelo usuário: preservar GTNA

- `LEGACY`: baseParallel 64, baseThreads 16; ambos escalam por `2^tier`.
  Paralelismo inclui batch e aquecimento, com saturação em Integer.MAX_VALUE no GTNA.
- Aquecimento: `1 + (maxWarmup - 1) * (1 - exp(-runningSecs/warmupTau))`;
  máximo padrão 8x, tau 60 s, overload a 120 s força o máximo.
  Em operação acumula 1 s por segundo; parado perde 16 s por segundo.
- `SHARED_BUDGET`: orçamento dividido entre threads selecionadas, sem superar a
  capacidade total com receitas simultâneas. Capacidades padrão LV 1, MV 2, HV 4,
  EV 8, IV 16, LuV 32, ZPM 64, UV 128; acima de UV usa o fallback de UV.
- `UNLIMITED`: exige allowUnlimited para usar o teto técnico; sem a opção habilitada
  mantém o orçamento compartilhado. Teto técnico padrão 1.048.576 operações.
- Nos dois modos não LEGACY, a seleção de threads tem teto de 256 e da capacidade;
  aquecimento/batch não modificam o paralelo nesses modos, conforme a implementação GTNA.
- Batch padrão 1, máximo 1000. O GTNA expõe um botão AUTO e salva o estado, mas a
  pesquisa na lógica consultada não encontrou uso de getAutoBatch: não inventar um
  comportamento automático e descrevê-lo como preservado do GTNA.
- allowedRecipeTypes é uma lista de permissão configurável. Hatches especiais vêm
  desabilitados no padrão GTNA; adaptar as peças do GTO somente após conferir a semântica.

## Pontos técnicos e validação pendente

O port utiliza um dispatcher próprio sobre WorkableElectricMultiblockMachine,
com handlers nativos do GTO e persistência por GTRecipe.DATA_CODEC.
A API do dev9 também fornece `CrossRecipeMultiblockMachine`, `CrossRecipeTrait` e
`ICrossRecipeMachine`. Os métodos do GTOLib no jar de compilação são protegidos/hollow;
compilar contra eles não confirma o comportamento interno no jogo. Não assumir que o
motor nativo alterna automaticamente por todos os tipos sem validar em runtime.

Manter receitas compartilhadas intactas; modificar somente instâncias de execução.
Validar condições próprias de cada receita (inclusive cleanroom/mana quando existentes),
inputs não consumíveis, saída cheia e compatibilidade com buses/hatches estendidos.
Conservar energia por thread e o orçamento compartilhado durante receitas já em andamento.
Salvar progresso/saídas pendentes e tempo térmico; verificar suspensão e reload do mundo.
Interface precisa acomodar 41 modos; o GTNA usa seleção rolável por esse motivo.

Teste essencial: AE2 com várias camadas e tipos, incluindo duas receitas que aceitam o
mesmo input e produzem outputs diferentes. Não basta testar apenas a estrutura formando.
