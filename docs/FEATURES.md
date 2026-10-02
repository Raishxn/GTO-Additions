# Recursos do GTO-Additions

## Primitive Stone Furnace

ID: `gtoa:primitive_stone_furnace`.

- Receitas de `FURNACE_RECIPES`, incluindo as receitas vanilla convertidas pelo GTO.
- Processamento em um tick do servidor, sem EU/t e sem combustível.
- Todas as receitas distintas encontradas nas unidades de entrada são atendidas no mesmo tick.
- Cada receita usa o máximo de paralelos permitido pelas entradas e pelas saídas.
- Sem limite de threads imposto pela máquina e sem necessidade de hatches especiais.
- O máximo numérico de paralelos da API é 9.007.199.254.740.991 por receita.
  “Infinito” significa ausência de limite de tier/máquina; tipos numéricos e inventários são finitos.
- Cada par receita/unidade de entrada executa no máximo uma vez por tick, evitando loops
  de receitas autocatalíticas. Novas entradas podem executar no próximo tick.
- As definições globais de receitas permanecem intactas. A remoção de energia e a duração
  de um tick são aplicadas somente à cópia usada pela máquina.

Estrutura 3×3×3 de pedra, com o centro vazio e controlador no centro da face frontal.
Substitua blocos de pedra por pelo menos um barramento de entrada e um de saída de itens.
A aparência reutiliza a pedra vanilla e o overlay da primitive blast furnace do GTCEu.
Não inclui texturas do GTNA. O controlador está disponível por `/give @s gtoa:primitive_stone_furnace`.
Receita na bancada: 8 blocos de `minecraft:stone` ao redor de uma `minecraft:furnace`,
produzindo um controlador.

## Barramentos ULV estendidos e kit

`gtoa:ulv_extended_input_bus` e `gtoa:ulv_extended_output_bus` têm 8 slots cada
(o ULV normal do dev9 tem 1). Usam a lógica de barramento do próprio GTCEu, incluindo
inventário persistente, integração com o multiblock e automação. Entrada e saída são
variantes fixas: o comando de trocar direção do barramento normal fica desativado para
não transformar o estendido em um normal. A interface herdada expõe os oito slots.

Receitas sem formato: 2 `gtceu:ulv_input_bus` produzem 1 entrada estendida;
2 `gtceu:ulv_output_bus` produzem 1 saída estendida.

O item `gtoa:primitive_furnace_kit` entrega 23 pedras, 1 controlador e 1 de cada
barramento estendido ao clicar com o botão direito. Não coloca blocos no mundo.
É consumido no survival e reutilizável no criativo; itens que não couberem no inventário
caem perto do jogador. Está na aba criativa GTO-Additions, junto das máquinas, e pode
ser obtido por `/give @s gtoa:primitive_furnace_kit`. Receita do kit: 9 fornalhas normais preenchendo os 9 espaços da bancada (shaped),
produzindo 1 kit.

Validação manual adicional: confira os 8 slots, a persistência dos itens após recarregar,
a inserção/extração automatizada e as receitas dos barramentos. Use o kit no survival,
no criativo, com a mão secundária e com o inventário cheio; confira os 26 blocos entregues
(23 pedras + controlador + 2 barramentos) sem perdas nem entregas duplicadas.

## Registro das máquinas

O coremod `coremods/gtoa_machine_registration.js` injeta o registro antes dos retornos
de `com.gtocore.common.data.GTOMachines.<clinit>`, seguindo o ponto de integração usado
pelo GTOHJS. A descoberta via IGTAddon fornece apenas o namespace dos recursos; não
é responsável pelo registro das máquinas. Ao concluir o carregamento, o addon verifica
que controlador e barramentos estão nos registros de máquinas, blocos e itens.

Os testes do hook executam o script com Nashorn, verificam todos os retornos e aplicam
a transformação ao inicializador real do GTOCore 26.9.5. A inicialização completa do
modpack e o uso do kit ainda precisam de confirmação em jogo.

## Expansão de buses, hatches e covers

Todos os tiers ULV–MAX possuem barramentos de entrada/saída com 8 vezes os slots normais.
Hatches de entrada/saída nas variantes 1, 4 e 9 tanques mantêm essa quantidade de tanques,
com 8 vezes a capacidade por tanque. Os modelos referenciam explicitamente o GTCEu.

Cada saída estendida possui configuração de produção de 1× até `2^(tier + 1)`:
ULV 2×, LV 4×, MV 8×, e assim por diante, até MAX 32.768×. O padrão é o máximo do tier.
Ajuste pela aba do comparador na interface, com o controlador desativado. Alterações durante
uma receita ativa são recusadas. O boost afeta receitas, inclusive fluidos por camada de
destilação; inserções comuns por tubos não multiplicam recursos. Saídas cheias são simuladas
antes da inserção de um lote multiplicado, e overflow numérico recusa a operação.

As versões steam de saída têm 32 slots e 128.000 mB respectivamente, com boost de 1× a 8×,
padrão 8×. Não utilizam energia. Seus modelos e habilidades são de peças steam.

Crafts shapeless: 2 peças normais respectivas -> 1 peça estendida. O GTO começa hatches
normais de 4/9 tanques no EV; as novas variantes ULV–HV usam 2 hatches normais de 1 tanque
do tier correspondente. IDs: `gtoa:<tier>_extended_<input|output>_bus` e
`gtoa:<tier>_extended_<input|output>_hatch`, com sufixos `_4x` ou `_9x` nas variantes.

Covers `gtoa:<tier>_production_boost_cover`: hull do tier + redstone + lingote de ouro,
shapeless. Aplicáveis a máquinas singleblock que processam receitas. Configure o boost
com a chave de fenda no cover ou pela aba do cover na interface da máquina. Limites iguais
aos dos hatches: ULV 2× até MAX 32.768×. Reduzem duração para um quinto, mínimo 1 tick,
e EU/t positivo à metade, arredondado para cima. Vários covers aplicam apenas o maior boost;
velocidade e desconto de energia são aplicados uma vez. Os outputs da receita já modificada
são utilizados na verificação de espaço antes do consumo de entradas.

## Automação da torre primitiva

`gtoa:distillation_thermostat_hatch`: hatch de calor normal + comparador + lingote de ferro,
shapeless. Substitua um dos dois hatches de calor da Primitive Distillation Tower por ele.
Um termostato regula ambos os hatches da mesma torre, sem combustível: seção fria a 350 K
e seção quente configurável de 400 a 2000 K, padrão 800 K. Cada hatch mantém seu limite
próprio de temperatura, com margem de 1 K; o hatch normal limita a seção quente a 849 K.
Use dois termostatos se desejar a seção quente acima desse limite. A configuração fica
na aba do comparador da interface. A alteração do padrão de montagem fica restrita à torre
primitiva, sem habilitar o hatch nos outros multiblocos.

## Magic Generators do GTLCore

Port da implementação `GTLCore/.../generator/MagicEnergyMachine.java`, revisão
`18c781404aa5a406837b3492ca1afa428458e751`. Os valores ULV/LV originais foram
preservados e a família foi estendida até MAX. Para cada tier, a geração é
`256 × tensão` EU/s, o buffer é `512 × tensão` EU e a saída suporta até 16 A.
Exemplos:

| Máquina | Geração por segundo | Saída | Buffer |
| --- | ---: | ---: | ---: |
| `gtoa:ulv_magic_generator` | 2.048 EU | 8 V, até 16 A | 4.096 EU |
| `gtoa:lv_magic_generator` | 8.192 EU | 32 V, até 16 A | 16.384 EU |

End Crystal imediatamente acima, sem consumir cristal ou mana. Sem cristal a geração para;
a energia armazenada continua disponível. Buffer cheio não provoca explosão pelo gerador.
Para colocar o cristal, clique na face superior do gerador com um End Crystal, deixando dois
blocos livres acima. O cristal mantém seu comportamento normal de entidade do Minecraft.
O overlay reutiliza a turbina a gás do GTCEu, com o hull de cada tier.
IDs: `gtoa:<tier>_magic_generator`, de ULV até MAX.
A partir de MV, há também upgrade shapeless: gerador do tier anterior + hull do
novo tier + End Crystal.

Craft do gerador: hull do tier + olho de ender + diamante + redstone, shapeless.
Craft alternativo do End Crystal: receita vanilla de vidro e olho de ender, substituindo a
lágrima de ghast por diamante (`GGG / GEG / GDG`). Produz um cristal.

O estudo do miner está em [EASYTECHNOLOGY_STUDY.md](EASYTECHNOLOGY_STUDY.md).

Validação atual: compilação/reobfuscação e 55 testes automatizados aprovados,
incluindo os novos hooks nos jars efetivamente instalados do dev9 e análise da pilha ASM.
O modpack completo ainda precisa de validação em jogo das interfaces, persistência,
saídas cheias, boosts, controle de temperatura e geração.


## Entangled Miner e Entangled Oil Drill

Ambos são **singleblocks**, nas versões Steam, LV, MV, HV e EV. Possuem inventários
internos, sem estrutura, buses ou hatches externos. Os cartões não são consumidos.
A geração é infinita: não quebra ores, não diminui reservas e não carrega a área original.

| Tier | Miner: raw ores/s | Oil Drill: multiplicador do rendimento do campo | Consumo base |
| --- | ---: | ---: | --- |
| Steam | 4 | 1× | Carvão/carvão vegetal |
| LV | 8 | 2× | 32 EU/t |
| MV | 16 | 4× | 128 EU/t |
| HV | 32 | 8× | 512 EU/t |
| EV | 64 | 16× | 2.048 EU/t |

As versões Steam consomem **1 carvão ou carvão vegetal por 80 segundos de trabalho**.
Não precisam de EU, água ou vapor externo. O combustível só é gasto enquanto a receita
avança; saída bloqueada, falta de cartão e máquina desativada preservam o combustível
restante. Esse saldo é salvo no mundo. Cada Steam possui dois slots de entrada; as elétricas
possuem um. Os miners têm nove slots de saída; os Oil Drills têm um tanque de
64.000/128.000/256.000/512.000/1.024.000 mB, respectivamente.

Use as opções nativas de saída automática na interface e a chave para orientar a face de
saída. Os covers de produção funcionam nas duas famílias: multiplicam outputs, aceleram
receitas 5× e reduzem EU/t à metade. A tabela acima descreve a produção **sem covers**.
Não há overclock automático adicional. Os buses estendidos não se conectam a essas máquinas.

### Vincular um depósito de ores

1. Faça `gtoa:entangled_vein_card`: papel + pérola de ender + redstone, shapeless.
2. Clique em um ore do GTCEu dentro de um depósito gerado. O cartão escolhe o depósito
   compatível mais próximo entre os metadados a até 128 blocos. Confira ID, dimensão e centro
   no tooltip. Shift + clique direito no ar limpa o vínculo.
3. Insira o cartão diretamente na entrada do miner e forneça carvão (Steam) ou EU.

A composição vem da definição registrada do depósito vinculado, não da lista geral de ores
da dimensão. Os materiais com `rawOre` registrado são selecionados conforme os pesos do
veio; entradas sem raw ore são ignoradas. Cada lote dura cinco ticks e entrega 1/2/4/8/16
raw ores, mantendo exatamente 4/8/16/32/64 por segundo. Lotes menores permitem que o
miner EV acomode também sua produção com cover EV no inventário de nove slots.
Definição removida interrompe a produção; alterações de composição de datapack são respeitadas.
Depósitos de bedrock ore não fazem parte deste vínculo.

### Vincular um campo de fluido

1. Faça `gtoa:entangled_fluid_card`: papel + pérola de ender + balde vazio, shapeless.
2. Clique no chão do chunk do campo. O servidor consulta/inicializa os metadados nativos de
   fluido subterrâneo daquele chunk, sem reduzir suas operações restantes. Não é necessário
   expor um bloco de petróleo. Confira fluido, campo, dimensão, chunk e rendimento no tooltip.
3. Insira o cartão no Oil Drill, alimente a máquina e retire o fluido pelo tanque ou por tubos.

A máquina produz somente o fluido do campo vinculado: petróleo, petróleo bruto, leve/pesado,
gás ou outro fluido de bedrock registrado. O rendimento original salvo no cartão, em mB,
é a base de cada lote de 20 ticks. Não utiliza o rendimento reduzido de um campo esgotado.
Exemplo: cartão de **200 mB/s** resulta em **200/400/800/1.600/3.200 mB/s** de Steam a EV.
A máquina pode operar em outra dimensão e com a área original descarregada, sem tickets
remotos nem consultas ao mundo de origem durante a produção. Campo cuja definição foi
removida ou cujo fluido mudou exige vincular novamente o cartão. Shift + clique no ar limpa.

### Crafts e IDs

IDs: `gtoa:<steam|lv|mv|hv|ev>_entangled_miner` e
`gtoa:<steam|lv|mv|hv|ev>_entangled_oil_drill`.

Craft Steam do miner: `PEP / CFC / PDP`, P = picareta de ferro, E = pérola de ender,
C = bloco de cobre, F = fornalha normal, D = diamante.
Craft Steam do Oil Drill: `BEB / CFC / BDB`, B = balde vazio, demais ingredientes iguais.
Upgrades elétricos, shapeless: máquina do tier anterior + hull do novo tier + redstone.

Compatibilidade: o ID antigo `gtoa:entangled_miner` permanece registrado como singleblock
MV, fora da aba criativa. Uma receita shapeless com ele produz `gtoa:mv_entangled_miner`.
O antigo controlador já colocado passa a usar inventários internos; retire o cartão do bus
antigo e insira-o na máquina. Os blocos da estrutura anterior podem ser desmontados.

Se a saída encher durante a receita, o lote concluído aguarda espaço sem cobrar mais EU
ou carvão. O miner usa o contador salvo de ciclos concluídos para a seleção ponderada;
tentativas de inserção de um lote pendente não sorteiam outro ore.

### Verificação em jogo

Teste todos os tiers sem covers e confira as taxas da tabela. Vincule dois depósitos/campos
diferentes, confira os produtos, o cartão preservado e a ausência de esgotamento. Teste
saída cheia, falta de carvão/EU, saída automática por tubos, covers e origem descarregada.
Salve/recarregue trabalhando e com saída bloqueada, verificando combustível e inventários.

Compilação/reobfuscação e 55 testes automatizados aprovados, incluindo progressão de tiers,
overflow, seleção ponderada, consumo apenas em ticks bem-sucedidos, registro precoce e
contrato de saída bloqueada da RecipeLogic instalada. As máquinas novas ainda precisam
ser validadas dentro do modpack completo.


Correção de carregamento (2026-10-02): a integração do termostato preserva agora o overload
`Predicates.blocks(MetaMachineBlock[])`. O hook anterior retornava `Block[]`, causando
`VerifyError` ao carregar a Primitive Distillation Tower. O teste de regressão aplica o hook
no jar real do dev9, confere a assinatura do helper compilado e executa uma reprodução
mínima no verificador da JVM; o retorno antigo falha e o tipo corrigido é aceito. A análise
anterior com `BasicInterpreter` não verificava essa compatibilidade entre tipos de arrays.

Os tipos de receita do addon usam `com.gtolib.api.recipe.RecipeType`, exigido pelo carregamento
de dados/EMI do GTO. Os ciclos criam definições temporárias sem cadastrar receitas globais.
A verificação de recursos cobre todas as chaves de interface/tooltips e os nomes/modelos
ULV–MAX dos covers em português e inglês. Recursos faltantes de versões antigas também
exigem invalidar o cache persistente ao instalar, mesmo que os arquivos estejam no jar.

## Universal Factory

ID: `gtoa:universal_factory`. Port do GTNA com os 32 tipos originais e somente
os nove acréscimos escolhidos: Laminator, Loom, Laser Welder, Cluster, Rolling,
Dehydrator, Unpacker, Electromagnetic Separator e Alloy Smelter. Total: 41.

Textura do casing copiada do GTNA sem alteração; overlay do controlador é o
Assembly Line do GTCEu, como no original. Estrutura 3×3×3: controlador no centro
da face frontal, armação de aço no centro interno e os demais blocos de
`gtoa:universal_factory_casing`. Exatamente um hatch de manutenção. Buses de itens,
hatches de fluidos e energia substituem casings conforme as receitas desejadas.
Como no GTNA, formar sem energia é possível, mas receitas elétricas precisam dela.
Uma montagem com entrada/saída de itens, energia e manutenção usa 21 casings.

As duas receitas são 1:1 com o GTNA, adaptando somente IDs `gtna:` para `gtoa:`.
São receitas de bancada; não possuem duração nem consumo de EU.

Controlador: `ABC / DEF / GHI`, produz uma máquina:

| Posição | Ingrediente |
| --- | --- |
| A | Motor MV |
| B | Braço robótico MV |
| C | Pistão MV |
| D | Bomba MV |
| E | Universal Factory Casing |
| F | Emissor MV |
| G | Esteira MV |
| H | Sensor MV |
| I | Regulador de fluidos MV |

Casing: `BCB / DAD / BCB`, produz **2 casings**:
A = `gtceu:solid_machine_casing`; B = placa dupla de alumínio;
C = motor MV; D = pistão MV.

Configuração: `config/gtoa/balance/universal_factory.json`, criada no primeiro
uso da máquina. Alterações no arquivo exigem reiniciar o jogo/servidor.

- `LEGACY`, padrão: paralelo base 64 e threads base 16, multiplicados por `2^tier`.
  Aquecimento até 8×, tau 60 s e overload aos 120 s; parado perde 16 s térmicos
  por segundo. Batch configurável de 1 a 1000, multiplicando o teto de paralelo.
- `SHARED_BUDGET`: orçamento de operações dividido entre threads selecionadas.
  Valores LV/MV/HV/EV/IV/LuV/ZPM/UV: 1/2/4/8/16/32/64/128; acima de UV,
  permanece o orçamento de UV por padrão.
- `UNLIMITED`: usa o teto técnico de 1.048.576 somente com `allowUnlimited=true`;
  sem isso mantém orçamento compartilhado.

Threads selecionadas nos modos não Legacy e batch do Legacy são ajustáveis pela
interface sem receitas ativas. A seleção de tipos possui rolagem; o processamento
busca automaticamente em todos os tipos disponíveis. `allowedRecipeTypes` limita
a busca por IDs reais dos tipos de receita. A capacidade é um teto: entradas,
saídas e alimentação elétrica podem reduzir o paralelo efetivo. O aquecimento
aumenta o teto de paralelismo e não substitui bobinas ou requisitos de temperatura.
Hatches especiais de threads/overclock do GTNA não fazem parte da estrutura padrão.
O botão AUTO de batch, sem uso na lógica GTNA consultada, não é apresentado.

Cada receita em andamento mantém duração, progresso, EU e saídas próprios. O
orçamento inclui todas as receitas ativas. Saída bloqueada conserva o lote pronto
sem consumir mais energia; invalidação da estrutura pausa os lotes. Dados de
execução são salvos com o codec de disco do GTO, inclusive cor de saída.
Pattern Buffers usam a busca nativa, com checagem adicional das saídas codificadas
antes do consumo, feita sobre a receita original antes de multiplicar o paralelo.

Validação automática: compilação/reobfuscação e 55 testes, incluindo execução do
agendador com duas receitas diferentes simultâneas, retorno ao estado ocioso e
retomada após novas entradas. Receitas comparadas com o código GTNA e textura
verificada por hash. O funcionamento da Universal Factory foi confirmado em jogo
no dev9 após a correção da busca contínua de receitas. Persistência durante
receitas em andamento, condições especiais e outros setups ME ainda exigem testes.


No modo Legacy, uma receita ocupa no máximo uma thread quando o paralelismo é
maior que 1; receitas distintas podem ocupar threads simultâneas. A máquina busca
novas receitas mesmo após esvaziar a fila e fica ociosa quando não há trabalho.
Use saída de itens/fluidos compatível com as receitas; um Pattern Buffer de entrada
sozinho não substitui um barramento de saída. Os contadores da interface mostram
threads ocupadas e o teto disponível de paralelismo por thread.
